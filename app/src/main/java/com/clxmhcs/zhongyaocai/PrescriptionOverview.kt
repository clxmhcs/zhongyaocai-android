package com.clxmhcs.zhongyaocai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** Current iOS PrescriptionOverviewView visual structure. */
@Composable
fun PrescriptionOverviewScreen(
    data: AppData,
    viewModel: MainViewModel,
    onAdd: () -> Unit,
    onDetail: (Prescription) -> Unit = {}
) {
    var multiSelect by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var deleteConfirm by remember { mutableStateOf(false) }
    var comparePair by remember { mutableStateOf<Pair<Prescription, Prescription>?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val list = data.prescriptions.sortedByDescending { it.timestamp }
    val createXlsx = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        if (uri != null) {
            val selected = list.filter { it.id in selectedIds }
            scope.launch {
                runCatching { XlsxCodec.write(context.contentResolver, uri, XlsxCodec.prescriptionSheet(selected)) }
                    .onSuccess { message = "处方 Excel 已导出。" }
                    .onFailure { message = "导出失败：${it.message ?: "无法写入文件"}" }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp, start = 12.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFFFF9500))) { append("👉🏻 左滑可删除处方，多选") }
                    withStyle(SpanStyle(color = Color(0xFF00A6C7))) { append("可对比") }
                    withStyle(SpanStyle(color = Color(0xFFFF9500))) { append("或快速删除多个处方 👈🏻") }
                },
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = {
                multiSelect = !multiSelect
                if (!multiSelect) selectedIds = emptySet()
            }) { Text(if (multiSelect) "完成" else "多选") }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().background(Color.White)
        ) {
            if (list.isEmpty()) item { Text("暂无处方", color = Color.Gray, modifier = Modifier.padding(24.dp)) }
            items(list, key = { it.id }) { prescription ->
                PrescriptionListRow(
                    prescription = prescription,
                    multiSelect = multiSelect,
                    selected = prescription.id in selectedIds,
                    onToggle = {
                        selectedIds = if (prescription.id in selectedIds) selectedIds - prescription.id else selectedIds + prescription.id
                    },
                    onDetail = { onDetail(prescription) }
                )
                HorizontalDivider(color = Color(0x14000000))
            }
        }
        if (multiSelect) {
            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    enabled = selectedIds.size == 2,
                    onClick = {
                        val pair = list.filter { it.id in selectedIds }
                        if (pair.size == 2) comparePair = pair[0] to pair[1]
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("对比处方") }
                TextButton(
                    enabled = selectedIds.size > 2,
                    onClick = { createXlsx.launch("处方对比_${System.currentTimeMillis()}.xlsx") },
                    modifier = Modifier.weight(1f)
                ) { Text("导出处方") }
                TextButton(
                    enabled = selectedIds.isNotEmpty(),
                    onClick = { deleteConfirm = true },
                    modifier = Modifier.weight(1f)
                ) { Text("删除(${selectedIds.size})", color = Color.Red) }
            }
        }
    }

    if (deleteConfirm) AlertDialog(
        onDismissRequest = { deleteConfirm = false },
        title = { Text("⚠️确定删除选中的 ${selectedIds.size} 条处方吗？") },
        text = { Text("此操作不可恢复") },
        confirmButton = { TextButton(onClick = {
            viewModel.deletePrescriptions(selectedIds) { blocked ->
                message = if (blocked.isEmpty()) "已删除选中处方。" else "其余非重要处方已删除。\n选中的处方「${blocked.joinToString("、") { it.dateString }}」为重要处方，不可删除！\n请到处方详情页面取消重要处方后再操作！"
                selectedIds = blocked.map { it.id }.toSet()
                if (blocked.isEmpty()) multiSelect = false
                deleteConfirm = false
            }
        }) { Text("删除", color = Color.Red) } },
        dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("取消") } }
    )
    comparePair?.let { PrescriptionCompareDialog(it.first, it.second, data) { comparePair = null } }
    message?.let { text -> AlertDialog(onDismissRequest = { message = null }, title = { Text(if (text.contains("重要处方")) "⚠️不可删除重要处方" else "处方操作") }, text = { Text(text) }, confirmButton = { TextButton(onClick = { message = null }) { Text("确定") } }) }
}

@Composable
private fun PrescriptionListRow(
    prescription: Prescription,
    multiSelect: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
    onDetail: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (multiSelect) {
            Icon(
                if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                "选择处方",
                tint = if (selected) AppPurple else Color.Gray,
                modifier = Modifier.clickable(onClick = onToggle)
            )
            Spacer(Modifier.padding(horizontal = 5.dp))
        }
        Text(prescription.dateString)
        if (prescription.isImportant) {
            Spacer(Modifier.padding(horizontal = 4.dp))
            Icon(Icons.Default.Warning, "重要处方", tint = AppOrange)
        }
        Spacer(Modifier.weight(1f))
        if (!multiSelect) TextButton(onClick = onDetail) { Text("详情", color = Color(0xFF007AFF)) }
    }
}

@Composable
private fun PrescriptionCompareDialog(left: Prescription, right: Prescription, data: AppData, onDismiss: () -> Unit) {
    val allNames = (left.items.map { it.herbName } + right.items.map { it.herbName }).distinct().sorted()
    val lines = allNames.map { name ->
        val a = left.items.firstOrNull { it.herbName == name }?.grams
        val b = right.items.firstOrNull { it.herbName == name }?.grams
        "$name：${a?.gramText() ?: "—"} → ${b?.gramText() ?: "—"}"
    }
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("处方变量对比") },
        text = { LazyColumn { item { Text("${left.dateString}  →  ${right.dateString}", color = Color.Gray); Text("费用：¥${"%.2f".format(prescriptionCost(left, data))} → ¥${"%.2f".format(prescriptionCost(right, data))}", modifier = Modifier.padding(vertical = 6.dp)) }; items(lines) { Text(it, modifier = Modifier.padding(vertical = 2.dp)) } } },
        confirmButton = { TextButton(onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("处方变量对比", lines.joinToString("\n")))
            onDismiss()
        }) { Text("复制对比") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

fun prescriptionCost(prescription: Prescription, data: AppData): Double =
    prescription.items.sumOf { item -> (data.herbs.firstOrNull { it.name == item.herbName }?.pricePerKg ?: 0.0) * item.grams / 1000.0 }
