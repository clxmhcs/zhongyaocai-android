package com.clxmhcs.zhongyaocai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun PrescriptionOverviewScreen(data: AppData, viewModel: MainViewModel, onAdd: () -> Unit) {
    var multiSelect by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var deleteConfirm by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Prescription?>(null) }
    var compare by remember { mutableStateOf<Pair<Prescription, Prescription>?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val createXlsx = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        if (uri != null) {
            val selectedItems = data.prescriptions.filter { it.id in selected }
            scope.launch { runCatching { XlsxCodec.write(context.contentResolver, uri, XlsxCodec.prescriptionSheet(selectedItems)) }.onSuccess { message = "处方 Excel 已导出。" }.onFailure { message = "导出失败：${it.message ?: "无法写入文件"}" } }
        }
    }
    val list = data.prescriptions.sortedByDescending { it.timestamp }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text("左滑删除处方；多选可对比、导出或快速删除。", color = Color(0xFFB06B23), modifier = Modifier.weight(1f)); TextButton(onClick = { multiSelect = !multiSelect; if (!multiSelect) selected = emptySet() }) { Text(if (multiSelect) "完成" else "多选") } }
        if (multiSelect) Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { RoundedActionButton("对比处方", AppCyan, selected.size == 2, onClick = { val pair = list.filter { it.id in selected }; if (pair.size == 2) compare = pair[0] to pair[1] }, modifier = Modifier.weight(1f)); RoundedActionButton("导出处方", AppPurple, selected.size > 2, onClick = { createXlsx.launch("处方对比_${System.currentTimeMillis()}.xlsx") }, modifier = Modifier.weight(1f)); RoundedActionButton("删除(${selected.size})", Color(0xFFD85457), selected.isNotEmpty(), onClick = { deleteConfirm = true }, modifier = Modifier.weight(1f)) }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { RoundedActionButton("新增处方", AppOrange, onClick = onAdd, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) }
            if (list.isEmpty()) item { EmptyHint("暂无处方。保存处方不会自动扣减库存。") }
            items(list, key = { it.id }) { prescription -> PageCard(Modifier.fillMaxWidth().clickable { if (multiSelect) selected = if (prescription.id in selected) selected - prescription.id else selected + prescription.id else detail = prescription }) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { if (multiSelect) Checkbox(checked = prescription.id in selected, onCheckedChange = { selected = if (prescription.id in selected) selected - prescription.id else selected + prescription.id }); Column(Modifier.weight(1f)) { Text(prescription.dateString, fontWeight = FontWeight.SemiBold); Text("${prescription.items.size} 味药材 · 合计 ¥${"%.2f".format(prescriptionCost(prescription, data))}", color = Color.Gray) }; if (prescription.isImportant) Icon(Icons.Default.Warning, "重要处方", tint = AppOrange); Text("详情", color = AppCyan) } } }
        }
    }
    if (deleteConfirm) AlertDialog(onDismissRequest = { deleteConfirm = false }, title = { Text("确定删除选中的 ${selected.size} 条处方吗？") }, text = { Text("此操作不可恢复。重要处方不会被删除，需先在详情中取消重要标记。") }, confirmButton = { TextButton(onClick = { viewModel.deletePrescriptions(selected) { blocked -> message = if (blocked.isEmpty()) "已删除选中处方。" else "其余非重要处方已删除。以下重要处方不可删除：${blocked.joinToString("、") { it.dateString }}"; selected = blocked.map { it.id }.toSet(); deleteConfirm = false } }) { Text("删除", color = Color.Red) } }, dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("取消") } })
    detail?.let { PrescriptionDetailDialog(it, data, viewModel) { detail = null } }
    compare?.let { PrescriptionCompareDialog(it.first, it.second, data) { compare = null } }
    message?.let { text -> AlertDialog(onDismissRequest = { message = null }, title = { Text("处方操作") }, text = { Text(text) }, confirmButton = { TextButton(onClick = { message = null }) { Text("确定") } }) }
}

@Composable
private fun PrescriptionDetailDialog(prescription: Prescription, data: AppData, viewModel: MainViewModel, onDismiss: () -> Unit) {
    var important by remember { mutableStateOf(prescription.isImportant) }
    var note by remember { mutableStateOf(prescription.note) }
    var reimbursement by remember { mutableStateOf(if (prescription.reimbursement == 0.0) "" else prescription.reimbursement.toString()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("${prescription.dateString} 处方") }, text = { LazyColumn { items(prescription.items) { item -> Text("${item.herbName}  ${item.grams.gramText()}", modifier = Modifier.padding(vertical = 3.dp)) }; item { Text("费用：¥${"%.2f".format(prescriptionCost(prescription, data))}", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp)); if (prescription.reimbursement > 0) Text("报销：医保 ¥${"%.2f".format(prescription.reimbursement)}"); Text("水泡是否减轻：${when (prescription.blisterReduced) { true -> "是"; false -> "否"; null -> "未记录" }}"); if (prescription.note.isNotBlank()) Text("备注：${prescription.note}") } } }, confirmButton = { TextButton(onClick = { viewModel.savePrescription(prescription.copy(isImportant = important, note = note, reimbursement = reimbursement.toDoubleOrNull() ?: 0.0)); onDismiss() }) { Text("保存") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } })
}

@Composable
private fun PrescriptionCompareDialog(left: Prescription, right: Prescription, data: AppData, onDismiss: () -> Unit) {
    val allNames = (left.items.map { it.herbName } + right.items.map { it.herbName }).distinct().sorted()
    val lines = allNames.map { name -> val a = left.items.firstOrNull { it.herbName == name }?.grams; val b = right.items.firstOrNull { it.herbName == name }?.grams; "$name：${a?.gramText() ?: "—"} → ${b?.gramText() ?: "—"}" }
    val context = LocalContext.current
    AlertDialog(onDismissRequest = onDismiss, title = { Text("处方变量对比") }, text = { LazyColumn { item { Text("${left.dateString}  →  ${right.dateString}", color = Color.Gray); Text("费用：¥${"%.2f".format(prescriptionCost(left, data))} → ¥${"%.2f".format(prescriptionCost(right, data))}", modifier = Modifier.padding(vertical = 6.dp)) }; items(lines) { Text(it, modifier = Modifier.padding(vertical = 2.dp)) } } }, confirmButton = { TextButton(onClick = { val service = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; service.setPrimaryClip(ClipData.newPlainText("处方变量对比", lines.joinToString("\n"))); onDismiss() }) { Text("复制对比") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } })
}

fun prescriptionCost(prescription: Prescription, data: AppData): Double = prescription.items.sumOf { item -> (data.herbs.firstOrNull { it.name == item.herbName }?.pricePerKg ?: 0.0) * item.grams / 1000.0 }
