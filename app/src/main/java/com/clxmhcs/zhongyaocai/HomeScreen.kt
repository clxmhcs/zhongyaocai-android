package com.clxmhcs.zhongyaocai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Current iOS HomeView equivalent. */
@Composable
fun HomeScreen(data: AppData, onRoute: (AppRoute) -> Unit, onSearch: (String) -> Unit = {}) {
    val low = data.herbs.filter { it.stock < it.warningLevel }.map { it.name }
    val seven = data.herbs.filter { it.dailyUsage > 0 && it.stock.toDouble() / it.dailyUsage <= 7.0 }.map { it.name }
    val fourteen = data.herbs.filter { it.dailyUsage > 0 && it.stock.toDouble() / it.dailyUsage > 7.0 && it.stock.toDouble() / it.dailyUsage <= 14.0 }.map { it.name }
    var materialQuery by rememberSaveable { mutableStateOf("") }
    var inventoryQuery by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFFD1EDFF), Color(0xFFEBF8FF)))),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth()) {
                IOSHomeAlert(low, "⚠️ 当前有库存不足药材：", Color.Red) { onRoute(AppRoute.LowStock) }
                if (seven.isNotEmpty()) IOSHomeAlert(seven, "⚠️ 当前有余量不足7天的药材：", Color.Red) { onRoute(AppRoute.Overview7Days) }
                else if (fourteen.isNotEmpty()) IOSHomeAlert(fourteen, "⚠️ 当前有余量不足14天的药材：", Color(0xFFFF9500)) { onRoute(AppRoute.Overview14Days) }
            }
        }
        item { Text("中药材库存管理", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) }
        item {
            Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                IOSHomeSearchCard(materialQuery, { materialQuery = it }, "模糊输入药材名称搜索药材资料", "显示：性味归经 / 功效主治 / 用法用量 / 禁忌", Color(0xFFFF9500)) { onRoute(AppRoute.Profiles) }
                IOSHomeSearchCard(inventoryQuery, { inventoryQuery = it }, "模糊输入药材名称快速查询药材信息", "显示：余量 / 预警值 / 用量 / 价格", Color(0xFF007AFF)) { onSearch(inventoryQuery.trim()) }
                IOSFastInOutHomeCard({ onRoute(AppRoute.Quick) }, { onRoute(AppRoute.FastInbound) }, { onRoute(AppRoute.FastOutbound) })
            }
        }
        item { HomeSectionTitle("库存管理") }
        item {
            Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IOSHomeTile("药材余量总览", "查看库存概览", Icons.Default.ListAlt, Color(0xFF34A853), Modifier.weight(1f)) { onRoute(AppRoute.InventoryOverview) }
                    IOSHomeTile("低库存药材", "按 预警值 统计", Icons.Default.WarningAmber, Color(0xFFFF9500), Modifier.weight(1f)) { onRoute(AppRoute.LowStock) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IOSHomeTile("高价值药材", "价格⩾100元", Icons.Default.Calculate, Color(0xFFFFCC00), Modifier.weight(1f)) { onRoute(AppRoute.HighValue) }
                    IOSHomeTile("药材信息设置", "价格/预警/删除/新增", Icons.Default.Settings, Color(0xFF007AFF), Modifier.weight(1f)) { onRoute(AppRoute.HerbSettings) }
                }
                IOSHomeWideTile("处方用量可用天数测算", "按当前处方用量测算库存可用天数", Icons.Default.Calculate, AppPurple) { onRoute(AppRoute.PrescriptionUsage) }
            }
        }
        item { HomeSectionTitle("统计分析") }
        item {
            Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IOSHomeTile("处方管理", "管理/对比处方", Icons.Default.Description, Color(0xFF00B8D4), Modifier.weight(1f)) { onRoute(AppRoute.Prescriptions) }
                    IOSHomeTile("药方总价计算", "处方费用统计", Icons.Default.Calculate, Color(0xFF34A853), Modifier.weight(1f)) { onRoute(AppRoute.TotalPrice) }
                }
                IOSHomeWideTile("导出/写入/备份数据       APP说明书", "数据库管理  或  查看app说明书  或  药材资料录入", Icons.Default.Inventory2, Color(0xFF007AFF)) { onRoute(AppRoute.Database) }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("©️夢幻傳說", fontSize = 12.sp, color = Color.Gray)
                Text("Gmail - clxmhcs", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun HomeSectionTitle(title: String) {
    Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 18.dp, top = 6.dp))
}
