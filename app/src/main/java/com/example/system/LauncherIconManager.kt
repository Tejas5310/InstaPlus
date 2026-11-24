package com.example.system

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.example.data.model.LauncherIconVariant

private const val TAG = "LauncherIconManager"

object LauncherIconManager {

    fun switchAppIcon(context: Context, variant: LauncherIconVariant): Boolean {
        return runCatching {
            val pm = context.packageManager
            val packageName = context.packageName
            LauncherIconVariant.entries.forEach { entry ->
                val state = if (entry == variant) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
                pm.setComponentEnabledSetting(
                    ComponentName(packageName, entry.aliasClassName),
                    state,
                    PackageManager.DONT_KILL_APP
                )
            }
            true
        }.onFailure { err ->
            Log.e(TAG, "Failed switching launcher icon alias", err)
        }.getOrDefault(false)
    }
}
