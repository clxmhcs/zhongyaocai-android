package com.clxmhcs.zhongyaocai

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HerbListScreen(data: AppData, viewModel: MainViewModel, onAdd: () -> Unit, onLowStock: () -> Unit) {
    var keyword by rememberSaveable { mutableStateOf("") }
    var lowOnly by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Herb?>(null) }
    val list = data.herbs
        .filter { (!lowOnly || it.stock < it.warningLevel) && it.name.contains(keyword.trim()) }
        .sortedWith(compareBy<Herb> { it.pinyin }.thenBy { it.name })
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onAdd, modifier = Modifier.weight(1f)) { Text("新增药材", color = AppPurple) }
                TextButton(onClick = onLowStock, modifier = Modifier.weight(1f)) { Text("低库存药材", color = Color(0xFF007AFF)) }
            }
            OutlinedTextField(value = keyword, onValueChange = { keyword = it }, label = { Text("搜索药材名") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = lowOnly, onCheckedChange = { lowOnly = it })
                Text("只显示库存低于预警值的药材", fontSize = 14.sp)
            }
        }
        if (list.isEmpty()) item { EmptyHint("暂无药材数据") }
        items(list, key = { it.id }) { herb -> HerbRow(herb) { selected = herb } }
    }
    selected?.let { HerbEditDialog(it, viewModel) { selected = null } }
}

@Composable
fun HerbRow(herb: Herb, onClick: () -> Unit = {}) {
    val low = herb.stock < herb.warningLevel
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(herb.name, Modifier.weight(1f), color = if (low) Color.Red else Color.Unspecified, maxLines = 1, overflow = TextOverflow.Clip)
        Text("${herb.stock} g", color = if (low) Color.Red else Color.Unspecified)
    }
    HorizontalDivider(color = Color(0x1F000000))
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
        confirmButton = { TextButton(onClick = {
            val updated = herb.copy(stock = stock.toIntOrNull() ?: -1, warningLevel = warning.toIntOrNull() ?: -1, dailyUsage = daily.toIntOrNull() ?: -1, pricePerKg = price.toDoubleOrNull() ?: -1.0)
            viewModel.updateHerb(updated) { result -> if (result == null) onDismiss() else error = result }
        }) { Text("保存") } },
        dismissButton = { Row { TextButton(onClick = { deleteConfirm = true }) { Text("删除", color = Color.Red) }; TextButton(onClick = onDismiss) { Text("取消") } } }
    )
    if (deleteConfirm) AlertDialog(
        onDismissRequest = { deleteConfirm = false },
        title = { Text("确定删除 ${herb.name} 吗？") },
        text = { Text("删除药材不会删除已有处方、入库或支出记录。") },
        confirmButton = { TextButton(onClick = { viewModel.deleteHerb(herb.id); onDismiss() }) { Text("删除", color = Color.Red) } },
        dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("取消") } }
    )
}

/** Current iOS LowStockView: fixed-width table and strict stock < warning comparison. */
@Composable
fun LowStockScreen(data: AppData) {
    val low = data.herbs.filter { it.stock < it.warningLevel }.sortedWith(compareBy<Herb> { it.pinyin }.thenBy { it.name })
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("低库存药材", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {}) { Text("刷新", color = Color(0xFF007AFF)) }
        }
        if (low.isEmpty()) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("⚠️暂无低库存药材", color = Color.Gray)
                Text("当库存低于预警值时会自动出现在这里。", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
            }
        } else {
            Column(Modifier.horizontalScroll(scroll)) {
                LowStockHeader()
                HorizontalDivider(color = Color(0xFFD9D9D9))
                LazyColumn(Modifier.width(390.dp).fillMaxSize()) {
                    items(low, key = { it.id }) { herb ->
                        LowStockRow(herb)
                        HorizontalDivider(color = Color(0x59000000))
                    }
                }
            }
        }
    }
}

