package com.clxmhcs.zhongyaocai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil

@Serializable
data class Herb(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val stock: Int = 0,
    val warningLevel: Int = 0,
    val dailyUsage: Int = 0,
    val pricePerKg: Double = 0.0,
    val pinyin: String = name,
    val isBuiltIn: Boolean = false
)

@Serializable
data class AddHistory(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class InRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class OutRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class PrescriptionItem(
    val herbName: String,
    val grams: Double
)

@Serializable
data class Prescription(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String = dateLabel(System.currentTimeMillis()),
    val items: List<PrescriptionItem> = emptyList(),
    val note: String = "",
    val isImportant: Boolean = false,
    val blisterReduced: Boolean? = null,
    val reimbursement: Double = 0.0
)

@Serializable
data class HerbProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val alias: String = "",
    val medicinalPart: String = "",
    val natureFlavor: String = "",
    val meridian: String = "",
    val efficacy: String = "",
    val indications: String = "",
    val compatibility: String = "",
    val usageDosage: String = "",
    val contraindication: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class AppData(
    val herbs: List<Herb> = emptyList(),
    val addHistories: List<AddHistory> = emptyList(),
    val inRecords: List<InRecord> = emptyList(),
    val outRecords: List<OutRecord> = emptyList(),
    val prescriptions: List<Prescription> = emptyList(),
    val herbProfiles: List<HerbProfile> = emptyList(),
    val outboundBalances: Map<String, Double> = emptyMap(),
    val stockResetPasswordHash: String? = null
)

fun dateLabel(millis: Long): String = DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.CHINA)
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(millis))

fun fullDateLabel(millis: Long): String = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA)
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(millis))

fun Double.gramText(): String = if (this % 1.0 == 0.0) "${toInt()}g" else "${"%.2f".format(Locale.US, this).trimEnd('0').trimEnd('.') }g"

