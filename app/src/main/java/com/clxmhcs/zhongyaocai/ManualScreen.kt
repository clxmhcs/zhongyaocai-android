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
import androidx.compose.foundation.layout.weight
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

private data class ManualSectionData(val title: String, val lines: List<String>, val initiallyOpen: Boolean = true)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualScreen(expandAll: Boolean) {
    var query by rememberSaveable { mutableStateOf("") }
    val sections = remember {
        listOf(
            ManualSectionData("功能概览", listOf("快速查询、出入库、低库存、库存管理、处方与数据库管理。")),
            ManualSectionData("快速查询", listOf("输入药材名可查询库存、价格与预警值。")),
            ManualSectionData("快速入库 / 支出", listOf("用于日常进货与出库登记；清空只清临时输入或对应历史记录。")),
            ManualSectionData("低库存药材", listOf("库存低于预警值时，药材会显示在低库存列表。")),
            ManualSectionData("库存管理", listOf("可新增药材、设置价格和库存预警。")),
            ManualSectionData("统计分析", listOf("可浏览历史处方并计算药方总价。")),
            ManualSectionData("Excel 导入（严格校验）", listOf("只支持 .xlsx。任一行缺字段、格式错误或重名都会拒绝整次导入。覆盖导入前应先备份。")),
            ManualSectionData("数据库备份与恢复", listOf("可导出数据库文件，并通过备份文件恢复数据。")),
            ManualSectionData("常见问题（FAQ）", listOf("导入被拒绝时，请按提示修正错误行。覆盖导入不可回退。"), false),
            ManualSectionData("联系信息", listOf("反馈问题时请提供截图、操作步骤和文件样例。"), false),
            ManualSectionData("版本信息", listOf("中药材库存管理系统", "版本：1.0"))
        )
    }
    val visible = sections.filter { it.title.contains(query, true) || it.lines.any { line -> line.contains(query, true) } || query.isBlank() }

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
                    Text("Excel 导入属于覆盖性操作，导入前请先备份数据库；导入会进行严格校验。", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("搜索说明书（例如：Excel / 预警 / 备份）") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text("目录", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("点击跳转", fontSize = 12.sp, color = Color.Gray)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    visible.forEach { section ->
                        Text(section.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.background(Color.Black.copy(alpha = .06f), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 7.dp))
                    }
                }
            }
        }
        item { HorizontalDivider() }
        visible.forEach { section -> item { ManualSectionCard(section, expandAll) } }
        item { HorizontalDivider() }
        item {
            Column {
                Text("提示", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("如果需要图片、视频或更新日志，可继续扩展为多页面说明中心。", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun ManualSectionCard(section: ManualSectionData, expandAll: Boolean) {
    var open by remember(section.title) { mutableStateOf(section.initiallyOpen) }
    LaunchedEffect(expandAll) { open = expandAll }
    Column(
        Modifier.fillMaxWidth().background(Color.White.copy(alpha = .72f), RoundedCornerShape(14.dp)).clickable { open = !open }.padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(section.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Color.Gray)
        }
        if (open) {
            section.lines.forEach { line -> Text("• $line", fontSize = 13.sp, color = Color(0xFF4D4D4D), modifier = Modifier.padding(top = 8.dp)) }
            if (section.title == "联系信息") Text("QQ：1252688256", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
