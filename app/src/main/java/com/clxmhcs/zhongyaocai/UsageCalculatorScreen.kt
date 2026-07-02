package com.clxmhcs.zhongyaocai

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

private data class UsageFormulaItem(val name: String, val grams: Double)
private data class UsageFormulaParse(val items: List<UsageFormulaItem>, val invalid: List<String>)

/** Current iOS PrescriptionUsageEstimatorView: formula A/B and dose-count input columns. */
@Composable
fun IOSPrescriptionUsageScreen(data: AppData, viewModel: MainViewModel) {
    val histories by viewModel.usageHistory.collectAsState()
    var formulaA by rememberSaveable { mutableStateOf("") }
    var formulaB by rememberSaveable { mutableStateOf("") }
    var dosesA by rememberSaveable { mutableStateOf("") }
    var dosesB by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var resultRows by remember { mutableStateOf<List<UsageCalculationRow>>(emptyList()) }
    var showResult by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<UsageCalculationHistory?>(null) }
    var clearConfirm by remember { mutableStateOf(false) }
    var historyDetail by remember { mutableStateOf<UsageCalculationHistory?>(null) }
    val scroll = rememberScrollState()

    fun calculate() {
        val a = parseUsageFormula(formulaA)
        val b = parseUsageFormula(formulaB)
        val hasA = formulaA.trim().isNotEmpty()
        val hasB = formulaB.trim().isNotEmpty()
        if (!hasA && !hasB) { message = "请输入处方内容"; return }
        val countA = if (hasA) dosesA.toIntOrNull() else 0
        val countB = if (hasB) dosesB.toIntOrNull() else 0
        if (hasA && (countA == null || countA <= 0)) { message = "请输入正确的处方A剂数"; return }
        if (hasB && (countB == null || countB <= 0)) { message = "请输入正确的处方B剂数"; return }
        val invalid = a.invalid + b.invalid
        if (invalid.isNotEmpty()) { message = "以下处方行格式错误：\n${invalid.take(10).joinToString("\n")}"; return }
        val groups = linkedMapOf<String, Pair<Double, Double>>()
        a.items.forEach { item ->
            val old = groups[item.name] ?: (0.0 to 0.0)
            groups[item.name] = (old.first + item.grams) to old.second
        }
        b.items.forEach { item ->
            val old = groups[item.name] ?: (0.0 to 0.0)
            groups[item.name] = old.first to (old.second + item.grams)
        }
        val missing = groups.keys.filter { name -> data.herbs.none { it.name == name } }
        if (missing.isNotEmpty()) { message = "以下药材不存在：\n${missing.joinToString("、")}"; return }
        val rows = groups.map { (name, doses) ->
            val herb = data.herbs.first { it.name == name }
            val dailyNeed = doses.first + doses.second
            val courseNeed = doses.first * (countA ?: 0) + doses.second * (countB ?: 0)
            val days = if (dailyNeed > 0.0) herb.stock / dailyNeed else 0.0
            UsageCalculationRow(name, herb.stock, dailyNeed, days, herb.stock + 1e-9 < courseNeed)
        }.sortedBy { it.days }
        resultRows = rows
        showResult = true
        viewModel.saveUsageHistory(UsageCalculationHistory(
            prescriptionAText = formulaA.trim(),
            dosesA = countA ?: 0,
            prescriptionBText = formulaB.trim(),
            dosesB = countB ?: 0,
            rows = rows
        ))
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column {
                Text("⚠️输入格式：\n1、药材名+日用量，空格可有可无，最多3个，如：白芍 12 。\n2、剂数建议输入相同的数字。", fontSize = 13.sp, color = Color.Gray)
                Row(Modifier.horizontalScroll(scroll).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    UsageFormulaColumn("处方A", formulaA, { formulaA = it }, Modifier.width(150.dp))
                    UsageFormulaColumn("处方B", formulaB, { formulaB = it }, Modifier.width(150.dp))
                    Column(Modifier.width(100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("处方A剂数", fontWeight = FontWeight.Bold)
                        OutlinedTextField(dosesA, { dosesA = it.filter(Char::isDigit) }, placeholder = { Text("请输入") }, singleLine = true)
                        Text("处方B剂数", fontWeight = FontWeight.Bold)
                        OutlinedTextField(dosesB, { dosesB = it.filter(Char::isDigit) }, placeholder = { Text("请输入") }, singleLine = true)
                        Button(onClick = ::calculate, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)), modifier = Modifier.fillMaxWidth()) { Text("计算") }
                    }
                }
            }
        }
        if (histories.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("历史计算", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { clearConfirm = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Icon(Icons.Default.Delete, null); Text("清空") }
                }
            }
            items(histories, key = { it.id }) { item ->
                Row(Modifier.fillMaxWidth().clickable { historyDetail = item }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("A: ${item.dosesA} 剂｜B: ${item.dosesB} 剂｜${fullDateLabel(item.createdAt)}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(item.rows.take(4).joinToString("；") { "${it.herbName} ${"%.1f".format(it.days)}天" }, fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.Delete, "删除", tint = Color.Red, modifier = Modifier.clickable { deleteTarget = item })
                }
                HorizontalDivider()
            }
        }
    }

    if (showResult) UsageResultDialog(resultRows) { showResult = false }
    message?.let { text -> AlertDialog(onDismissRequest = { message = null }, title = { Text("提示") }, text = { Text(text) }, confirmButton = { Button(onClick = { message = null }) { Text("确定") } }) }
    deleteTarget?.let { item -> AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("确认删除") }, text = { Text("确认删除这条历史计算记录吗？") }, confirmButton = { Button(onClick = { viewModel.deleteUsageHistory(item.id); deleteTarget = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("删除") } }, dismissButton = { Button(onClick = { deleteTarget = null }) { Text("取消") } }) }
    if (clearConfirm) AlertDialog(onDismissRequest = { clearConfirm = false }, title = { Text("确认清空") }, text = { Text("当前操作会清空所有历史计算记录，确认清空吗？") }, confirmButton = { Button(onClick = { viewModel.clearUsageHistory(); clearConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("清空") } }, dismissButton = { Button(onClick = { clearConfirm = false }) { Text("取消") } })
    historyDetail?.let { item -> UsageResultDialog(item.rows) { historyDetail = null } }
}

