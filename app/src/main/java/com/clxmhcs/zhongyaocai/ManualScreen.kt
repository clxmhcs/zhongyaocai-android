package com.clxmhcs.zhongyaocai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ManualScreen() {
    val sections = listOf(
        "功能概览" to "本应用在手机本地保存药材库存、快速入库与支出记录、处方、药材资料和备份。未接入云端账号，不会自动上传数据。",
        "新增药材" to "药材名为必填项，且仅允许输入汉字。库存、预警值、日用量和价格允许留空，留空按 0 处理。相同药材名会被拒绝写入。",
        "快速入库" to "每行输入“药材名 + 整数克数”，如“白术18”或“白术 18g”。任何一行药材不存在或格式不正确，整次入库均不会执行。",
        "快速支出" to "支出支持小数克数。每味药使用独立的累计进一余额，确保连续小数支出的库存误差小于 1g。提交前会汇总重复药材，统一校验库存，库存不足不会扣减。",
        "处方" to "保存处方不会自动扣库存。多选两条处方可以查看变量对比并复制；选择超过两条时可导出 Excel。重要处方不能直接删除，需先在详情中取消重要标记。",
        "Excel" to "库存 Excel 导出为标准 .xlsx，库存小于或等于预警值的药材会整行标红。导入前进行整表校验，发现任意错误时不会改动当前数据；确认覆盖后仅更新药材库存、预警值、日用量与价格。",
        "备份与恢复" to "备份文件为 JSON，包含药材、出入库记录、处方、药材资料以及小数支出余额。恢复会替换当前本地数据，操作前应先导出备份。",
        "药材余量重置" to "该功能只会将所有药材余量设置为 0，不会清除药材名称、预警值、日用量、价格、处方或历史记录。首次使用先设置密码，再次点击并输入正确密码后才会执行。",
        "药材资料" to "药材资料可记录别名、药用部位、性味、归经、功效、主治、配伍、用法用量和禁忌，并可单独导出 JSON。"
    )
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("APP说明书", fontWeight = FontWeight.Bold, fontSize = 22.sp); Text("中药材管理 Android 版", color = Color.Gray) }
        items(sections.size) { index ->
            val (title, body) = sections[index]
            PageCard { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp); Text(body, color = Color(0xFF505050), modifier = Modifier.padding(top = 6.dp)) }
        }
    }
}
