package com.clxmhcs.zhongyaocai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class PriceChange(val herbId: String, val name: String, val oldPrice: Double, val newPrice: Double)

@Composable
fun HerbSettingsHubScreen(onAdd: () -> Unit, onPrice: () -> Unit, onWarning: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFFD1EDFF), Color(0xFFEBF8FF)))).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("药材信息设置", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        HerbSettingsCard("新增药材", "添加新的药材信息", Icons.Default.Add, onAdd)
        HerbSettingsCard("设置药材价格", "快速设置药材价格", Icons.Default.AttachMoney, onPrice)
        HerbSettingsCard("药材预警信息设置/删除药材", "设置库存预警、日用量 或 删除药材", Icons.Default.NotificationsActive, onWarning)
    }
}

@Composable
private fun HerbSettingsCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White.copy(alpha = .55f), RoundedCornerShape(22.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).background(Brush.linearGradient(listOf(Color(0xFFFF7AA2), Color(0xFFFFC47B))), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) { Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, fontSize = 13.sp, color = Color.Gray) }
        Icon(Icons.Default.ArrowForwardIos, null, tint = Color.Gray)
    }
}

@Composable
fun PriceSettingsScreen(data: AppData, viewModel: MainViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var highlight by remember { mutableStateOf<String?>(null) }
    var lastChange by remember { mutableStateOf<PriceChange?>(null) }
    val herbs = data.herbs.sortedByDescending { it.pricePerKg }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        item {
            Column(Modifier.padding(16.dp).background(Color.White.copy(alpha = .8f), RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                message?.let { text ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text, modifier = Modifier.weight(1f))
                        if (lastChange != null) {
                            OutlinedButton(onClick = {
                                val change = lastChange ?: return@OutlinedButton
                                val current = data.herbs.firstOrNull { it.id == change.herbId }
                                if (current == null) {
                                    message = "⚠️撤销失败：找不到对应药材（可能已删除）。"
                                    lastChange = null
                                } else {
                                    viewModel.updateHerb(current.copy(pricePerKg = change.oldPrice)) { error ->
                                        if (error == null) {
                                            highlight = current.name
                                            message = "「${current.name}」 价格变更已撤销，恢复为原价格： ${"%.2f".format(change.oldPrice)。"
                                            lastChange = null
                                        } else message = error
                                    }
                                }
                            }) { Text("撤销") }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(name, { name = it }, label = { Text("药材名") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(price, { price = it }, label = { Text("价格") }, modifier = Modifier.width(100.dp), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = {
                        if (name.isBlank()) message = "⚠️请先输入要搜索的药材名。"
                        else if (data.herbs.none { it.name == name.trim() }) message = "⚠️当前输入 【${name.trim()}】 药材不存在，请重新输入。"
                        else { highlight = name.trim(); message = null }
                    }) { Text("搜索", color = Color(0xFFFF9500)) }
                    Button(onClick = {
                        val herb = data.herbs.firstOrNull { it.name == name.trim() }
                        val next = price.toDoubleOrNull()
                        if (herb == null) message = "⚠️当前输入药材不存在，请到新增药材页面操作！"
                        else if (next == null) message = "⚠️价格格式无效，请输入有效数字。"
                        else {
                            val old = herb.pricePerKg
                            viewModel.updateHerb(herb.copy(pricePerKg = next)) { error ->
                                if (error == null) {
                                    message = "「${herb.name}」 价格已由 ${"%.2f".format(old)} 变更为： ${"%.2f".format(next)}。"
                                    lastChange = PriceChange(herb.id, herb.name, old, next)
                                    highlight = herb.name
                                } else message = error
                            }
                            name = ""; price = ""
                        }
                    }) { Text("更新") }
                }
            }
        }
        item { Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) { Text("药材名", Modifier.width(100.dp), color = Color.Gray); Text("价格(元/kg)", Modifier.width(100.dp), color = Color.Gray); Text("库存", color = Color.Gray) }; HorizontalDivider() }
        items(herbs, key = { it.id }) { herb ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                val hit = highlight == herb.name
                Text(herb.name, Modifier.width(100.dp), color = if (hit) Color.Red else Color.Unspecified)
                Text("${"%.2f".format(herb.pricePerKg)}", Modifier.width(100.dp), color = if (hit) Color.Red else Color.Unspecified)
                Text(herb.stock.toString(), color = if (hit) Color.Red else Color.Unspecified)
            }
        }
    }
}

@Composable
fun WarningSettingsScreen(data: AppData, viewModel: MainViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var warning by rememberSaveable { mutableStateOf("") }
    var daily by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<Herb?>(null) }
    val herbs = data.herbs.filter { it.name.contains(query.trim()) }.sortedWith(compareBy<Herb> { it.pinyin }.thenBy { it.name })
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Color(0xFFF2F2F7), RoundedCornerShape(10.dp)).padding(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("药材名") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    OutlinedTextField(warning, { warning = it.filter(Char::isDigit) }, label = { Text("预警值") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(daily, { daily = it.filter(Char::isDigit) }, label = { Text("日用量") }, modifier = Modifier.weight(1f), singleLine = true)
                    Button(onClick = {
                        val herb = data.herbs.firstOrNull { it.name == name.trim() }
                        if (herb == null) message = "⚠️药材不存在，请去新增药材页面操作！"
                        else viewModel.updateHerb(herb.copy(warningLevel = warning.toIntOrNull() ?: herb.warningLevel, dailyUsage = daily.toIntOrNull() ?: herb.dailyUsage)) { error -> message = error ?: "已更新 ${herb.name}"; name = ""; warning = ""; daily = "" }
                    }) { Text("更新") }
                }
            }
        }
        item { OutlinedTextField(query, { query = it }, label = { Text("输入药材名以搜索") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { Row(Modifier.fillMaxWidth()) { Text("药材名", Modifier.width(95.dp), fontWeight = FontWeight.Bold); Text("预警值", Modifier.width(70.dp), fontWeight = FontWeight.Bold); Text("日用量", Modifier.width(70.dp), fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("操作", fontWeight = FontWeight.Bold) }; HorizontalDivider() }
        items(herbs, key = { it.id }) { herb ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(herb.name, Modifier.width(95.dp)); Text("${herb.warningLevel}", Modifier.width(70.dp)); Text("${herb.dailyUsage}", Modifier.width(70.dp)); Spacer(Modifier.weight(1f)); Icon(Icons.Default.Delete, "删除", tint = Color.Red, modifier = Modifier.clickable { deleteTarget = herb })
            }
        }
        message?.let { item { Text(it, color = Color(0xFFFF9500)) } }
    }
    deleteTarget?.let { herb -> AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("确认删除？") }, text = { Text("⚠️当前操作会永久删除 【${herb.name}】 的所有信息，确认继续？") }, confirmButton = { Button(onClick = { viewModel.deleteHerb(herb.id); deleteTarget = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("删除") } }, dismissButton = { OutlinedButton(onClick = { deleteTarget = null }) { Text("取消") } }) }
}
