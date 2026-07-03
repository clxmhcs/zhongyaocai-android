package com.clxmhcs.zhongyaocai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WebHostBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val prefs = context.getSharedPreferences(WebHostService.PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(WebHostService.KEY_ENABLED, false)) {
            WebHostService.start(context)
        }
    }
}
