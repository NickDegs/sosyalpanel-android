package com.nickdegs.sosyalpanel.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

// iOS ComposerService.openApp karşılığı — uygulama yüklüyse onu, değilse web'i açar.
object AppOpener {
    fun open(context: Context, platform: Platform) {
        // Önce native uygulamayı paketinden başlatmayı dene
        val pm = context.packageManager
        val launch = pm.getLaunchIntentForPackage(platform.packageName)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launch)
            return
        }
        // Yoksa web profili tarayıcıda aç
        runCatching {
            val web = Intent(Intent.ACTION_VIEW, Uri.parse(platform.webBase)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(web)
        }.onFailure {
            Toast.makeText(context, platform.displayName, Toast.LENGTH_SHORT).show()
        }
    }
}
