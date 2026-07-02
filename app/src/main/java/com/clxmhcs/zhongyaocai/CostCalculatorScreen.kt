package com.clxmhcs.zhongyaocai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

private data class CostFormulaItem(val name: String, val grams: Double, val gramsText: String)
private data class CostFormulaParse(val items: List<CostFormulaItem>, val invalidLines: List<String>)

/** Current iOS PrescriptionCostCalculatorView equivalent. */
@Composable
fun IOSPrescriptionCostCalculatorScreen(data: AppData, viewModel: MainViewModel) {
    val histories by viewModel.costHistory.collectAsState()
    var formula by rememberSaveable { mutableStateOf("") }
    var doses by rememberSaveable { mutableStateOf("7") }
    var insurance by rememberSaveable { mutableStateOf("60") }
    var secondPercent by rememberSaveable { mutableStateOf("30") }
    var secondCap by rememberSaveable { mutableStateOf("50") }
    var gap by rememberSaveable { mutableStateOf("0") }
    var lastResult by remember { mutableStateOf<CostCalculationHistory?>(null) }
    var alert by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<CostCalculationHistory?>(null) }
    var deleteTarget by remember { mutableStateOf<CostCalculationHistory?>(null) }
    val horizontal = rememberScrollState()

    fun calculate() {
        val parsed = parseCostFormula(formula)
        if (formula.trim().isEmpty()) { alert = "⚠️请逐行输入药方（每行：药名 克数，例如：白芍12，空格可有可无）"; return }
        val doseCount = doses.toIntOrNull()
        if (doseCount == null || doseCount <= 0) { alert = "⚠️剂数输入无效，请输入正整数。"; return }
        if (parsed.invalidLines.isNotEmpty()) {
            alert = "⚠️以下处方行格式错误，未进行计算：\n${parsed.invalidLines.take(8).joinToString("\n")}" + if (parsed.invalidLines.size > 8) "\n...（共 ${parsed.invalidLines.size} 行）" else "" + "\n\n格式示例：白芍12 或 白芍 12.5。"
            return
        }
        val missing = parsed.items.filter { item -> data.herbs.none { it.name == item.name } }.map { it.name }.distinct()
        if (missing.isNotEmpty()) { alert = "⚠️以下药材不存在，未进行计算：\n${missing.joinToString("、")}"; return }
        val ins = (insurance.toIntOrNull() ?: 0).coerceIn(0, 100)
        val second = (secondPercent.toIntOrNull() ?: 0).coerceIn(0, 100)
        val cap = max(0.0, secondCap.toDoubleOrNull() ?: 0.0)
        val deductible = max(0.0, gap.toDoubleOrNull() ?: 0.0)
        var perDose = 0.0
        val detailLines = mutableListOf<String>()
        parsed.items.forEach { item ->
            val unit = data.herbs.first { it.name == item.name }.pricePerKg
            val cost = item.grams / 1000.0 * unit
            perDose += cost
            detailLines += "${item.name} ${item.gramsText}g × ${formatMoney(unit)} 元/kg = ${formatMoney(cost)} 元/剂"
        }
        val total = roundMoney(perDose * doseCount)
        val eligible = max(0.0, total - deductible)
        val afterInsurance = roundMoney(eligible * (1.0 - ins / 100.0))
        val rawSecond = afterInsurance * second / 100.0
        val reimbursed = roundMoney(min(rawSecond, cap))
        val capHit = rawSecond > cap
        val finalPay = roundMoney(afterInsurance - reimbursed + deductible)
        val entry = CostCalculationHistory(
            formulaText = formula.trim(),
            doses = doseCount,
            insurancePercent = ins,
            secondPercent = second,
            secondCapYuan = cap,
            deductibleGapYuan = deductible,
            totalYuan = total,
            afterInsuranceYuan = afterInsurance,
            secondReimbursedYuan = reimbursed,
            secondCapTriggered = capHit,
            finalPayYuan = finalPay,
            detailText = detailLines.joinToString("\n")
        )
        lastResult = entry
        viewModel.saveCostHistory(entry)
    }

    LazyColumn(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("⚠️处方输入框（逐行输入：药名+克数，如：白芍12，空格可有可无。   ⚠️无需额外输入金额！！！）", fontSize = 13.sp, color = Color.Gray)
                OutlinedTextField(
                    value = formula,
                    onValueChange = { formula = it },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
                    trailingIcon = { if (formula.isNotEmpty()) Text("×", fontSize = 24.sp, color = Color.Gray, modifier = Modifier.clickable { formula = "" }) }
                )
                Row(Modifier.horizontalScroll(horizontal), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CostField("剂数", doses, 52) { doses = it }
                    CostField("医保比例\n(%)", insurance, 74) { insurance = it }
                    CostField("二次比例\n(%)", secondPercent, 74) { secondPercent = it }
                    CostField("二次封顶\n(元)", secondCap, 74, true) { secondCap = it }
                    CostField("起付线\n差额(元)", gap, 74, true) { gap = it }
                    Button(onClick = ::calculate, colors = ButtonDefaults.buttonColors(containerColor = AppPurple), modifier = Modifier.height(56.dp)) { Text("计算", fontWeight = FontWeight.SemiBold) }
                }
                lastResult?.let { result -> CostSummary(result) }
            }
        }
        item { CostHistoryHeader() }
        if (histories.isEmpty()) item { Text("暂无历史记录", color = Color.Gray, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) }
        items(histories, key = { it.id }) { record ->
            CostHistoryRow(record, onOpen = { detail = record }, onDelete = { deleteTarget = record })
        }
    }

    alert?.let { text -> AlertDialog(onDismissRequest = { alert = null }, title = { Text("提示") }, text = { Text(text) }, confirmButton = { Button(onClick = { alert = null }) { Text("确定") } }) }
    detail?.let { record -> CostHistoryDetailDialog(record) { detail = null } }
    deleteTarget?.let { record -> AlertDialog(
        onDismissRequest = { deleteTarget = null }, title = { Text("确认删除") }, text = { Text("确认删除这条历史计算记录吗？") },
        confirmButton = { Button(onClick = { viewModel.deleteCostHistory(record.id); deleteTarget = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("删除") } },
        dismissButton = { Button(onClick = { deleteTarget = null }) { Text("取消") } }
    ) }
}

