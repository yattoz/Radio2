package io.r_a_d.radio2.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import androidx.core.app.AlarmManagerCompat
import io.r_a_d.radio2.Actions
import io.r_a_d.radio2.MainActivity
import io.r_a_d.radio2.ParametersActivity
import io.r_a_d.radio2.RadioService
import io.r_a_d.radio2.streamerNotificationService.BootBroadcastReceiver
import io.r_a_d.radio2.streamerNotificationService.StreamerMonitorService

class RadioAlarm {

    companion object {
        val instance by lazy {
            RadioAlarm()
        }
    }
    lateinit var alarmIntent: PendingIntent


    fun cancelAlarm(c: Context)
    {
        if (::alarmIntent.isInitialized)
        {
            val alarmManager = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(alarmIntent)
        }
    }

    fun setAlarm(c: Context)
    {
        val alarmManager = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmIntent = Intent(c, RadioService::class.java).let { intent ->
            intent.putExtra("action", Actions.PLAY_OR_FALLBACK.name)
            PendingIntent.getService(c, 0, intent, 0)
        }
        val showIntent = Intent(c, ParametersActivity::class.java).let { intent ->
            PendingIntent.getActivity(c, 0, intent, 0)
        }
        val time = System.currentTimeMillis() + (30*1000)
        AlarmManagerCompat.setAlarmClock(alarmManager, time, showIntent, alarmIntent)
    }
}