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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class CostCalculationHistory(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val formulaText: String,
    val doses: Int,
    val insurancePercent: Int,
    val secondPercent: Int,
    val secondCapYuan: Double,
    val deductibleGapYuan: Double,
    val totalYuan: Double,
    val afterInsuranceYuan: Double,
    val secondReimbursedYuan: Double,
    val secondCapTriggered: Boolean,
    val finalPayYuan: Double,
    val detailText: String
)

@Serializable
data class UsageCalculationRow(
    val herbName: String,
    val stock: Int,
    val dailyNeed: Double,
    val days: Double,
    val insufficient: Boolean
)

@Serializable
data class UsageCalculationHistory(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val prescriptionAText: String,
    val dosesA: Int,
    val prescriptionBText: String,
    val dosesB: Int,
    val rows: List<UsageCalculationRow>
)

class CalculationStore(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }
    private val costFile = File(appContext.filesDir, "cost_calculation_history.json")
    private val usageFile = File(appContext.filesDir, "usage_calculation_history.json")
    private val lock = Mutex()
    private val _costHistory = MutableStateFlow(readCost())
    private val _usageHistory = MutableStateFlow(readUsage())
    val costHistory: StateFlow<List<CostCalculationHistory>> = _costHistory.asStateFlow()
    val usageHistory: StateFlow<List<UsageCalculationHistory>> = _usageHistory.asStateFlow()

    private fun readCost(): List<CostCalculationHistory> = try {
        if (!costFile.exists()) emptyList() else json.decodeFromString(ListSerializer(CostCalculationHistory.serializer()), costFile.readText())
    } catch (_: Exception) { emptyList() }

    private fun readUsage(): List<UsageCalculationHistory> = try {
        if (!usageFile.exists()) emptyList() else json.decodeFromString(ListSerializer(UsageCalculationHistory.serializer()), usageFile.readText())
    } catch (_: Exception) { emptyList() }

    suspend fun saveCost(entry: CostCalculationHistory) = withContext(Dispatchers.IO) {
        lock.withLock {
            val next = listOf(entry) + _costHistory.value
            costFile.writeText(json.encodeToString(ListSerializer(CostCalculationHistory.serializer()), next))
            _costHistory.value = next
        }
    }

    suspend fun deleteCost(id: String) = withContext(Dispatchers.IO) {
        lock.withLock {
            val next = _costHistory.value.filterNot { it.id == id }
            costFile.writeText(json.encodeToString(ListSerializer(CostCalculationHistory.serializer()), next))
            _costHistory.value = next
        }
    }

    suspend fun clearCost() = withContext(Dispatchers.IO) {
        lock.withLock {
            costFile.writeText("[]")
            _costHistory.value = emptyList()
        }
    }

    suspend fun saveUsage(entry: UsageCalculationHistory) = withContext(Dispatchers.IO) {
        lock.withLock {
            val next = listOf(entry) + _usageHistory.value
            usageFile.writeText(json.encodeToString(ListSerializer(UsageCalculationHistory.serializer()), next))
            _usageHistory.value = next
        }
    }

    suspend fun deleteUsage(id: String) = withContext(Dispatchers.IO) {
        lock.withLock {
            val next = _usageHistory.value.filterNot { it.id == id }
            usageFile.writeText(json.encodeToString(ListSerializer(UsageCalculationHistory.serializer()), next))
            _usageHistory.value = next
        }
    }

    suspend fun clearUsage() = withContext(Dispatchers.IO) {
        lock.withLock {
            usageFile.writeText("[]")
            _usageHistory.value = emptyList()
        }
    }
}
