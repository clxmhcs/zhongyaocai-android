package com.clxmhcs.zhongyaocai

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
            MaterialTheme { Surface { ZhongYaoApp(viewModel) } }
        }
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val json = Json { encodeDefaults = true }
    val data: StateFlow<AppData> = repository.state

    fun addHerb(name: String, stock: Int, warning: Int, daily: Int, price: Double, done: (String?) -> Unit) = viewModelScope.launch { done(repository.addHerb(name, stock, warning, daily, price)) }
    fun updateHerb(herb: Herb, done: (String?) -> Unit) = viewModelScope.launch { done(repository.updateHerb(herb)) }
    fun deleteHerb(id: String) = viewModelScope.launch { repository.deleteHerb(id) }
    fun undoLatestAddedHerb(name: String, done: (Boolean) -> Unit) = viewModelScope.launch {
        val current = data.value
        val herb = current.herbs.firstOrNull { it.name == name }
        val history = current.addHistories.firstOrNull { it.name == name }
        if (herb == null || history == null) { done(false); return@launch }
        val next = current.copy(herbs = current.herbs.filterNot { it.id == herb.id }, addHistories = current.addHistories.filterNot { it.id == history.id })
        done(repository.restoreBackup(json.encodeToString(AppData.serializer(), next)) == null)
    }
    fun clearAddHistory() = viewModelScope.launch { repository.clearAddHistory() }
    fun commitInbound(items: List<Pair<String, Int>>) = viewModelScope.launch { repository.commitInbound(items) }
    fun commitOutbound(items: List<Pair<String, Int>>, balances: Map<String, Double>) = viewModelScope.launch { repository.commitOutbound(items, balances) }
    fun clearInRecords() = viewModelScope.launch { repository.clearInRecords() }
    fun clearOutRecords() = viewModelScope.launch { repository.clearOutRecords() }
    fun clearAllRecords() = viewModelScope.launch { repository.clearAllRecords() }
    fun savePrescription(item: Prescription) = viewModelScope.launch { repository.savePrescription(item) }
    fun deletePrescriptions(ids: Set<String>, done: (List<Prescription>) -> Unit) = viewModelScope.launch { done(repository.deletePrescriptions(ids)) }
    fun saveProfile(profile: HerbProfile) = viewModelScope.launch { repository.saveHerbProfile(profile) }
    fun deleteProfile(id: String) = viewModelScope.launch { repository.deleteHerbProfile(id) }
    fun overwriteHerbs(items: List<Herb>) = viewModelScope.launch { repository.overwriteHerbs(items) }
    fun setResetPassword(password: String, done: (String?) -> Unit) = viewModelScope.launch { done(repository.setResetPassword(password)) }
    fun verifyResetPassword(password: String, done: (ResetResult) -> Unit) = viewModelScope.launch {
        val expected = data.value.stockResetPasswordHash
        done(when {
            expected == null -> ResetResult.NeedsPasswordSetup
            sha256(password) != expected -> ResetResult.WrongPassword
            else -> ResetResult.Success
        })
    }
    fun resetStock(password: String, done: (ResetResult) -> Unit) = viewModelScope.launch { done(repository.resetStock(password)) }
    fun exportBackup(): String = repository.exportBackup()
    fun restoreBackup(text: String, done: (String?) -> Unit) = viewModelScope.launch { done(repository.restoreBackup(text)) }
}
