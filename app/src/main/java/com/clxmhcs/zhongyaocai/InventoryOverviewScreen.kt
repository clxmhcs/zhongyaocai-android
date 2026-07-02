package com.clxmhcs.zhongyaocai

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** iOS InventoryOverviewWarningView equivalent. */
@Composable
fun InventoryOverviewScreen(data: AppData) {
    val herbs = data.herbs.sortedWith(compareBy<Herb> { it.pinyin }.thenBy { it.name })
    val tableScroll = rememberScrollState()
    Column(Modifier.fillMaxSize().padding(top = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("药材余量总览", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {}) { Text("刷新", color = Color(0xFF007AFF)) }
        }
        Column(Modifier.horizontalScroll(tableScroll)) {
            InventoryTableHeader()
            HorizontalDivider(color = Color(0xFFD9D9D9))
            if (herbs.isEmpty()) {
                Column(Modifier.width(420.dp).padding(top = 74.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无药材数据", color = Color.Gray)
                    Text("可在“新增药材”页面添加，或稍后自动完成初始化。", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                }
            } else {
                LazyColumn(Modifier.width(420.dp).fillMaxSize()) {
                    items(herbs, key = { it.id }) { herb -> InventoryTableRow(herb) }
                }
            }
        }
    }
}

@Composable
private fun InventoryTableHeader() {
    Row(Modifier.width(420.dp).padding(horizontal = 16.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("药材名", Modifier.width(72.dp), fontSize = 14.sp, color = Color.Gray, maxLines = 1)
        Text("库存", Modifier.width(70.dp), fontSize = 14.sp, color = Color.Gray, maxLines = 1)
        Spacer(Modifier.width(48.dp))
        Text("日(周)用量", Modifier.width(120.dp), fontSize = 14.sp, color = Color.Gray, maxLines = 1)
        Text("价格", Modifier.width(46.dp), fontSize = 14.sp, color = Color.Gray, maxLines = 1)
        Text("(元/kg)", Modifier.width(48.dp), fontSize = 12.sp, color = Color.Gray, maxLines = 1)
    }
}

@Composable
private fun InventoryTableRow(herb: Herb) {
    val isLow = herb.stock < herb.warningLevel
    val mainColor = if (isLow) Color.Red else Color(0xFF111111)
    Row(Modifier.width(420.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(herb.name, Modifier.width(72.dp), color = mainColor, maxLines = 1, overflow = TextOverflow.Clip)
        Row(Modifier.width(70.dp), horizontalArrangement = Arrangement.End) {
            Text("${herb.stock}", color = mainColor)
            Text(" g", color = Color.Black)
        }
        Spacer(Modifier.width(48.dp))
        Text("${herb.dailyUsage} g (周 ${herb.dailyUsage * 7} g)", Modifier.width(120.dp), color = Color.Gray, maxLines = 1, overflow = TextOverflow.Clip)
        Text("${"%.2f".format(herb.pricePerKg)}", Modifier.width(94.dp), maxLines = 1)
    }
}
