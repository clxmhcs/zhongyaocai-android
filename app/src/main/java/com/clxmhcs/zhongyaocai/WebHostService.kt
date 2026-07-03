package com.clxmhcs.zhongyaocai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class WebHostService : Service() {
    companion object {
        const val ACTION_START = "com.clxmhcs.zhongyaocai.webhost.START"
        const val ACTION_STOP = "com.clxmhcs.zhongyaocai.webhost.STOP"
        const val PREFS = "web_host_prefs"
        const val KEY_ENABLED = "enabled"
        const val KEY_SCRIPT = "script"
        const val KEY_URL = "url"
        const val KEY_INTERVAL = "interval"
        const val CHANNEL_ID = "zhongyaocai_web_host"
        const val NOTIFICATION_ID = 8787

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, WebHostService::class.java).setAction(ACTION_START))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, WebHostService::class.java).setAction(ACTION_STOP))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            preferences().edit().putBoolean(KEY_ENABLED, false).apply()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        preferences().edit().putBoolean(KEY_ENABLED, true).apply()
        createChannel()
        startForeground(NOTIFICATION_ID, notification("正在检查本地网站服务…"))
        if (monitorJob?.isActive != true) monitorJob = scope.launch { monitorLoop() }
        return START_STICKY
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun monitorLoop() {
        while (scope.isActive) {
            val ok = checkServer()
            if (!ok) launchTermuxServer()
            updateNotification(if (ok) "网站服务正常运行" else "网站未响应，已请求 Termux 启动服务")
            val seconds = preferences().getInt(KEY_INTERVAL, 60).coerceIn(30, 900)
            delay(seconds * 1000L)
        }
    }

    private fun checkServer(): Boolean = try {
        val url = preferences().getString(KEY_URL, "http://127.0.0.1:8787/api/session")!!
        (URL(url).openConnection() as HttpURLConnection).run {
            connectTimeout = 6_000
            readTimeout = 6_000
            requestMethod = "GET"
            instanceFollowRedirects = false
            connect()
            responseCode in 200..499
        }
    } catch (_: Exception) {
        false
    }

    private fun launchTermuxServer() {
        val script = preferences().getString(
            KEY_SCRIPT,
            "/data/data/com.termux/files/home/zhongyaocai-web-server/scripts/termux-start-all.sh"
        )!!
        val workDir = script.substringBeforeLast("/scripts/", script.substringBeforeLast('/'))
        try {
            val command = Intent("com.termux.RUN_COMMAND").apply {
                setPackage("com.termux")
                putExtra("com.termux.RUN_COMMAND_PATH", script)
                putExtra("com.termux.RUN_COMMAND_WORKDIR", workDir)
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            }
            sendBroadcast(command)
        } catch (_: Exception) {
            updateNotification("无法调用 Termux。请安装 Termux:API 并允许外部命令。")
        }
    }

    private fun preferences() = getSharedPreferences(PREFS, MODE_PRIVATE)

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "中药材网站宿主", NotificationManager.IMPORTANCE_LOW).apply {
                description = "保持中药材网站服务的后台监测与自动拉起"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notification(text: String): Notification {
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, WebHostService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = PendingIntent.getActivity(
            this, 2, Intent(this, WebHostActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle("中药材网站宿主")
            .setContentText(text)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", stopIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text))
    }
}
