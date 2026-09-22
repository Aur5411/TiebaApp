package com.huanchengfly.tieba.post.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences

class AutoSignAlarm : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 安全模式下不执行自动签到（签到为写操作，已被网络层拦截）
        if (context.appPreferences.safeMode) return
        runCatching {
            TiebaUtil.startSign(context)
        }
    }

    companion object {
        val TAG: String = "AutoSignAlarm"
    }
}