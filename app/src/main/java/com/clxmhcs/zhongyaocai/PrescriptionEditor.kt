package com.clxmhcs.zhongyaocai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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

private val prescriptionLineRegex = Regex("^([\\u4e00-\\u9fa5A-Za-z_·-]+)\\s{0,2}(\\d{1,4}(?:\\.\\d{1,4})?)(?:\\s*(?:[gG]|克))?$")

@Composable
fun AddPrescriptionScreen(data: AppData, viewModel: MainViewModel, onDone: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var reimbursement by rememberSaveable { mutableStateOf("") }
    var important by rememberSaveable { mutableStateOf(false) }
    var blisterCode by rememberSaveable { mutableStateOf(0) }
    var issue by remember { mutableStateOf<String?>(null) }
    val preview = parsePrescriptionItems(text, data.herbs)
    val temporary = Prescription(items = preview.items, reimbursement = reimbursement.toDoubleOrNull() ?: 0.0)
    LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            PageCard {
                Text("保存处方", fontWeight = FontWeight.Bold)
                Text("每行填写“药材名 + 克数”，支持小数，例如：白术18、砂仁0.5。药材必须已录入库存。保存处方不会自动扣减库存。", color = Color.Gray)
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("处方药材") }, modifier = Modifier.fillMaxWidth().height(180.dp))
                if (preview.errors.isNotEmpty()) Text("以下药材不存在或格式错误：\n${preview.errors.joinToString("\n")}", color = Color.Red, modifier = Modifier.padding(top = 8.dp))
                if (preview.items.isNotEmpty()) {
                    Text("费用合计：¥${"%.2f".format(prescriptionCost(temporary, data))}", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                    preview.items.forEach { item -> Text("${item.herbName}  ${item.grams.gramText()}") }
                }
            }
        }
        item {
            PageCard {
                NumberField("报销：医保（元）", reimbursement, true) { reimbursement = it }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = important, onCheckedChange = { important = it }); Text("设为重要处方（重要处方不可直接删除）") }
                Text("水泡是否减轻", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = blisterCode == 0, onClick = { blisterCode = 0 }); Text("未记录") }
                Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = blisterCode == 1, onClick = { blisterCode = 1 }); Text("是") }
                Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = blisterCode == 2, onClick = { blisterCode = 2 }); Text("否") }
                Spacer(Modifier.height(8.dp))
                RoundedActionButton("保存处方", AppOrange, preview.items.isNotEmpty() && preview.errors.isEmpty(), onClick = {
                    val now = System.currentTimeMillis()
                    viewModel.savePrescription(Prescription(timestamp = now, dateString = dateLabel(now), items = preview.items, note = note.trim(), isImportant = important, blisterReduced = when (blisterCode) { 1 -> true; 2 -> false; else -> null }, reimbursement = reimbursement.toDoubleOrNull() ?: 0.0))
                    issue = "处方已保存。"
                }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    issue?.let { AlertDialog(onDismissRequest = { issue = null; onDone() }, title = { Text("保存处方") }, text = { Text(it) }, confirmButton = { TextButton(onClick = { issue = null; onDone() }) { Text("确定") } }) }
}

data class PrescriptionParse(val items: List<PrescriptionItem>, val errors: List<String>)

fun parsePrescriptionItems(text: String, herbs: List<Herb>): PrescriptionParse {
    val errors = mutableListOf<String>()
    val grouped = linkedMapOf<String, Double>()
    text.lines().map { InventoryLineParser.normalize(it) }.filter { it.isNotBlank() }.forEach { raw ->
        val match = prescriptionLineRegex.matchEntire(raw)
        val name = match?.groupValues?.getOrNull(1)
        val grams = match?.groupValues?.getOrNull(2)?.toDoubleOrNull()
        if (name == null || grams == null || grams <= 0 || herbs.none { it.name == name }) errors += raw
        else grouped[name] = (grouped[name] ?: 0.0) + grams
    }
    return PrescriptionParse(grouped.map { PrescriptionItem(it.key, it.value) }, errors)
}
