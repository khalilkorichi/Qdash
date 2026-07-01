package com.example.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.FinTrackApp

class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_PAUSE = "com.example.action.DOWNLOAD_PAUSE"
        const val ACTION_RESUME = "com.example.action.DOWNLOAD_RESUME"
        const val ACTION_CANCEL = "com.example.action.DOWNLOAD_CANCEL"
        const val EXTRA_UPDATE_INFO = "extra_update_info"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("NotificationActionReceiver", "Received action: $action")
        
        val app = context.applicationContext as? FinTrackApp ?: return
        val repository = app.container.updateRepository
        
        val infoJson = intent.getStringExtra(EXTRA_UPDATE_INFO)
        if (infoJson.isNullOrBlank()) {
            Log.e("NotificationActionReceiver", "Missing UpdateInfo JSON in intent")
            return
        }

        val moshi = com.squareup.moshi.Moshi.Builder()
            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(UpdateInfo::class.java)
        val info = try {
            adapter.fromJson(infoJson)
        } catch (e: Exception) {
            Log.e("NotificationActionReceiver", "Failed to parse UpdateInfo", e)
            null
        }

        if (info == null) {
            return
        }

        when (action) {
            ACTION_PAUSE -> {
                repository.pauseDownload(info)
            }
            ACTION_RESUME -> {
                repository.startDownload(info)
            }
            ACTION_CANCEL -> {
                repository.cancelDownload(info)
            }
        }
    }
}
