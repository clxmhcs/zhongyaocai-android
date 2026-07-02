package com.clxmhcs.zhongyaocai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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

/** Current iOS FastInOutView layout: two mini cards, detail entry cards, dual history columns. */
@Composable
fun QuickInOutScreen(
    data: AppData,
    viewModel: MainViewModel,
    onHistoryDetail: () -> Unit = {},
    onInboundDetail: () -> Unit = {}
) {
    var inboundText by rememberSaveable { mutableStateOf("") }
    var outboundText by rememberSaveable { mutableStateOf("") }
    var banner by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showWhichToClear by remember { mutableStateOf(false) }
    var clearTarget by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    InOutMiniCard(
                        title = "快速入库",
                        text = inboundText,
                        placeholder = "逐行输入\n药材名 数量(g)",
                        primaryTitle = "入库",
                        onTextChange = { inboundText = it },
                        onPrimary = {
                            val check = InventoryLineParser.validateInbound(inboundText, data.herbs)
                            when {
                                check.invalidOrMissing.isNotEmpty() -> errorText = "以下药材不存在或格式错误：\n${check.invalidOrMissing.joinToString("\n")}" 
                                check.items.isEmpty() -> errorText = "请输入有效的入库内容。"
                                else -> {
                                    viewModel.commitInbound(check.items)
                                    inboundText = ""
                                    banner = "药材已入库成功"
                                }
                            }
                        },
                        onClear = { inboundText = "" },
                        modifier = Modifier.weight(1f)
                    )
                    InOutMiniCard(
                        title = "快速支出",
                        text = outboundText,
                        placeholder = "逐行输入\n药材名 数量(g)",
                        primaryTitle = "支出",
                        onTextChange = { outboundText = it },
                        onPrimary = {
                            if (outboundText.isBlank()) {
                                errorText = "请输入支出内容。"
                            } else {
                                val check = InventoryLineParser.validateOutbound(outboundText, data.herbs, data.outboundBalances)
                                errorText = outboundErrorText(check)
                                if (errorText == null) {
                                    viewModel.commitOutbound(check.deductions, check.balancesAfter)
                                    outboundText = ""
                                    banner = if (check.deductions.isEmpty()) "本次支出已由「进一余额抵扣」，无需扣减库存" else "药材已支出成功"
                                }
                            }
                        },
                        onClear = { outboundText = "" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    DetailEntryCard("历史记录明细", Icons.Default.ListAlt, onHistoryDetail, Modifier.weight(1f))
                    DetailEntryCard("入库明细", Icons.Default.MoveToInbox, onInboundDetail, Modifier.weight(1f))
                }
            }
            item {
                PageCard(Modifier.fillMaxWidth()) {
                    Text("历史记录", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("入库历史", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showWhichToClear = true }, modifier = Modifier.weight(1f)) { Text("清空历史", color = Color.Red, fontWeight = FontWeight.Bold) }
                        Text("支出历史", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth().height(320.dp)) {
                        HistoryColumn(data.inRecords, Modifier.weight(1f))
                        Spacer(Modifier.width(1.dp).fillMaxHeight().background(Color(0xFFDDDDDD)))
                        OutHistoryColumn(data.outRecords, Modifier.weight(1f))
                    }
                }
            }
        }
        banner?.let { text ->
            Row(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                    .background(Color(0xFF34A853), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                Text(text, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
    errorText?.let { message -> AlertDialog(onDismissRequest = { errorText = null }, title = { Text("操作提示") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { errorText = null }) { Text("确定") } }) }
    if (showWhichToClear) AlertDialog(
        onDismissRequest = { showWhichToClear = false },
        title = { Text("❓清空哪个记录？") },
        text = { Text("请选择需要清空的记录。") },
        confirmButton = {
            Column {
                TextButton(onClick = { showWhichToClear = false; clearTarget = "inbound" }) { Text("入库记录") }
                TextButton(onClick = { showWhichToClear = false; clearTarget = "outbound" }) { Text("支出记录") }
                TextButton(onClick = { showWhichToClear = false; clearTarget = "all" }) { Text("清空全部", color = Color.Red) }
            }
        },
        dismissButton = { TextButton(onClick = { showWhichToClear = false }) { Text("取消") } }
    )
    clearTarget?.let { target ->
        val targetName = when (target) { "inbound" -> "入库记录"; "outbound" -> "支出记录"; else -> "全部记录" }
        AlertDialog(
            onDismissRequest = { clearTarget = null },
            title = { Text("确定清空 $targetName 吗？") },
            text = { Text("⚠️该操作将删除选定记录，且无法恢复。") },
            confirmButton = { TextButton(onClick = {
                when (target) { "inbound" -> viewModel.clearInRecords(); "outbound" -> viewModel.clearOutRecords(); else -> viewModel.clearAllRecords() }
                clearTarget = null
            }) { Text("清空", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { clearTarget = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun InOutMiniCard(title: String, text: String, placeholder: String, primaryTitle: String, onTextChange: (String) -> Unit, onPrimary: () -> Unit, onClear: () -> Unit, modifier: Modifier) {
    PageCard(modifier) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = text, onValueChange = onTextChange, placeholder = { Text(placeholder) }, modifier = Modifier.fillMaxWidth().height(185.dp), minLines = 7)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onPrimary, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = AppPurple)) { Text(primaryTitle) }
            OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text("清空", color = Color.Red) }
        }
    }
}

@Composable
private fun DetailEntryCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier) {
    PageCard(modifier.clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, title, tint = AppPurple)
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowForwardIos, null, tint = Color.Gray)
        }
    }
}

@Composable
private fun HistoryColumn(records: List<InRecord>, modifier: Modifier) {
    Column(modifier) {
        HistoryHeader(); HorizontalDivider()
        if (records.isEmpty()) Text("暂无入库记录", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
        else LazyColumn { items(records, key = { it.id }) { record -> HistoryRow(record.name, record.amount); HorizontalDivider() } }
    }
}

@Composable
private fun OutHistoryColumn(records: List<OutRecord>, modifier: Modifier) {
    Column(modifier.padding(start = 8.dp)) {
        HistoryHeader(); HorizontalDivider()
        if (records.isEmpty()) Text("暂无支出记录", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
        else LazyColumn { items(records, key = { it.id }) { record -> HistoryRow(record.name, record.amount); HorizontalDivider() } }
    }
}

@Composable
private fun HistoryHeader() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
        Text("药材名", Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("克数", fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HistoryRow(name: String, amount: Int) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
        Text(name, Modifier.weight(1f)); Text(amount.toString())
    }
}

private fun outboundErrorText(check: OutboundValidation): String? = when {
    check.invalidOrMissing.isNotEmpty() && check.insufficient.isNotEmpty() -> "以下药材不存在或格式错误：\n${check.invalidOrMissing.joinToString("\n")}\n\n余量不足：\n${check.insufficient.joinToString("\n") { "${it.first}（当前余量：${it.second}g）" }}"
    check.invalidOrMissing.isNotEmpty() -> "以下药材不存在或格式错误：\n${check.invalidOrMissing.joinToString("\n")}" 
    check.insufficient.isNotEmpty() -> "以下药材余量不足：\n${check.insufficient.joinToString("\n") { "${it.first}（当前余量：${it.second}g）" }}"
    else -> null
}
