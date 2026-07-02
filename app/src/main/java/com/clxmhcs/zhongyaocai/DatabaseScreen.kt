package com.clxmhcs.zhongyaocai

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun DatabaseScreen(data: AppData, viewModel: MainViewModel, onRoute: (AppRoute) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var importConfirm by remember { mutableStateOf<List<Herb>?>(null) }
    var passwordSetup by remember { mutableStateOf(false) }
    var resetConfirm by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    val exportHerbs = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        if (uri != null) scope.launch { runCatching { XlsxCodec.write(context.contentResolver, uri, XlsxCodec.herbSheet(data.herbs)) }.onSuccess { message = "库存 Excel 已导出。库存不足药材已标红。" }.onFailure { message = "导出失败：${it.message}" } }
    }
    val importHerbs = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { runCatching { XlsxCodec.readHerbs(context.contentResolver, uri) }.onSuccess { importConfirm = it }.onFailure { message = "导入失败：${it.message ?: "无法读取 Excel"}" } }
    }
    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch { runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(viewModel.exportBackup().toByteArray()) } ?: error("无法写入文件") }.onSuccess { message = "数据库备份已导出。" }.onFailure { message = "备份失败：${it.message}" } }
    }
    val restoreBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { val text = runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("无法读取文件") }.getOrElse { message = "恢复失败：${it.message}"; return@launch }; viewModel.restoreBackup(text) { error -> message = error ?: "恢复成功，当前数据已替换。" } }
    }
    val exportProfiles = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch { runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(XlsxCodec.profileJson(data.herbProfiles).toByteArray()) } ?: error("无法写入文件") }.onSuccess { message = "药材资料 JSON 已导出。" }.onFailure { message = "导出失败：${it.message}" } }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("数据库管理", fontWeight = FontWeight.Bold) }
        item { RoundedActionButton("导出Excel（.xlsx，库存不足红色）", Color(0xFFA6DFFF), onClick = { exportHerbs.launch("Herbs_${System.currentTimeMillis()}.xlsx") }, modifier = Modifier.fillMaxWidth()) }
        item { RoundedActionButton("导入Excel（.xlsx，覆盖药材库存）", Color(0xFF99C7FF), onClick = { importHerbs.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) }
        item { RoundedActionButton("导出 / 备份数据库（JSON）", Color(0xFFBFE6D0), onClick = { exportBackup.launch("中药材库存_${System.currentTimeMillis()}.json") }, modifier = Modifier.fillMaxWidth()) }
        item { RoundedActionButton("恢复数据库（导入 JSON）", Color(0xFFFFCCCC), onClick = { restoreBackup.launch(arrayOf("application/json", "text/plain")) }, modifier = Modifier.fillMaxWidth()) }
        item { RoundedActionButton("药材余量重置", Color(0xFFE98282), onClick = { if (data.stockResetPasswordHash == null) passwordSetup = true else resetConfirm = true }, modifier = Modifier.fillMaxWidth()) }
        item { Spacer(Modifier.height(22.dp)) }
        item { RoundedActionButton("APP说明书", AppOrange, onClick = { onRoute(AppRoute.Manual) }, modifier = Modifier.fillMaxWidth()) }
        item { RoundedActionButton("导出药材资料（JSON）", Color(0xFF4BBFC0), onClick = { exportProfiles.launch("herb_records_${System.currentTimeMillis()}.json") }, modifier = Modifier.fillMaxWidth()) }
        item { RoundedActionButton("药材资料录入", Color(0xFFFBB85A), onClick = { onRoute(AppRoute.Profiles) }, modifier = Modifier.fillMaxWidth()) }
    }

    importConfirm?.let { imported -> AlertDialog(
        onDismissRequest = { importConfirm = null },
        title = { Text("确认导入") },
        text = { Text("已通过校验，共 ${imported.size} 味药材。确认后会覆盖现有药材库存、预警、日用量和价格；处方及出入库历史不会删除。") },
        confirmButton = { TextButton(onClick = { viewModel.overwriteHerbs(imported); importConfirm = null; message = "导入完成：已覆盖 ${imported.size} 味药材。" }) { Text("确认导入") } },
        dismissButton = { TextButton(onClick = { importConfirm = null }) { Text("取消") } }
    ) }
    if (passwordSetup) PasswordDialog("首次使用请设置余量重置密码", password, { password = it }, onDismiss = { passwordSetup = false; password = "" }) {
        viewModel.setResetPassword(password) { error ->
            if (error == null) { passwordSetup = false; password = ""; message = "密码已设置。请再次点击“药材余量重置”并输入密码后执行清空。" } else message = error
        }
    }
    if (resetConfirm) PasswordDialog("输入密码确认清零", password, { password = it }, onDismiss = { resetConfirm = false; password = "" }) {
        viewModel.resetStock(password) { result ->
            when (result) {
                ResetResult.Success -> { resetConfirm = false; password = ""; message = "所有药材余量已清零；药材名称、预警值、日用量、价格和其他数据未改变。" }
                ResetResult.WrongPassword -> message = "密码错误，未执行清空。"
                ResetResult.NeedsPasswordSetup -> { resetConfirm = false; passwordSetup = true }
            }
        }
    }
    message?.let { AlertDialog(onDismissRequest = { message = null }, title = { Text("数据库管理") }, text = { Text(it) }, confirmButton = { TextButton(onClick = { message = null }) { Text("确定") } }) }
}

@Composable
private fun PasswordDialog(title: String, value: String, change: (String) -> Unit, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { NumberField("密码", value) { change(it) } },
        confirmButton = { TextButton(onClick = onConfirm) { Text("确认") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
