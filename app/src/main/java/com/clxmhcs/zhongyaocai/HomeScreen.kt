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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(data: AppData, onRoute: (AppRoute) -> Unit) {
    val low = data.herbs.filter { it.stock <= it.warningLevel }
    var keyword by rememberSaveable { mutableStateOf("") }
    val found = data.herbs.filter { it.name.contains(keyword.trim()) }.take(5)
    LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("中药材管理库", fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("离线保存 · 库存、处方、资料统一管理", color = Color.Gray, fontSize = 13.sp) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { SummaryCard("药材", data.herbs.size.toString(), AppCyan, Modifier.weight(1f)) { onRoute(AppRoute.Herbs) }; SummaryCard("低库存", low.size.toString(), if (low.isEmpty()) Color(0xFF65B576) else Color(0xFFE56B6F), Modifier.weight(1f)) { onRoute(AppRoute.LowStock) }; SummaryCard("处方", data.prescriptions.size.toString(), AppPurple, Modifier.weight(1f)) { onRoute(AppRoute.Prescriptions) } } }
        item { PageCard { Text("快速查询", fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp)); OutlinedTextField(value = keyword, onValueChange = { keyword = it }, label = { Text("输入药材名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true); if (keyword.isNotBlank()) { Spacer(Modifier.height(8.dp)); if (found.isEmpty()) Text("未找到匹配药材", color = Color.Gray) else found.forEach { Text("${it.name}   余量 ${it.stock}g", Modifier.padding(vertical = 4.dp)) } } } }
        item { Text("常用功能", fontWeight = FontWeight.SemiBold, fontSize = 17.sp) }
        item { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { RoundedActionButton("快速入库 / 支出", AppCyan, onClick = { onRoute(AppRoute.Quick) }, modifier = Modifier.fillMaxWidth()); RoundedActionButton("新增药材", AppPurple, onClick = { onRoute(AppRoute.AddHerb) }, modifier = Modifier.fillMaxWidth()); RoundedActionButton("保存处方", AppOrange, onClick = { onRoute(AppRoute.AddPrescription) }, modifier = Modifier.fillMaxWidth()); RoundedActionButton("数据库管理", Color(0xFF5AB8B7), onClick = { onRoute(AppRoute.Database) }, modifier = Modifier.fillMaxWidth()) } }
        if (low.isNotEmpty()) item { PageCard { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Warning, "预警", tint = Color(0xFFE46B6C)); Spacer(Modifier.width(6.dp)); Text("库存预警", fontWeight = FontWeight.SemiBold) }; low.take(5).forEach { herb -> Text("${herb.name}：${herb.stock}g（预警 ${herb.warningLevel}g）", color = Color(0xFFD85457), modifier = Modifier.padding(top = 6.dp)) }; if (low.size > 5) Text("还有 ${low.size - 5} 味药材", color = Color.Gray, modifier = Modifier.padding(top = 4.dp)) } }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = color)) { Column(Modifier.padding(13.dp)) { Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp); Text(label, color = Color.White, fontSize = 12.sp) } }
}
