package com.clxmhcs.zhongyaocai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Current iOS PrescriptionDetailView visual hierarchy. */
@Composable
fun PrescriptionDetailScreen(prescription: Prescription, data: AppData, viewModel: MainViewModel) {
    var copySuccess by remember { mutableStateOf(false) }
    var important by remember { mutableStateOf(prescription.isImportant) }
    val context = LocalContext.current
    val rawContent = remember(prescription.items) { prescription.items.joinToString("\n") { "${it.herbName}${it.grams.gramText()}" } }
    fun copyContent() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("处方", rawContent))
        copySuccess = true
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("日期：${prescription.dateString}", fontWeight = FontWeight.Bold)
                    Text("（共${prescription.items.size}味）", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(start = 6.dp))
                    if (important) Icon(Icons.Default.Warning, "重要处方", tint = AppOrange, modifier = Modifier.padding(start = 6.dp))
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = ::copyContent) { Icon(Icons.Default.ContentCopy, "复制"); Text("复制", fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp)) }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().background(Color(0xFFF2F2F7), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row { Text("处方药材", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray); Text("（点击蓝色药材名称可查看药材详细资料）", fontSize = 12.sp, color = Color.Gray) }
                    prescription.items.chunked(2).forEach { pair -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { PrescriptionDetailLine(pair[0], Modifier.weight(1f)); if (pair.size > 1) PrescriptionDetailLine(pair[1], Modifier.weight(1f)) else Spacer(Modifier.weight(1f)) } }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("原始处方（共${prescription.items.size}味）。", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text("「重要处方：${if (important) "是" else "否"} 」", color = if (important) Color.Red else Color.Gray, fontSize = 14.sp)
                        Switch(checked = important, onCheckedChange = { checked -> important = checked; viewModel.savePrescription(prescription.copy(isImportant = checked)) }, modifier = Modifier.padding(start = 6.dp))
                    }
                    Text(rawContent, fontFamily = FontFamily.Monospace, modifier = Modifier.fillMaxWidth().background(Color(0xFFF2F2F7), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(16.dp))
                }
            }
            item {
                Button(onClick = ::copyContent, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)), shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)) { Icon(Icons.Default.ContentCopy, "一键复制"); Text("一键复制", modifier = Modifier.padding(start = 8.dp)) }
            }
        }
        if (copySuccess) {
            Row(modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp).background(Color(0xFF34C759), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                Text("复制成功", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun PrescriptionDetailLine(item: PrescriptionItem, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(item.herbName, color = Color(0xFF007AFF), fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Text(item.grams.gramText(), color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
    }
}
