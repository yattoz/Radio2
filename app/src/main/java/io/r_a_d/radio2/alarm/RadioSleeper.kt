package io.r_a_d.radio2.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.AlarmManagerCompat
import androidx.preference.PreferenceManager
import io.r_a_d.radio2.*
import io.r_a_d.radio2.streamerNotificationService.BootBroadcastReceiver

class RadioSleeper {

    companion object {
        val instance by lazy {
            RadioSleeper()
        }
    }

    lateinit var alarmIntent: PendingIntent


    fun setSleep(c: Context, isForce: Boolean = false, forceDuration: Long? = null)
    {
        // don't do anything if the preference is set to FALSE, of course.
        if (!PreferenceManager.getDefaultSharedPreferences(c).getBoolean("isSleeping", false) && !isForce)
            return

        val minutesDuration: Long = forceDuration ?: Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(c).getString("sleepDuration", "1") ?: "1").toLong()

        val alarmManager = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmIntent = Intent(c, RadioService::class.java).let { intent ->
            intent.putExtra("action", Actions.KILL.name)
            PendingIntent.getService(c, 0, intent, 0)
        }

        if (minutesDuration > 0)
            AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + minutesDuration * 60 * 1000,  alarmIntent)
        Log.d(tag, "set sleep to $minutesDuration minutes")
    }


    fun cancelAlarm(c: Context)
    {
        if (::alarmIntent.isInitialized)
        {
            val alarmManager = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(alarmIntent)
            Log.d(tag, "cancelled sleep")
        }
    }
}