class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val storeFile = File(appContext.filesDir, "zhongyaocai_data.json")
    private val backupFile = File(appContext.filesDir, "zhongyaocai_data.tmp")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }
    private val lock = Mutex()
    private val _state = MutableStateFlow(readStoredData())
    val state: StateFlow<AppData> = _state.asStateFlow()

    private fun readStoredData(): AppData = try {
        if (!storeFile.exists()) AppData() else json.decodeFromString(AppData.serializer(), storeFile.readText())
    } catch (_: Exception) {
        AppData()
    }

    private suspend fun change(transform: (AppData) -> AppData) = withContext(Dispatchers.IO) {
        lock.withLock {
            val next = transform(_state.value)
            backupFile.writeText(json.encodeToString(AppData.serializer(), next))
            if (storeFile.exists()) storeFile.delete()
            backupFile.renameTo(storeFile)
            _state.value = next
        }
    }

    suspend fun addHerb(name: String, stock: Int, warning: Int, daily: Int, price: Double = 0.0): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "药材名不能为空"
        if (!isChineseOnly(trimmed)) return "药材名仅限汉字"
        val data = state.value
        if (data.herbs.any { it.name == trimmed }) return "当前输入的药材已存在，请勿重复添加"
        if (stock < 0 || warning < 0 || daily < 0 || price < 0) return "库存、预警值、日用量和价格不能为负数"
        val item = Herb(name = trimmed, stock = stock, warningLevel = warning, dailyUsage = daily, pricePerKg = price, pinyin = makePinyinKey(trimmed))
        change { old -> old.copy(herbs = old.herbs + item, addHistories = listOf(AddHistory(name = trimmed, amount = stock)) + old.addHistories) }
        return null
    }

    suspend fun updateHerb(herb: Herb): String? {
        if (herb.name.isBlank()) return "药材名不能为空"
        if (herb.stock < 0 || herb.warningLevel < 0 || herb.dailyUsage < 0 || herb.pricePerKg < 0) return "数值不能为负数"
        change { old -> old.copy(herbs = old.herbs.map { if (it.id == herb.id) herb.copy(pinyin = makePinyinKey(herb.name)) else it }) }
        return null
    }

    suspend fun deleteHerb(id: String) {
        change { old -> old.copy(herbs = old.herbs.filterNot { it.id == id }) }
    }

    suspend fun clearAddHistory() { change { it.copy(addHistories = emptyList()) } }

    suspend fun commitInbound(items: List<Pair<String, Int>>) {
        if (items.isEmpty()) return
        val now = System.currentTimeMillis()
        change { old ->
            var nextHerbs = old.herbs
            items.forEach { (name, amount) ->
                nextHerbs = nextHerbs.map { if (it.name == name) it.copy(stock = it.stock + amount) else it }
            }
            old.copy(
                herbs = nextHerbs,
                inRecords = items.map { InRecord(name = it.first, amount = it.second, createdAt = now) } + old.inRecords
            )
        }
    }

    suspend fun commitOutbound(deductions: List<Pair<String, Int>>, balancesAfter: Map<String, Double>) {
        val now = System.currentTimeMillis()
        change { old ->
            val totals = deductions.groupBy { it.first }.mapValues { entry -> entry.value.sumOf { it.second } }
            val nextHerbs = old.herbs.map { herb -> herb.copy(stock = herb.stock - (totals[herb.name] ?: 0)) }
            old.copy(
                herbs = nextHerbs,
                outRecords = deductions.filter { it.second > 0 }.map { OutRecord(name = it.first, amount = it.second, createdAt = now) } + old.outRecords,
                outboundBalances = balancesAfter
            )
        }
    }

    suspend fun clearInRecords() { change { it.copy(inRecords = emptyList()) } }
    suspend fun clearOutRecords() { change { it.copy(outRecords = emptyList()) } }
    suspend fun clearAllRecords() { change { it.copy(inRecords = emptyList(), outRecords = emptyList()) } }

    suspend fun savePrescription(prescription: Prescription) {
        change { old ->
            val exists = old.prescriptions.any { it.id == prescription.id }
            old.copy(prescriptions = if (exists) old.prescriptions.map { if (it.id == prescription.id) prescription else it } else listOf(prescription) + old.prescriptions)
        }
    }

    suspend fun deletePrescriptions(ids: Set<String>): List<Prescription> {
        val blocked = state.value.prescriptions.filter { it.id in ids && it.isImportant }
        change { old -> old.copy(prescriptions = old.prescriptions.filterNot { it.id in ids && !it.isImportant }) }
        return blocked
    }

    suspend fun saveHerbProfile(profile: HerbProfile) {
        change { old ->
            val exists = old.herbProfiles.any { it.id == profile.id }
            old.copy(herbProfiles = if (exists) old.herbProfiles.map { if (it.id == profile.id) profile else it } else listOf(profile) + old.herbProfiles)
        }
    }

    suspend fun deleteHerbProfile(id: String) { change { old -> old.copy(herbProfiles = old.herbProfiles.filterNot { it.id == id }) } }

    suspend fun overwriteHerbs(imported: List<Herb>) {
        change { old -> old.copy(herbs = imported.map { it.copy(pinyin = makePinyinKey(it.name)) }) }
    }

    suspend fun resetStock(password: String): ResetResult {
        val expected = state.value.stockResetPasswordHash
        if (expected == null) return ResetResult.NeedsPasswordSetup
        if (sha256(password) != expected) return ResetResult.WrongPassword
        change { old -> old.copy(herbs = old.herbs.map { it.copy(stock = 0) }) }
        return ResetResult.Success
    }

    suspend fun setResetPassword(password: String): String? {
        if (password.length < 4) return "密码至少需要 4 位"
        change { it.copy(stockResetPasswordHash = sha256(password)) }
        return null
    }

    fun exportBackup(): String = json.encodeToString(AppData.serializer(), state.value)

    suspend fun restoreBackup(text: String): String? = withContext(Dispatchers.IO) {
        val restored = try { json.decodeFromString(AppData.serializer(), text) } catch (_: Exception) { return@withContext "备份文件无法识别" }
        lock.withLock {
            backupFile.writeText(json.encodeToString(AppData.serializer(), restored))
            if (storeFile.exists()) storeFile.delete()
            backupFile.renameTo(storeFile)
            _state.value = restored
        }
        null
    }
}

