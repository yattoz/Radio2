package io.r_a_d.radio2.streamerNotificationService

import android.content.BroadcastReceiver
import android.content.Context
import android.os.Build
import android.content.Intent
import android.util.Log
import androidx.preference.PreferenceManager
import io.r_a_d.radio2.Actions
import io.r_a_d.radio2.preferenceStore
import io.r_a_d.radio2.tag


class BootBroadcastReceiver : BroadcastReceiver(){

    override fun onReceive(context: Context, arg1: Intent) {
        startStreamerMonitor(context)
    }

}

fun stopStreamerMonitor(context: Context)
{
    val intent = Intent(context, StreamerMonitorService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.stopService(intent)
    } else {
        context.stopService(intent)
    }
    Log.i(tag, "Service stopped")
}

fun startStreamerMonitor(context: Context, force: Boolean = false)
{
    if (!force)
    {
        val isNotifyingForNewStreamer = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("newStreamerNotification", false)
        if (!isNotifyingForNewStreamer)
            return
    }

    // at boot, reset streamerName in the preferenceStore.
    // Setting the previous streamerName to "" allows the the service to initialize it silently on first fetch.
    with(PreferenceManager.getDefaultSharedPreferences(context).edit()){
        putString("streamerName", "")
        commit() // I commit on main thread to be sure it's been updated before continuing.
    }
    val intent = Intent(context, StreamerMonitorService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
    Log.i(tag, "Service started on boot")
}