@Composable
private fun UsageFormulaColumn(title: String, text: String, change: (String) -> Unit, modifier: Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text(title, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); if (text.isNotEmpty()) Text("×", color = Color.Gray, modifier = Modifier.clickable { change("") }) }
        OutlinedTextField(text, change, modifier = Modifier.fillMaxWidth().height(180.dp), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace))
    }
}

@Composable
private fun UsageResultDialog(rows: List<UsageCalculationRow>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("库存可用天数测算结果") },
        text = {
            LazyColumn {
                item { Row(Modifier.fillMaxWidth()) { Text("药材", Modifier.weight(1f), fontWeight = FontWeight.Bold); Text("库存", Modifier.width(54.dp), fontWeight = FontWeight.Bold); Text("日用量", Modifier.width(64.dp), fontWeight = FontWeight.Bold); Text("天数", Modifier.width(54.dp), fontWeight = FontWeight.Bold) }; HorizontalDivider() }
                items(rows) { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(row.herbName, Modifier.weight(1f), color = if (row.insufficient) Color.Red else Color.Unspecified)
                        Text("${row.stock}g", Modifier.width(54.dp), color = if (row.insufficient) Color.Red else Color.Unspecified)
                        Text("${"%.1f".format(row.dailyNeed)}g", Modifier.width(64.dp))
                        Text("${"%.1f".format(row.days)}", Modifier.width(54.dp), color = if (row.days <= 7) Color.Red else Color.Unspecified)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("关闭") } }
    )
}

private fun parseUsageFormula(text: String): UsageFormulaParse {
    val regex = Regex("^([\\u4e00-\\u9fa5A-Za-z_·-]+)\\s{0,2}(\\d{1,4}(?:\\.\\d{1,4})?)(?:\\s*(?:[gG]|克))?$")
    val invalid = mutableListOf<String>()
    val items = mutableListOf<UsageFormulaItem>()
    text.lines().map(InventoryLineParser::normalize).filter { it.isNotBlank() }.forEach { raw ->
        val match = regex.matchEntire(raw)
        val name = match?.groupValues?.getOrNull(1)
        val grams = match?.groupValues?.getOrNull(2)?.toDoubleOrNull()
        if (name == null || grams == null || grams <= 0) invalid += raw else items += UsageFormulaItem(name, grams)
    }
    return UsageFormulaParse(items, invalid)
}
