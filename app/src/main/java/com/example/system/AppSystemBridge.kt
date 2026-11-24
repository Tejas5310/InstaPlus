package com.example.system

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.example.data.model.LauncherIconVariant

/**
 * Encapsulates OS-level operations that require Application Context so ViewModels
 * stay decoupled from Android Context.
 */
class AppSystemBridge(private val appContext: Context) {

    fun switchLauncherIcon(variant: LauncherIconVariant): Boolean {
        return LauncherIconManager.switchAppIcon(appContext, variant)
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun shareText(title: String, body: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(chooser) }
    }
}