@Composable
private fun LowStockHeader() {
    Row(Modifier.width(390.dp).padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text("药材名", Modifier.width(96.dp), color = Color.Gray, fontSize = 14.sp)
        Text("库存(g)", Modifier.width(60.dp), color = Color.Gray, fontSize = 14.sp)
        Text("预警值", Modifier.width(64.dp), color = Color.Gray, fontSize = 14.sp)
        Text("日用量", Modifier.width(64.dp), color = Color.Gray, fontSize = 14.sp)
        Text("价格(元/kg)", Modifier.width(90.dp), color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
private fun LowStockRow(herb: Herb) {
    Row(Modifier.width(390.dp).padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(herb.name, Modifier.width(96.dp), color = Color.Red, maxLines = 1, overflow = TextOverflow.Clip)
        Text("${herb.stock} g", Modifier.width(60.dp), color = Color.Red, maxLines = 1)
        Text("${herb.warningLevel} g", Modifier.width(64.dp), maxLines = 1)
        Text("${herb.dailyUsage} g", Modifier.width(64.dp), maxLines = 1)
        Text("${"%.2f".format(herb.pricePerKg)}", Modifier.width(90.dp), maxLines = 1)
    }
}

/** Current iOS AddHerbView: two-column inputs and paired purple actions. */
@Composable
fun AddHerbScreen(data: AppData, viewModel: MainViewModel, onDone: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var stock by rememberSaveable { mutableStateOf("") }
    var warning by rememberSaveable { mutableStateOf("") }
    var daily by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    var lastAddedName by remember { mutableStateOf<String?>(null) }
    var clearConfirm by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(name, { input -> name = input.filter { ch -> ch.code in 0x3400..0x4DBF || ch.code in 0x4E00..0x9FFF } }, label = { Text("药材名（必填，仅限汉字）") }, modifier = Modifier.weight(1f), singleLine = true)
                    NumberField("库存 (g)", stock) { stock = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    NumberField("预警值 (g)", warning) { warning = it }
                    NumberField("日用量 (g)", daily) { daily = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
                    RoundedActionButton("添加", AppPurple, onClick = {
                        val submitted = name
                        viewModel.addHerb(submitted, stock.toIntOrNull() ?: 0, warning.toIntOrNull() ?: 0, daily.toIntOrNull() ?: 0, 0.0) { message ->
                            result = message ?: "新增药材成功。"
                            if (message == null) { lastAddedName = submitted; name = ""; stock = ""; warning = ""; daily = "" }
                        }
                    }, modifier = Modifier.weight(1f))
                    RoundedActionButton("撤销", AppPurple, lastAddedName != null, onClick = {
                        lastAddedName?.let { added ->
                            viewModel.undoLatestAddedHerb(added) { success ->
                                result = if (success) "已撤销本次新增。" else "无法撤销本次新增。"
                                if (success) lastAddedName = null
                            }
                        }
                    }, modifier = Modifier.weight(1f))
                }
                result?.let { text ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(text)
                        if (text.contains("成功")) Text("如有重名会提示并阻止写入。", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
        item { HorizontalDivider() }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("历史新增记录", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { clearConfirm = true }) { Text("清空历史", color = AppPurple) }
            }
        }
        if (data.addHistories.isEmpty()) item { EmptyHint("暂无新增记录") }
        items(data.addHistories.sortedByDescending { it.createdAt }, key = { it.id }) { record ->
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("${record.name}  ${record.amount}g")
                Text(fullDateLabel(record.createdAt), fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
    if (clearConfirm) AlertDialog(
        onDismissRequest = { clearConfirm = false },
        title = { Text("清空历史新增记录？") },
        text = { Text("只清空新增记录，不影响现有药材及其库存。") },
        confirmButton = { TextButton(onClick = { viewModel.clearAddHistory(); clearConfirm = false }) { Text("清空", color = Color.Red) } },
        dismissButton = { TextButton(onClick = { clearConfirm = false }) { Text("取消") } }
    )
}
