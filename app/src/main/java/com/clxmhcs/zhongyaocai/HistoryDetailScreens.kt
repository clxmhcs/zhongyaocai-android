package com.clxmhcs.zhongyaocai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HistoryDetailScreen(data: AppData) {
    val rows = buildList {
        data.inRecords.forEach { add(HistoryDetailRow(it.createdAt, "入库", it.name, it.amount)) }
        data.outRecords.forEach { add(HistoryDetailRow(it.createdAt, "支出", it.name, it.amount)) }
    }.sortedByDescending { it.createdAt }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("时间", Modifier.weight(1.4f), fontWeight = FontWeight.Bold)
                Text("类型", Modifier.weight(.7f), fontWeight = FontWeight.Bold)
                Text("药材名", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("克数", fontWeight = FontWeight.Bold)
            }
            HorizontalDivider()
        }
        if (rows.isEmpty()) item { EmptyHint("暂无历史记录") }
        items(rows, key = { "${it.kind}-${it.createdAt}-${it.name}-${it.amount}" }) { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
                Text(fullDateLabel(row.createdAt), Modifier.weight(1.4f))
                Text(row.kind, Modifier.weight(.7f))
                Text(row.name, Modifier.weight(1f))
                Text("${row.amount}g")
            }
            HorizontalDivider()
        }
    }
}

@Composable
fun InboundHistoryScreen(data: AppData) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("时间", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("药材名", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("克数", fontWeight = FontWeight.Bold)
            }
            HorizontalDivider()
        }
        if (data.inRecords.isEmpty()) item { EmptyHint("暂无入库记录") }
        items(data.inRecords, key = { it.id }) { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
                Text(fullDateLabel(row.createdAt), Modifier.weight(1f))
                Text(row.name, Modifier.weight(1f))
                Text("${row.amount}g")
            }
            HorizontalDivider()
        }
    }
}

private data class HistoryDetailRow(val createdAt: Long, val kind: String, val name: String, val amount: Int)
