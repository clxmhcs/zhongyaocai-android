package com.clxmhcs.zhongyaocai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class UsageFormulaItem(val name: String, val grams: Double)
private data class UsageFormulaParse(val items: List<UsageFormulaItem>, val invalid: List<String>)

@Composable
fun IOSPrescriptionUsageScreen(data: AppData, viewModel: MainViewModel) {
    val histories by viewModel.usageHistory.collectAsState()
    var a by rememberSaveable { mutableStateOf("") }
    var b by rememberSaveable { mutableStateOf("") }
    var dosesA by rememberSaveable { mutableStateOf("") }
    var dosesB by rememberSaveable { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<UsageCalculationRow>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<UsageCalculationHistory?>(null) }
    var clearConfirm by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    fun calculate() {
        val pa = parseUsageFormula(a); val pb = parseUsageFormula(b)
        val ca = dosesA.toIntOrNull() ?: 0; val cb = dosesB.toIntOrNull() ?: 0
        when {
            a.isBlank() && b.isBlank() -> message = "请输入处方内容"
            a.isNotBlank() && ca <= 0 -> message = "请输入正确的处方A剂数"
            b.isNotBlank() && cb <= 0 -> message = "请输入正确的处方B剂数"
            (pa.invalid + pb.invalid).isNotEmpty() -> message = "以下处方行格式错误：\n${(pa.invalid + pb.invalid).joinToString("\n")}" 
            else -> {
                val all = linkedMapOf<String, Pair<Double, Double>>()
                pa.items.forEach { x -> val old = all[x.name] ?: (0.0 to 0.0); all[x.name] = old.first + x.grams to old.second }
                pb.items.forEach { x -> val old = all[x.name] ?: (0.0 to 0.0); all[x.name] = old.first to old.second + x.grams }
                val missing = all.keys.filter { n -> data.herbs.none { it.name == n } }
                if (missing.isNotEmpty()) message = "以下药材不存在：\n${missing.joinToString("、")}" else {
                    rows = all.map { (name, pair) ->
                        val herb = data.herbs.first { it.name == name }
                        val daily = pair.first + pair.second
                        val need = pair.first * ca + pair.second * cb
                        UsageCalculationRow(name, herb.stock, daily, if (daily > 0) herb.stock / daily else 0.0, herb.stock < need)
                    }.sortedBy { it.days }
                    viewModel.saveUsageHistory(UsageCalculationHistory(prescriptionAText = a, dosesA = ca, prescriptionBText = b, dosesB = cb, rows = rows))
                    showResult = true
                }
            }
        }
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("⚠️输入格式：药材名+日用量；剂数建议输入相同数字。", fontSize = 13.sp, color = Color.Gray)
            Row(Modifier.horizontalScroll(scroll).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormulaInput("处方A", a, { a = it }, Modifier.width(150.dp))
                FormulaInput("处方B", b, { b = it }, Modifier.width(150.dp))
                Column(Modifier.width(110.dp)) {
                    Text("处方A剂数"); OutlinedTextField(dosesA, { dosesA = it.filter(Char::isDigit) }, singleLine = true)
                    Spacer(Modifier.height(8.dp)); Text("处方B剂数"); OutlinedTextField(dosesB, { dosesB = it.filter(Char::isDigit) }, singleLine = true)
                    Spacer(Modifier.height(8.dp)); Button(onClick = ::calculate, modifier = Modifier.fillMaxWidth()) { Text("计算") }
                }
            }
        }
        if (histories.isNotEmpty()) {
            item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("历史计算", fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Button(onClick = { clearConfirm = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("清空") } } }
            items(histories, key = { it.id }) { h -> Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text("A:${h.dosesA}剂  B:${h.dosesB}剂  ${fullDateLabel(h.createdAt)}", Modifier.weight(1f)); Icon(Icons.Default.Delete, "删除", tint = Color.Red, modifier = Modifier.clickable { deleteTarget = h }) }; HorizontalDivider() }
        }
    }
    if (showResult) AlertDialog(onDismissRequest = { showResult = false }, title = { Text("库存可用天数测算结果") }, text = { LazyColumn { items(rows) { r -> Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(r.herbName, Modifier.weight(1f), color = if (r.insufficient) Color.Red else Color.Unspecified); Text("${r.stock}g", Modifier.width(58.dp)); Text("${"%.1f".format(r.dailyNeed)}g", Modifier.width(64.dp)); Text("${"%.1f".format(r.days)}天", Modifier.width(62.dp), color = if (r.days <= 7) Color.Red else Color.Unspecified) } } } }, confirmButton = { Button(onClick = { showResult = false }) { Text("关闭") } })
    message?.let { m -> AlertDialog(onDismissRequest = { message = null }, title = { Text("提示") }, text = { Text(m) }, confirmButton = { Button(onClick = { message = null }) { Text("确定") } }) }
    deleteTarget?.let { h -> AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("确认删除") }, text = { Text("确认删除这条历史计算记录吗？") }, confirmButton = { Button(onClick = { viewModel.deleteUsageHistory(h.id); deleteTarget = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("删除") } }, dismissButton = { Button(onClick = { deleteTarget = null }) { Text("取消") } }) }
    if (clearConfirm) AlertDialog(onDismissRequest = { clearConfirm = false }, title = { Text("确认清空") }, text = { Text("当前操作会清空所有历史计算记录，确认清空吗？") }, confirmButton = { Button(onClick = { viewModel.clearUsageHistory(); clearConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("清空") } }, dismissButton = { Button(onClick = { clearConfirm = false }) { Text("取消") } })
}

@Composable
private fun FormulaInput(title: String, value: String, change: (String) -> Unit, modifier: Modifier) {
    Column(modifier) { Row { Text(title, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); if (value.isNotEmpty()) Text("×", color = Color.Gray, modifier = Modifier.clickable { change("") }) }; OutlinedTextField(value, change, modifier = Modifier.fillMaxWidth().height(180.dp), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)) }
}

private fun parseUsageFormula(text: String): UsageFormulaParse {
    val regex = Regex("^([\\u4e00-\\u9fa5A-Za-z_·-]+)\\s{0,2}(\\d{1,4}(?:\\.\\d{1,4})?)(?:\\s*(?:[gG]|克))?$")
    val bad = mutableListOf<String>(); val out = mutableListOf<UsageFormulaItem>()
    text.lines().map(InventoryLineParser::normalize).filter { it.isNotBlank() }.forEach { line ->
        val m = regex.matchEntire(line); val name = m?.groupValues?.getOrNull(1); val grams = m?.groupValues?.getOrNull(2)?.toDoubleOrNull()
        if (name == null || grams == null || grams <= 0) bad += line else out += UsageFormulaItem(name, grams)
    }
    return UsageFormulaParse(out, bad)
}
