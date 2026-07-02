package com.clxmhcs.zhongyaocai

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.UUID

@Composable
fun PrescriptionHistoryScreen(data: AppData, viewModel: MainViewModel, onOpenOverview: () -> Unit, onDetail: (Prescription) -> Unit) {
    var input by remember { mutableStateOf("") }
    var addDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var searchDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var addWarning by remember { mutableStateOf<String?>(null) }
    var searchWarning by remember { mutableStateOf<String?>(null) }
    var pendingItems by remember { mutableStateOf<List<PrescriptionItem>>(emptyList()) }
    var saveConfirm by remember { mutableStateOf(false) }
    var overwriteConfirm by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val addDateText = historyDateText(addDate)
    val searchDateText = historyDateText(searchDate)

    fun showDatePicker(current: Long, set: (Long) -> Unit) {
        val calendar = Calendar.getInstance().apply { timeInMillis = current }
        DatePickerDialog(context, { _, year, month, day ->
            val selected = Calendar.getInstance().apply { set(year, month, day, 12, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
            set(selected)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    fun validateAdd() {
        val parsed = parsePrescriptionItems(input, data.herbs)
        if (parsed.errors.isNotEmpty()) {
            addWarning = "以下药材不存在或格式错误：\n${parsed.errors.joinToString("\n")}" 
            return
        }
        if (parsed.items.isEmpty()) {
            addWarning = "请输入至少一味有效药材。"
            return
        }
        addWarning = null
        pendingItems = parsed.items
        if (data.prescriptions.any { it.dateString == addDateText }) overwriteConfirm = true else saveConfirm = true
    }
    fun save(overwrite: Boolean) {
        val existing = data.prescriptions.firstOrNull { it.dateString == addDateText }
        viewModel.savePrescription(
            Prescription(
                id = if (overwrite) existing?.id ?: UUID.randomUUID().toString() else UUID.randomUUID().toString(),
                timestamp = addDate,
                dateString = addDateText,
                items = pendingItems,
                note = existing?.note.orEmpty(),
                isImportant = existing?.isImportant ?: false,
                blisterReduced = existing?.blisterReduced,
                reimbursement = existing?.reimbursement ?: 0.0
            )
        )
        input = ""
        success = if (overwrite) "处方已覆盖保存。" else "处方已保存成功。"
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        success?.let { text -> item { Text("✓  $text", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) } }
        item {
            PageCard {
                Text("添加处方", fontWeight = FontWeight.Bold)
                Text("(逐行输入药材名+克数，如：白芍 12 )", fontSize = 12.sp, color = Color.Gray)
                OutlinedTextField(input, { input = it }, modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 10.dp), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace))
                DateLine("选择日期：", addDateText) { showDatePicker(addDate, { addDate = it }) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Button(onClick = ::validateAdd, modifier = Modifier.weight(1f)) { Text("确认添加") }
                    OutlinedButton(onClick = { input = ""; addWarning = null; pendingItems = emptyList() }, modifier = Modifier.weight(1f)) { Text("清空输入") }
                }
                addWarning?.let { Text(it, color = Color(0xFFFF9500), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
        item {
            PageCard {
                Text("查询处方", fontWeight = FontWeight.Bold)
                DateLine("历史日期：", searchDateText) { showDatePicker(searchDate, { searchDate = it }) }
                OutlinedButton(onClick = {
                    val found = data.prescriptions.firstOrNull { it.dateString == searchDateText }
                    if (found == null) searchWarning = "⚠️当前选择的日期「$searchDateText」没有录入处方！" else { searchWarning = null; onDetail(found) }
                }, modifier = Modifier.padding(top = 8.dp)) { Text("搜索") }
                searchWarning?.let { Text(it, color = Color(0xFFFF9500), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
        item {
            PageCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("全部处方", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                    Text("此页面可以查看处方详情、快速删除处方和对比处方。", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                    Button(onClick = onOpenOverview, modifier = Modifier.padding(top = 12.dp)) { Text("点击进入 处方总览 页面") }
                }
            }
        }
    }
    if (saveConfirm) PrescriptionSaveDialog("确认保存处方", addDateText, pendingItems, onDismiss = { saveConfirm = false }) { save(false); saveConfirm = false }
    if (overwriteConfirm) AlertDialog(
        onDismissRequest = { overwriteConfirm = false },
        title = { Text("确认覆盖处方") },
        text = { Text("当前日期「$addDateText」存在已保存的处方，确定覆盖原有处方吗？\n\n此操作不可撤销！") },
        confirmButton = { Button(onClick = { save(true); overwriteConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("确定覆盖") } },
        dismissButton = { OutlinedButton(onClick = { overwriteConfirm = false }) { Text("取消") } }
    )
}

@Composable
private fun DateLine(label: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Spacer(Modifier.weight(1f))
        Text(value, color = AppPurple, modifier = Modifier.padding(8.dp).then(Modifier))
        OutlinedButton(onClick = onClick) { Text("选择") }
    }
}

@Composable
private fun PrescriptionSaveDialog(title: String, date: String, items: List<PrescriptionItem>, onDismiss: () -> Unit, onSave: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn { item { Text("$date（共${items.size}味）", color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp)) }; items(items) { item -> Text("${item.herbName} ${item.grams.gramText()}", fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 3.dp)) } }
        },
        confirmButton = { Button(onClick = onSave) { Text("是的，确认保存") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun historyDateText(millis: Long): String = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(millis))