sealed interface ResetResult {
    data object NeedsPasswordSetup : ResetResult
    data object WrongPassword : ResetResult
    data object Success : ResetResult
}

data class InboundValidation(
    val invalidOrMissing: List<String> = emptyList(),
    val items: List<Pair<String, Int>> = emptyList()
) {
    val valid: Boolean get() = invalidOrMissing.isEmpty() && items.isNotEmpty()
}

data class OutboundValidation(
    val invalidOrMissing: List<String> = emptyList(),
    val insufficient: List<Pair<String, Int>> = emptyList(),
    val deductions: List<Pair<String, Int>> = emptyList(),
    val balancesAfter: Map<String, Double> = emptyMap()
) {
    val valid: Boolean get() = invalidOrMissing.isEmpty() && insufficient.isEmpty()
}

object InventoryLineParser {
    private val inboundRegex = Regex("^([\\u4e00-\\u9fa5A-Za-z_·-]+)\\s{0,2}(\\d{1,4})(?:\\s*(?:[gG]|克))?$")
    private val outboundRegex = Regex("^([\\u4e00-\\u9fa5A-Za-z_·-]+)\\s{0,2}(\\d{1,4}(?:\\.\\d{1,4})?)(?:\\s*(?:[gG]|克))?$")

    fun normalize(line: String): String = Normalizer.normalize(line, Normalizer.Form.NFKC)
        .replace(Regex("[\\u200B-\\u200D\\uFEFF]"), "")
        .replace(Regex("[\\u00A0\\u2000-\\u200A\\u202F]"), " ")
        .trim()

    fun validateInbound(text: String, herbs: List<Herb>): InboundValidation {
        val invalid = mutableListOf<String>()
        val items = mutableListOf<Pair<String, Int>>()
        text.lines().map(::normalize).filter { it.isNotBlank() }.forEach { raw ->
            val match = inboundRegex.matchEntire(raw)
            val name = match?.groupValues?.getOrNull(1)
            val amount = match?.groupValues?.getOrNull(2)?.toIntOrNull()
            if (name == null || amount == null || herbs.none { it.name == name }) invalid += raw else items += name to amount
        }
        return InboundValidation(invalid, items)
    }

    fun validateOutbound(text: String, herbs: List<Herb>, storedBalances: Map<String, Double>): OutboundValidation {
        val invalid = mutableListOf<String>()
        val deductions = mutableListOf<Pair<String, Int>>()
        val tempBalances = storedBalances.toMutableMap()
        text.lines().map(::normalize).filter { it.isNotBlank() }.forEach { raw ->
            val match = outboundRegex.matchEntire(raw)
            val name = match?.groupValues?.getOrNull(1)
            val actual = match?.groupValues?.getOrNull(2)?.toDoubleOrNull()
            val herb = name?.let { n -> herbs.firstOrNull { it.name == n } }
            if (name == null || actual == null || actual <= 0 || herb == null) {
                invalid += raw
            } else {
                val oldBalance = (tempBalances[name] ?: 0.0).coerceIn(0.0, 0.999999)
                val need = actual - oldBalance
                val deduct = ceil(need.coerceAtLeast(0.0)).toInt()
                val newBalance = (oldBalance + deduct - actual).coerceIn(0.0, 0.999999)
                tempBalances[name] = newBalance
                if (deduct > 0) deductions += name to deduct
            }
        }
        val totals = deductions.groupBy { it.first }.mapValues { it.value.sumOf { pair -> pair.second } }
        val insufficient = totals.mapNotNull { (name, need) ->
            val have = herbs.firstOrNull { it.name == name }?.stock ?: return@mapNotNull null
            if (have < need) name to have else null
        }
        return OutboundValidation(invalid, insufficient, deductions, if (invalid.isEmpty() && insufficient.isEmpty()) tempBalances else emptyMap())
    }
}

fun isChineseOnly(text: String): Boolean = text.isNotBlank() && text.all { ch -> ch.code in 0x3400..0x4DBF || ch.code in 0x4E00..0x9FFF }

fun makePinyinKey(text: String): String = text.lowercase(Locale.ROOT)

fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray())
    .joinToString("") { "%02x".format(it) }
