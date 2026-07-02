package com.clxmhcs.zhongyaocai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HerbListScreen(data: AppData, viewModel: MainViewModel, onAdd: () -> Unit, onLowStock: () -> Unit) {
    var keyword by rememberSaveable { mutableStateOf("") }
    var lowOnly by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Herb?>(null) }
    val list = data.herbs.filter { (!lowOnly || it.stock <= it.warningLevel) && it.name.contains(keyword.trim()) }
        .sortedWith(compareBy<Herb> { it.pinyin }.thenBy { it.name })
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundedActionButton("新增药材", AppPurple, onClick = onAdd, modifier = Modifier.weight(1f))
                RoundedActionButton("低库存（${data.herbs.count { it.stock <= it.warningLevel }}）", AppOrange, onClick = onLowStock, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = keyword, onValueChange = { keyword = it }, label = { Text("搜索药材") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = lowOnly, onCheckedChange = { lowOnly = it }); Text("只显示库存不足或等于预警值的药材") }
        }
        if (list.isEmpty()) item { EmptyHint("暂无符合条件的药材。可通过“新增药材”录入，或从 Excel 覆盖导入。") }
        items(list, key = { it.id }) { herb -> HerbRow(herb) { selected = herb } }
        item { Spacer(Modifier.height(76.dp)) }
    }
    selected?.let { HerbEditDialog(it, viewModel) { selected = null } }
}

@Composable
fun HerbRow(herb: Herb, onClick: () -> Unit = {}) {
    val low = herb.stock <= herb.warningLevel
    PageCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(herb.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Text("预警 ${herb.warningLevel}g · 日用量 ${herb.dailyUsage}g · ¥${"%.2f".format(herb.pricePerKg)}/kg", color = Color.Gray, fontSize = 12.sp)
            }
            Text("${herb.stock}g", color = if (low) Color(0xFFD85457) else Color(0xFF31864A), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun HerbEditDialog(herb: Herb, viewModel: MainViewModel, onDismiss: () -> Unit) {
    var stock by remember { mutableStateOf(herb.stock.toString()) }
    var warning by remember { mutableStateOf(herb.warningLevel.toString()) }
    var daily by remember { mutableStateOf(herb.dailyUsage.toString()) }
    var price by remember { mutableStateOf(if (herb.pricePerKg % 1.0 == 0.0) herb.pricePerKg.toInt().toString() else herb.pricePerKg.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    var deleteConfirm by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑 ${herb.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NumberField("库存(g)", stock) { stock = it }
                NumberField("预警值(g)", warning) { warning = it }
                NumberField("日用量(g)", daily) { daily = it }
                NumberField("价格(元/kg)", price, true) { price = it }
                error?.let { Text(it, color = Color.Red, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val updated = herb.copy(stock = stock.toIntOrNull() ?: -1, warningLevel = warning.toIntOrNull() ?: -1, dailyUsage = daily.toIntOrNull() ?: -1, pricePerKg = price.toDoubleOrNull() ?: -1.0)
                viewModel.updateHerb(updated) { result -> if (result == null) onDismiss() else error = result }
            }) { Text("保存") }
        },
        dismissButton = { Row { TextButton(onClick = { deleteConfirm = true }) { Text("删除", color = Color.Red) }; TextButton(onClick = onDismiss) { Text("取消") } } }
    )
    if (deleteConfirm) AlertDialog(onDismissRequest = { deleteConfirm = false }, title = { Text("确定删除 ${herb.name} 吗？") }, text = { Text("删除药材不会删除已有处方、入库或支出记录。") }, confirmButton = { TextButton(onClick = { viewModel.deleteHerb(herb.id); onDismiss() }) { Text("删除", color = Color.Red) } }, dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("取消") } })
}

@Composable
fun LowStockScreen(data: AppData) {
    val low = data.herbs.filter { it.stock <= it.warningLevel }.sortedBy { it.stock - it.warningLevel }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("低库存药材（${low.size} 味）", fontWeight = FontWeight.Bold) }
        if (low.isEmpty()) item { EmptyHint("当前没有低库存药材。") }
        items(low, key = { it.id }) { HerbRow(it) }
    }
}

@Composable
fun AddHerbScreen(data: AppData, viewModel: MainViewModel, onDone: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var stock by rememberSaveable { mutableStateOf("") }
    var warning by rememberSaveable { mutableStateOf("") }
    var daily by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    var clearConfirm by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            PageCard {
                Text("药材名必填且仅限汉字；库存、预警值、日用量可留空，留空按 0 处理。", color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { input -> name = input.filter { ch -> ch.code in 0x3400..0x4DBF || ch.code in 0x4E00..0x9FFF } }, label = { Text("药材名（必填，仅限汉字）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                NumberField("库存(g)", stock) { stock = it }
                NumberField("预警值(g)", warning) { warning = it }
                NumberField("日用量(g)", daily) { daily = it }
                NumberField("价格(元/kg)", price, true) { price = it }
                Spacer(Modifier.height(10.dp))
                RoundedActionButton("添加", AppPurple, onClick = {
                    viewModel.addHerb(name, stock.toIntOrNull() ?: 0, warning.toIntOrNull() ?: 0, daily.toIntOrNull() ?: 0, price.toDoubleOrNull() ?: 0.0) { message ->
                        result = message ?: "新增药材成功。"
                        if (message == null) { name = ""; stock = ""; warning = ""; daily = ""; price = "" }
                    }
                }, modifier = Modifier.fillMaxWidth())
                result?.let { Text(it, color = if (it.contains("成功")) Color(0xFF31864A) else Color.Red, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("历史新增记录", fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); TextButton(onClick = { clearConfirm = true }) { Text("清空历史", color = AppPurple) } } }
        if (data.addHistories.isEmpty()) item { EmptyHint("暂无新增记录。") }
        items(data.addHistories.sortedByDescending { it.createdAt }, key = { it.id }) { history -> PageCard { Text("${history.name}  ${history.amount}g"); Text(fullDateLabel(history.createdAt), color = Color.Gray, fontSize = 12.sp) } }
        item { RoundedActionButton("完成", AppCyan, onClick = onDone, modifier = Modifier.fillMaxWidth()) }
    }
    if (clearConfirm) AlertDialog(onDismissRequest = { clearConfirm = false }, title = { Text("清空历史新增记录？") }, text = { Text("只清空新增记录，不影响现有药材及其库存。") }, confirmButton = { TextButton(onClick = { viewModel.clearAddHistory(); clearConfirm = false }) { Text("清空", color = Color.Red) } }, dismissButton = { TextButton(onClick = { clearConfirm = false }) { Text("取消") } })
}
