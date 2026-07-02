package com.clxmhcs.zhongyaocai

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Current iOS SearchView: multiple Chinese herb terms, warnings, and fixed-width table. */
@Composable
fun InventorySearchScreen(data: AppData, initialKeyword: String) {
    var input by rememberSaveable { mutableStateOf(initialKeyword) }
    var appliedInput by rememberSaveable { mutableStateOf(initialKeyword) }
    var didSearch by rememberSaveable { mutableStateOf(initialKeyword.isNotBlank()) }
    val tokens = appliedInput
        .replace(Regex("[，,、;；|｜\\n\\t]"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    val all = data.herbs
    val results = if (!didSearch || tokens.isEmpty()) emptyList() else all.filter { herb -> tokens.any { token -> herb.name.contains(token) } }
    val missing = if (!didSearch) emptyList() else tokens.filter { token -> all.none { it.name.contains(token) } }
    val lowNames = results.filter { it.stock < it.warningLevel }.map { it.name }
    val scroll = rememberScrollState()

    Column(Modifier.fillMaxSize()) {
        if (didSearch && (missing.isNotEmpty() || lowNames.isNotEmpty())) {
            Column(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (missing.isNotEmpty()) {
                    Text("⚠️当前输入中，有不存在的药材⚠️", color = Color(0xFFFF9500), fontWeight = FontWeight.Bold)
                    Text(missing.joinToString("、"), color = Color.Red)
                }
                if (missing.isNotEmpty() && lowNames.isNotEmpty()) HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                if (lowNames.isNotEmpty()) Text("⚠️当前有库存不足药材，已标红显示", color = Color(0xFFFF9500), fontWeight = FontWeight.Bold)
            }
        } else if (all.isEmpty()) {
            Text("⚠️ 当前数据库中没有药材数据。", color = Color.Red, modifier = Modifier.padding(top = 8.dp, start = 16.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("输入药材名（空格/、/，/,/；/| 可分隔多个；仅中文）") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            if (input.isNotEmpty()) Text("×", fontSize = 24.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp).then(Modifier))
        }
        Button(
            onClick = { appliedInput = input; didSearch = true },
            colors = ButtonDefaults.buttonColors(containerColor = AppPurple),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) { Text("搜索") }
        if (didSearch) {
            if (results.isEmpty()) {
                Text("⚠️未找到匹配的药材", color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp))
            } else {
                Column(Modifier.horizontalScroll(scroll).padding(top = 14.dp)) {
                    Row(Modifier.width(400.dp).padding(horizontal = 16.dp)) {
                        TableHeader("药材名", 72); TableHeader("余量", 60, true); TableHeader("预警值", 60, true); TableHeader("日(周)用量(g)", 110, true); TableHeader("价格(元/kg)", 88, true)
                    }
                    HorizontalDivider()
                    LazyColumn(Modifier.width(400.dp).fillMaxSize()) {
                        items(results, key = { it.id }) { herb ->
                            val low = herb.stock < herb.warningLevel
                            val color = if (low) Color.Red else Color.Unspecified
                            Row(Modifier.width(400.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
                                TableValue(herb.name, 72, color)
                                TableValue("${herb.stock} g", 60, color, true)
                                TableValue("${herb.warningLevel} g", 60, color, true)
                                TableValue("${herb.dailyUsage} (周 ${herb.dailyUsage * 7})", 110, color, true)
                                TableValue("${"%.2f".format(herb.pricePerKg)}", 88, color, true)
                            }
                            HorizontalDivider(color = Color(0x59000000))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHeader(text: String, width: Int, end: Boolean = false) {
    Text(text, Modifier.width(width.dp), fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Clip, textAlign = if (end) androidx.compose.ui.text.style.TextAlign.End else androidx.compose.ui.text.style.TextAlign.Start)
}

@Composable
private fun TableValue(text: String, width: Int, color: Color, end: Boolean = false) {
    Text(text, Modifier.width(width.dp), color = color, maxLines = 1, overflow = TextOverflow.Clip, textAlign = if (end) androidx.compose.ui.text.style.TextAlign.End else androidx.compose.ui.text.style.TextAlign.Start)
}

/** Current iOS HighValueHerbsView: price > 95, descending price, three columns. */
@Composable
fun HighValueHerbsScreen(data: AppData) {
    val herbs = data.herbs.filter { it.pricePerKg > 95.0 }.sortedByDescending { it.pricePerKg }
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize()) {
        Text("高价值药材信息", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(top = 20.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.horizontalScroll(scroll)) {
            Row(Modifier.width(304.dp).padding(horizontal = 12.dp)) {
                TableHeader("药材名称", 100)
                TableHeader("价格(元/kg)", 100, true)
                TableHeader("库存", 80, true)
            }
            HorizontalDivider()
            LazyColumn(Modifier.width(304.dp).fillMaxSize()) {
                if (herbs.isEmpty()) item { EmptyHint("暂无高价值药材") }
                items(herbs, key = { it.id }) { herb ->
                    val low = herb.stock < herb.warningLevel
                    val color = if (low) Color.Red else Color.Unspecified
                    Row(Modifier.width(304.dp).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        TableValue(herb.name, 100, color)
                        TableValue("${"%.2f".format(herb.pricePerKg)}", 100, color, true)
                        TableValue("${herb.stock} g", 80, color, true)
                    }
                }
            }
        }
    }
}

@Composable
fun PrescriptionCostCalculatorScreen(data: AppData) {
    val list = data.prescriptions.sortedByDescending { it.timestamp }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("根据当前药材价格计算历史处方费用。", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp)) }
        if (list.isEmpty()) item { EmptyHint("暂无处方") }
        items(list, key = { it.id }) { prescription ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Column(Modifier.weight(1f)) { Text(prescription.dateString, fontWeight = FontWeight.SemiBold); Text("共 ${prescription.items.size} 味", fontSize = 12.sp, color = Color.Gray) }
                Text("¥${"%.2f".format(prescriptionCost(prescription, data))}", fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider()
        }
    }
}

@Composable
fun PrescriptionUsageScreen(data: AppData) {
    val list = data.prescriptions.sortedByDescending { it.timestamp }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("按处方单日用量估算现有库存可用天数。", color = Color.Gray, fontSize = 13.sp) }
        if (list.isEmpty()) item { EmptyHint("暂无处方，无法测算。") }
        items(list, key = { it.id }) { prescription ->
            val values = prescription.items.mapNotNull { item -> val stock = data.herbs.firstOrNull { it.name == item.herbName }?.stock ?: return@mapNotNull null; if (item.grams > 0) stock / item.grams else null }
            val minDays = values.minOrNull()
            PageCard(Modifier.fillMaxWidth()) { Text(prescription.dateString, fontWeight = FontWeight.SemiBold); Text(if (minDays == null) "无法测算：处方药材未在库存中找到。" else "按最短药材计算：约 ${"%.1f".format(minDays)} 天", color = if ((minDays ?: 99.0) <= 7) Color.Red else Color(0xFF31864A), modifier = Modifier.padding(top = 6.dp)) }
        }
    }
}

@Composable
fun RemainingDaysScreen(data: AppData, title: String, lowerExclusive: Double, upperInclusive: Double) {
    val herbs = data.herbs.filter { herb -> herb.dailyUsage > 0 && (herb.stock.toDouble() / herb.dailyUsage) > lowerExclusive && (herb.stock.toDouble() / herb.dailyUsage) <= upperInclusive }.sortedBy { it.stock.toDouble() / it.dailyUsage }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        if (herbs.isEmpty()) item { EmptyHint("暂无符合条件的药材") }
        items(herbs, key = { it.id }) { herb ->
            val days = herb.stock.toDouble() / herb.dailyUsage
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) { Text(herb.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Text("约 ${"%.1f".format(days)} 天", color = if (days <= 7) Color.Red else Color(0xFFFF9500)) }
            HorizontalDivider()
        }
    }
}
