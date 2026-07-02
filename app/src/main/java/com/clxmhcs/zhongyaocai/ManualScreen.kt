package com.clxmhcs.zhongyaocai

import androidx.compose.foundation.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Megaphone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

private data class ManualSectionData(val title: String, val lines: List<String>, val defaultExpanded: Boolean = true)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualScreen(expandAll: Boolean) {
    var searchText by rememberSaveable { mutableStateOf("") }
    val sections = remember {
        listOf(
            ManualSectionData("功能概览", listOf("快速查询：按药材名快速查看库存/价格/预警值。", "快速入库/支出：快速登记出入库记录。", "低库存药材：自动汇总库存不足列表，并在主页公告条提醒。", "库存管理：新增药材、设置价格、预警设置等。", "统计分析：处方历史记录、处方总价计算等。", "数据库管理：导出/导入Excel、备份/恢复数据库。")),
            ManualSectionData("快速查询", listOf("输入药材名（支持中文）即可查询：库存、价格、预警值等信息。", "建议：常用药材可通过拼音/首字母录入（如有）。")),
            ManualSectionData("快速入库 / 支出", listOf("用于日常进货/出库登记。", "注意：清空操作只影响该页面的临时输入，不会影响单独保存的入库历史库（如你已启用双 Realm 方案）。")),
            ManualSectionData("低库存药材", listOf("当库存低于预警值时，会出现在低库存列表。", "主页公告条可点击直接进入低库存页面。")),
            ManualSectionData("库存管理", listOf("新增药材：新增一味药材并设置初始库存等信息。", "设置药材价格：用于处方总价计算。", "预警设置：设置库存预警阈值。")),
            ManualSectionData("统计分析", listOf("处方历史记录：浏览历史处方。", "药方总价计算：根据药材价格计算处方总费用。")),
            ManualSectionData("Excel 导入（严格校验）", listOf("仅支持 .xlsx 文件。", "表头必须包含：药材名、库存(g)、预警值(g)、价格(元/kg)。列顺序可变。", "校验规则：任一行缺字段/格式错误/药材名重复，会拒绝整次导入并提示错误行号与原因。", "覆盖导入：会清空现有药材数据，且无法回退。建议导入前先备份。")),
            ManualSectionData("数据库备份与恢复", listOf("备份：导出数据库文件用于保存。", "恢复：选择备份文件恢复数据。", "提示：恢复后如未立即生效，可按页面提示手动重启 App。")),
            ManualSectionData("常见问题（FAQ）", listOf("Q：为什么导入提示“导入被拒绝”？\nA：说明 Excel 中至少有一行数据不完整/格式不对/重复药材名，请按提示行号修正后再导入。", "Q：为什么低库存公告条有时不滚动？\nA：主页每次出现会强制重启动画；如果你刚更新库存，公告条也会刷新。", "Q：导入后数据不对怎么办？\nA：覆盖导入不可回退，请先用备份功能导出数据库文件。"), false),
            ManualSectionData("联系信息", listOf("如需反馈问题，请准备：截图、操作步骤、导入文件样例（如有），便于定位。"), false),
            ManualSectionData("版本信息", listOf("中药材库存管理系统", "版本：1.0"))
        )
    }
    val visible = sections.filter { section ->
        searchText.isBlank() || section.title.contains(searchText, true) || section.lines.any { it.contains(searchText, true) }
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().background(Color.White.copy(alpha = .72f), RoundedCornerShape(14.dp)).padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Megaphone, "使用提示", tint = AppOrange)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("使用提示", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Excel 导入属于覆盖性操作，导入前请先备份数据库；导入会进行严格校验，任一行错误都会拒绝导入。", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        item {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("搜索说明书（例如：Excel / 预警 / 备份）") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth()) { Text("目录", fontSize = 16.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Text("点击跳转", fontSize = 12.sp, color = Color.Gray) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    visible.forEach { section ->
                        Text(section.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.background(Color.Black.copy(alpha = .06f), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 7.dp))
                    }
                }
            }
        }
        item { HorizontalDivider() }
        items(visible.size) { index -> ManualSectionCard(visible[index], expandAll, searchText) }
        item { HorizontalDivider() }
        item {
            Column {
                Text("提示", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("如果你希望说明书支持图片/视频/更新日志，我可以继续扩展为多页面说明中心。", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun ManualSectionCard(section: ManualSectionData, expandAll: Boolean, searchText: String) {
    var expanded by remember(section.title) { mutableStateOf(section.defaultExpanded) }
    LaunchedEffect(expandAll) { expanded = expandAll }
    val showing = searchText.isBlank() || section.title.contains(searchText, true) || section.lines.any { it.contains(searchText, true) }
    if (!showing) return
    Column(
        Modifier.fillMaxWidth().background(Color.White.copy(alpha = .72f), RoundedCornerShape(14.dp)).clickable { expanded = !expanded }.padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(section.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Color.Gray)
        }
        if (expanded) {
            section.lines.forEach { line ->
                Text("• $line", fontSize = 13.sp, color = Color(0xFF4D4D4D), modifier = Modifier.padding(top = 8.dp))
            }
            if (section.title == "联系信息") Text("QQ：1252688256", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
    }
}
