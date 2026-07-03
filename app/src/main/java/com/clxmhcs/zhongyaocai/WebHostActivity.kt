package com.clxmhcs.zhongyaocai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class WebHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs = getSharedPreferences(WebHostService.PREFS, Context.MODE_PRIVATE)
            var script by remember { mutableStateOf(prefs.getString(WebHostService.KEY_SCRIPT, "/data/data/com.termux/files/home/zhongyaocai-web-server/scripts/termux-start-all.sh") ?: "") }
            var url by remember { mutableStateOf(prefs.getString(WebHostService.KEY_URL, "http://127.0.0.1:8787/api/session") ?: "") }
            var interval by remember { mutableIntStateOf(prefs.getInt(WebHostService.KEY_INTERVAL, 60)) }
            var enabled by remember { mutableStateOf(prefs.getBoolean(WebHostService.KEY_ENABLED, false)) }
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("中药材网站宿主", style = MaterialTheme.typography.headlineSmall)
                        Text("原生前台服务负责开机后拉起、持续监测并在本地网站无响应时请求 Termux 启动脚本。网页业务服务仍由 zhongyaocai-web-server 运行。")
                        OutlinedTextField(script, { script = it }, Modifier.fillMaxWidth(), label = { Text("Termux 启动脚本路径") })
                        OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("本地健康检查地址") })
                        OutlinedTextField(interval.toString(), { interval = it.toIntOrNull()?.coerceIn(30, 900) ?: interval }, Modifier.fillMaxWidth(), label = { Text("检查间隔（秒，30-900）") })
                        Button(onClick = {
                            prefs.edit().putString(WebHostService.KEY_SCRIPT, script.trim()).putString(WebHostService.KEY_URL, url.trim()).putInt(WebHostService.KEY_INTERVAL, interval).apply()
                            enabled = true
                            WebHostService.start(this@WebHostActivity)
                        }, modifier = Modifier.fillMaxWidth()) { Text("保存并启动宿主服务") }
                        Button(onClick = {
                            enabled = false
                            WebHostService.stop(this@WebHostActivity)
                        }, modifier = Modifier.fillMaxWidth()) { Text("停止宿主服务") }
                        Text("当前状态：${if (enabled) "已启用。请在通知栏查看运行状态。" else "未启用。"}")
                        TextButton(onClick = { requestBatteryOptimizationExemption() }) { Text("请求忽略电池优化") }
                        Text("使用前需要安装 Termux 与 Termux:API，并在 Termux:API 中允许外部应用执行命令。原生宿主不会直接执行 Node.js，而是通过 Termux 启动已配置的网站脚本。")
                        Spacer(Modifier.height(8.dp))
                        Text("健康检查默认地址：127.0.0.1:8787/api/session")
                    }
                }
            }
        }
    }

    private fun requestBatteryOptimizationExemption() {
        val powerManager = getSystemService(PowerManager::class.java)
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        }
    }
}
