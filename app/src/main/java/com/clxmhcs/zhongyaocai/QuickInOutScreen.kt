package com.clxmhcs.zhongyaocai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert

@Composable
fun QuickInOutScreen(data: AppData, viewModel: MainViewModel) {
    var inbound by rememberSaveable { mutableStateOf("") }
    var outbound by rememberSaveable { mutableStateOf("") }
    var alertText by remember { mutableStateOf<String?>(null) }
    var confirmInbound by remember { mutableStateOf<InboundValidation?>(null) }
    var confirmOutbound by remember { mutableStateOf<OutboundValidation?>(null) }
    var clearChoice by remember { mutableStateOf(false) }
    var clearTarget by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            PageCard {
                Text("快速入库", fontWeight = FontWeight.Bold, color = AppCyan)
                Text("每行：药材名 + 整数克数，例如：白术18 或 白术 18g。药材必须已存在。", color = Color.Gray, fontSize = 12.sp)
                OutlinedTextField(value = inbound, onValueChange = { inbound = it }, modifier = Modifier.fillMaxWidth().height(130.dp), label = { Text("入库内容") })
                Spacer(Modifier.height(8.dp))
                RoundedActionButton("校验并入库", AppCyan, onClick = {
                    val check = InventoryLineParser.validateInbound(inbound, data.herbs)
                    when {
                        inbound.isBlank() -> alertText = "请先输入入库内容。"
                        check.invalidOrMissing.isNotEmpty() -> alertText = "以下药材不存在或格式错误：\n${check.invalidOrMissing.joinToString("\n")}" 
                        check.items.isEmpty() -> alertText = "未识别到可入库的数据。"
                        else -> confirmInbound = check
                    }
                }, modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            PageCard {
                Text("快速支出", fontWeight = FontWeight.Bold, color = AppPurple)
                Text("每行：药材名 + 克数。可填写小数；按每味药分别累计进一扣库存，累计误差小于 1g。", color = Color.Gray, fontSize = 12.sp)
                OutlinedTextField(value = outbound, onValueChange = { outbound = it }, modifier = Modifier.fillMaxWidth().height(130.dp), label = { Text("支出内容") })
                Spacer(Modifier.height(8.dp))
                RoundedActionButton("校验并支出", AppPurple, onClick = {
                    val check = InventoryLineParser.validateOutbound(outbound, data.herbs, data.outboundBalances)
                    when {
                        outbound.isBlank() -> alertText = "请先输入支出内容。"
                        check.invalidOrMissing.isNotEmpty() && check.insufficient.isNotEmpty() -> alertText = "格式错误或药材不存在：\n${check.invalidOrMissing.joinToString("\n")}\n\n库存不足：\n${check.insufficient.joinToString("\n") { "${it.first}（当前 ${it.second}g）" }}"
                        check.invalidOrMissing.isNotEmpty() -> alertText = "以下药材不存在或格式错误：\n${check.invalidOrMissing.joinToString("\n")}" 
                        check.insufficient.isNotEmpty() -> alertText = "以下药材库存不足：\n${check.insufficient.joinToString("\n") { "${it.first}（当前 ${it.second}g）" }}"
                        else -> confirmOutbound = check
                    }
                }, modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("历史记录", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { clearChoice = true }) { androidx.compose.material3.Icon(Icons.Default.MoreVert, "清空记录") }
            }
        }
        val records = (data.inRecords.map { true to it } + data.outRecords.map { false to it }).sortedByDescending { it.second.createdAt }
        if (records.isEmpty()) item { EmptyHint("暂无快速入库或支出记录。处方保存不会自动扣减库存。") }
        items(records, key = { it.second.id }) { pair ->
            val isInbound = pair.first
            val record = pair.second
            PageCard { Row(Modifier.fillMaxWidth()) { Text(if (isInbound) "入库" else "支出", color = if (isInbound) AppCyan else AppPurple, fontWeight = FontWeight.Bold); Spacer(Modifier.width(12.dp)); Column { Text("${record.name}  ${record.amount}g"); Text(fullDateLabel(record.createdAt), color = Color.Gray, fontSize = 12.sp) } } }
        }
        item { Spacer(Modifier.height(70.dp)) }
    }
    confirmInbound?.let { check -> AlertDialog(
        onDismissRequest = { confirmInbound = null },
        title = { Text("确认入库") },
        text = { Text("本次将入库：\n${check.items.joinToString("\n") { "${it.first} ${it.second}g" }}") },
        confirmButton = { TextButton(onClick = { viewModel.commitInbound(check.items); inbound = ""; confirmInbound = null; alertText = "入库成功。" }) { Text("确认") } },
        dismissButton = { TextButton(onClick = { confirmInbound = null }) { Text("取消") } }
    ) }
    confirmOutbound?.let { check -> AlertDialog(
        onDismissRequest = { confirmOutbound = null },
        title = { Text("确认支出") },
        text = { Text(if (check.deductions.isEmpty()) "本次支出已由累计余额抵扣，不会扣减整数库存。" else "实际扣减：\n${check.deductions.joinToString("\n") { "${it.first} ${it.second}g" }}") },
        confirmButton = { TextButton(onClick = { viewModel.commitOutbound(check.deductions, check.balancesAfter); outbound = ""; confirmOutbound = null; alertText = "支出成功。" }) { Text("确认") } },
        dismissButton = { TextButton(onClick = { confirmOutbound = null }) { Text("取消") } }
    ) }
    alertText?.let { text -> AlertDialog(onDismissRequest = { alertText = null }, title = { Text(if (text.contains("成功")) "操作完成" else "无法执行") }, text = { Text(text) }, confirmButton = { TextButton(onClick = { alertText = null }) { Text("确定") } }) }
    if (clearChoice) AlertDialog(
        onDismissRequest = { clearChoice = false }, title = { Text("清空历史记录") }, text = { Text("请选择要清空的记录类型。") },
        confirmButton = { Column { TextButton(onClick = { clearChoice = false; clearTarget = "in" }) { Text("清空入库记录") }; TextButton(onClick = { clearChoice = false; clearTarget = "out" }) { Text("清空支出记录") }; TextButton(onClick = { clearChoice = false; clearTarget = "all" }) { Text("清空全部记录", color = Color.Red) } } },
        dismissButton = { TextButton(onClick = { clearChoice = false }) { Text("取消") } }
    )
    clearTarget?.let { target -> AlertDialog(
        onDismissRequest = { clearTarget = null },
        title = { Text("确定清空${if (target == "in") "入库" else if (target == "out") "支出" else "全部"}记录吗？") },
        text = { Text("此操作不可恢复，不影响当前药材库存。") },
        confirmButton = { TextButton(onClick = { when (target) { "in" -> viewModel.clearInRecords(); "out" -> viewModel.clearOutRecords(); else -> viewModel.clearAllRecords() }; clearTarget = null }) { Text("清空", color = Color.Red) } },
        dismissButton = { TextButton(onClick = { clearTarget = null }) { Text("取消") } }
    ) }
}
