package com.clxmhcs.zhongyaocai

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InventorySearchScreen(data: AppData, initialKeyword: String) {
    var keyword by rememberSaveable { mutableStateOf(initialKeyword) }
    val results = data.herbs.filter { keyword.isBlank() || it.name.contains(keyword.trim()) }.sortedWith(compareBy<Herb> { it.pinyin }.thenBy { it.name })
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { OutlinedTextField(keyword, { keyword = it }, label = { Text("输入药材名") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { Text("查询结果", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
        if (results.isEmpty()) item { EmptyHint("未找到相关药材") }
        items(results, key = { it.id }) { herb ->
            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
                Text(herb.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text("余量 ${herb.stock}g", color = if (herb.stock < herb.warningLevel) Color.Red else Color.Unspecified)
            }
            Text("预警 ${herb.warningLevel}g · 日用量 ${herb.dailyUsage}g · ¥${"%.2f".format(herb.pricePerKg)}/kg", fontSize = 12.sp, color = Color.Gray)
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun HighValueHerbsScreen(data: AppData) {
    val herbs = data.herbs.filter { it.pricePerKg >= 100.0 }.sortedByDescending { it.pricePerKg }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("价格 ≥ 100 元/kg", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp)) }
        if (herbs.isEmpty()) item { EmptyHint("暂无高价值药材") }
        items(herbs, key = { it.id }) { herb ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Text(herb.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text("¥${"%.2f".format(herb.pricePerKg)}/kg")
            }
            HorizontalDivider()
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
                Column(Modifier.weight(1f)) {
                    Text(prescription.dateString, fontWeight = FontWeight.SemiBold)
                    Text("共 ${prescription.items.size} 味", fontSize = 12.sp, color = Color.Gray)
                }
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
            val values = prescription.items.mapNotNull { item ->
                val stock = data.herbs.firstOrNull { it.name == item.herbName }?.stock ?: return@mapNotNull null
                if (item.grams > 0) stock / item.grams else null
            }
            val minDays = values.minOrNull()
            PageCard(Modifier.fillMaxWidth()) {
                Text(prescription.dateString, fontWeight = FontWeight.SemiBold)
                Text(if (minDays == null) "无法测算：处方药材未在库存中找到。" else "按最短药材计算：约 ${"%.1f".format(minDays)} 天", color = if ((minDays ?: 99.0) <= 7) Color.Red else Color(0xFF31864A), modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
fun RemainingDaysScreen(data: AppData, title: String, lowerExclusive: Double, upperInclusive: Double) {
    val herbs = data.herbs.filter { herb ->
        herb.dailyUsage > 0 && (herb.stock.toDouble() / herb.dailyUsage) > lowerExclusive && (herb.stock.toDouble() / herb.dailyUsage) <= upperInclusive
    }.sortedBy { it.stock.toDouble() / it.dailyUsage }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        if (herbs.isEmpty()) item { EmptyHint("暂无符合条件的药材") }
        items(herbs, key = { it.id }) { herb ->
            val days = herb.stock.toDouble() / herb.dailyUsage
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Text(herb.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text("约 ${"%.1f".format(days)} 天", color = if (days <= 7) Color.Red else Color(0xFFFF9500))
            }
            HorizontalDivider()
        }
    }
}