@Composable
private fun CostField(title: String, value: String, width: Int, decimal: Boolean = false, change: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(width.dp)) {
        Text(title, fontSize = 12.sp, color = Color.Gray, maxLines = 2)
        OutlinedTextField(value = value, onValueChange = { input -> change(input.filter { it.isDigit() || (decimal && it == '.') }) }, modifier = Modifier.width(width.dp), singleLine = true)
    }
}

@Composable
private fun CostSummary(item: CostCalculationHistory) {
    Column(Modifier.fillMaxWidth().background(Color(0xFFF2F2F7), RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("总价：${formatMoney(item.totalYuan)} 元（${item.doses}剂）", fontSize = 15.sp)
        Text("医保后自付：${formatMoney(item.afterInsuranceYuan)} 元；二次报销：${formatMoney(item.secondReimbursedYuan)} 元${if (item.secondCapTriggered) "（已触发封顶）" else ""}", fontSize = 13.sp, color = Color.Gray)
        Text("最终自付：${formatMoney(item.finalPayYuan)} 元", fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CostHistoryHeader() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("时间", Modifier.weight(1f), color = Color.Gray, fontSize = 13.sp)
        Text("医保后\n自付(元)", Modifier.width(68.dp), color = Color.Gray, fontSize = 11.sp)
        Text("二次报销\n(元)", Modifier.width(68.dp), color = Color.Gray, fontSize = 11.sp)
        Text("最终自付\n(元)", Modifier.width(80.dp), color = Color.Gray, fontSize = 11.sp)
        Text("操作", Modifier.width(36.dp), color = Color.Gray, fontSize = 12.sp)
    }
    HorizontalDivider()
}

@Composable
private fun CostHistoryRow(record: CostCalculationHistory, onOpen: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFFF2F2F7), RoundedCornerShape(10.dp)).clickable(onClick = onOpen).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(fullDateLabel(record.createdAt), Modifier.weight(1f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Clip)
        Text(formatMoney(record.afterInsuranceYuan), Modifier.width(68.dp), fontSize = 13.sp)
        Text(formatMoney(record.secondReimbursedYuan), Modifier.width(68.dp), fontSize = 13.sp)
        Text(formatMoney(record.finalPayYuan), Modifier.width(80.dp), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Icon(Icons.Default.Delete, "删除", tint = Color.Red, modifier = Modifier.width(36.dp).clickable(onClick = onDelete))
    }
}

@Composable
private fun CostHistoryDetailDialog(record: CostCalculationHistory, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("处方总价计算详情") },
        text = { LazyColumn { item { Text(record.formulaText, fontFamily = FontFamily.Monospace); Text(record.detailText, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 10.dp)); Text("最终自付：${formatMoney(record.finalPayYuan)} 元", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp)) } } },
        confirmButton = { Button(onClick = onDismiss) { Text("关闭") } }
    )
}

private fun parseCostFormula(text: String): CostFormulaParse {
    val regex = Regex("^([\\u4e00-\\u9fa5A-Za-z_·-]+)\\s{0,2}(\\d{1,4}(?:\\.\\d{1,4})?)(?:\\s*(?:[gG]|克))?$")
    val invalid = mutableListOf<String>()
    val items = mutableListOf<CostFormulaItem>()
    text.lines().map(InventoryLineParser::normalize).filter { it.isNotBlank() }.forEach { raw ->
        val match = regex.matchEntire(raw)
        val name = match?.groupValues?.getOrNull(1)
        val gramsText = match?.groupValues?.getOrNull(2)
        val grams = gramsText?.toDoubleOrNull()
        if (name == null || grams == null || grams <= 0) invalid += raw else items += CostFormulaItem(name, grams, gramsText)
    }
    return CostFormulaParse(items, invalid)
}

private fun roundMoney(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0
private fun formatMoney(value: Double): String = "%.2f".format(value)
