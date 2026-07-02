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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** Current iOS DatabaseView: pale-green background, solid 30pt-radius full width actions. */
@Composable
fun DatabaseScreen(data: AppData, viewModel: MainViewModel, onRoute: (AppRoute) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var importConfirm by remember { mutableStateOf<List<Herb>?>(null) }
    var passwordSetup by remember { mutableStateOf(false) }
    var passwordVerify by remember { mutableStateOf(false) }
    var finalResetConfirm by remember { mutableStateOf(false) }
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
        if (uri != null) scope.launch {
            val text = runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("无法读取文件") }.getOrElse { message = "恢复失败：${it.message}"; return@launch }
            viewModel.restoreBackup(text) { error -> message = error ?: "恢复成功，当前数据已替换。" }
        }
    }
    val exportProfiles = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch { runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(XlsxCodec.profileJson(data.herbProfiles).toByteArray()) } ?: error("无法写入文件") }.onSuccess { message = "药材资料 JSON 已导出。" }.onFailure { message = "导出失败：${it.message}" } }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Text("数据库管理", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth()) }
        item { IOSDatabaseButton("导出Excel（.xlsx，库存不足红色）", Color(0xFFA6E0FF)) { exportHerbs.launch("Herbs_${System.currentTimeMillis()}.xlsx") } }
        item { IOSDatabaseButton("导入Excel（.xlsx，写入数据库）", Color(0xFF99C7FF)) { importHerbs.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/octet-stream")) } }
        item { IOSDatabaseButton("导出 / 备份数据库（JSON）", Color(0xFFBFE6D9)) { exportBackup.launch("中药材库存_${System.currentTimeMillis()}.json") } }
        item { IOSDatabaseButton("恢复数据库（导入 JSON）", Color(0xFFFFCCCC)) { restoreBackup.launch(arrayOf("application/json", "text/plain")) } }
        item { IOSDatabaseButton("药材余量重置", Color(0xFFF28C8C)) { if (data.stockResetPasswordHash == null) passwordSetup = true else passwordVerify = true } }
        item { Spacer(Modifier.height(18.dp)) }
        item { IOSDatabaseButton("APP说明书", Color(0xFFFF9E40)) { onRoute(AppRoute.Manual) } }
        item { IOSDatabaseButton("导出药材资料（JSON）", Color(0xFF4DC7C7)) { exportProfiles.launch("herb_records_${System.currentTimeMillis()}.json") } }
        item { Spacer(Modifier.height(18.dp)) }
        item { IOSDatabaseButton("药材资料录入", Color(0xFFFAB85A)) { onRoute(AppRoute.Profiles) } }
    }

    importConfirm?.let { imported -> AlertDialog(
        onDismissRequest = { importConfirm = null },
        title = { Text("确认导入") },
        text = { Text("确认导入该文件的内容吗？\n\n此操作会覆盖现有药材数据且无法回退！！！") },
        confirmButton = { TextButton(onClick = { viewModel.overwriteHerbs(imported); importConfirm = null; message = "导入完成：新增 ${imported.size} 条，更新 0 条，跳过 0 条" }) { Text("确认导入", color = Color.Red) } },
        dismissButton = { TextButton(onClick = { importConfirm = null }) { Text("取消") } }
    ) }
    if (passwordSetup) PasswordDialog(
        title = "首次使用需设置密码",
        message = "首次点击“药材余量重置”必须先设置密码。本次只保存密码，不会清空库存。设置完成后，请重新点击按钮执行清空操作。",
        value = password,
        change = { password = it },
        confirmTitle = "保存密码",
        onDismiss = { passwordSetup = false; password = "" },
        onConfirm = {
            viewModel.setResetPassword(password) { error ->
                if (error == null) { passwordSetup = false; password = ""; message = "密码已设置。本次不会清空库存，请重新点击“药材余量重置”后输入密码执行清空。" } else message = error
            }
        }
    )
    if (passwordVerify) PasswordDialog(
        title = "输入密码确认",
        message = "此操作会将所有药材库存余量清零。请输入已设置的密码进行校验。",
        value = password,
        change = { password = it },
        confirmTitle = "下一步",
        onDismiss = { passwordVerify = false; password = "" },
        onConfirm = {
            viewModel.verifyResetPassword(password) { result ->
                when (result) {
                    ResetResult.Success -> { passwordVerify = false; finalResetConfirm = true }
                    ResetResult.WrongPassword -> { password = ""; message = "密码错误，未执行清空操作。" }
                    ResetResult.NeedsPasswordSetup -> { passwordVerify = false; passwordSetup = true }
                }
            }
        }
    )
    if (finalResetConfirm) AlertDialog(
        onDismissRequest = { finalResetConfirm = false; password = "" },
        title = { Text("二次确认") },
        text = { Text("确认后，所有药材库存余量都会变为 0，且无法自动回退。是否继续？") },
        confirmButton = { TextButton(onClick = {
            viewModel.resetStock(password) { result ->
                finalResetConfirm = false
                password = ""
                message = when (result) {
                    ResetResult.Success -> "已将全部药材库存余量清零，共处理 ${data.herbs.size} 条。"
                    ResetResult.WrongPassword -> "密码错误，未执行清空操作。"
                    ResetResult.NeedsPasswordSetup -> "尚未设置密码。"
                }
            }
        }) { Text("确认清零", color = Color.Red) } },
        dismissButton = { TextButton(onClick = { finalResetConfirm = false; password = "" }) { Text("取消") } }
    )
    message?.let { AlertDialog(onDismissRequest = { message = null }, title = { Text("数据库管理") }, text = { Text(it) }, confirmButton = { TextButton(onClick = { message = null }) { Text("确定") } }) }
}

@Composable
private fun IOSDatabaseButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(30.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
    ) { Text(text) }
}

@Composable
private fun PasswordDialog(title: String, message: String, value: String, change: (String) -> Unit, confirmTitle: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column { Text(message); OutlinedTextField(value, change, label = { Text("请输入重置密码") }, modifier = Modifier.fillMaxWidth()) } },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmTitle) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
