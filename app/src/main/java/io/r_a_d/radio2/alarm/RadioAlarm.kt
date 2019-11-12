package io.r_a_d.radio2.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.AlarmManagerCompat
import io.r_a_d.radio2.streamerNotificationService.BootBroadcastReceiver
import androidx.preference.PreferenceManager
import io.r_a_d.radio2.*
import java.util.*
import kotlin.collections.ArrayList


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

    fun setNextAlarm(c: Context, isForce: Boolean = false, forceTime: Int? = null, forceDays: Set<String>? = null)
    {
        // don't do anything if the preference is set to FALSE, of course.
        if (!PreferenceManager.getDefaultSharedPreferences(c).getBoolean("isWakingUp", false) && !isForce)
            return

        val alarmManager = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmIntent = Intent(c, BootBroadcastReceiver::class.java).let { intent ->
            intent.putExtra("action", "io.r_a_d.radio2.${Actions.PLAY_OR_FALLBACK.name}")
            PendingIntent.getBroadcast(c, 0, intent, 0)
        }
        val showIntent = Intent(c, ParametersActivity::class.java).let { intent ->
            PendingIntent.getActivity(c, 0, intent, 0)
        }
        val time = findNextAlarmTime(c, forceTime, forceDays)
        if (time > 0)
            AlarmManagerCompat.setAlarmClock(alarmManager, time, showIntent, alarmIntent)
    }

    private fun findNextAlarmTime(c: Context, forceTime: Int? = null, forceDays: Set<String>? = null) : Long
    {
        val calendar = Calendar.getInstance()

        val days = forceDays ?: PreferenceManager.getDefaultSharedPreferences(c).getStringSet("alarmDays", setOf())
        val time = forceTime ?: PreferenceManager.getDefaultSharedPreferences(c).getInt("alarmTimeFromMidnight", 7*60)

        val hourOfDay = time / 60
        val minute = time % 60

        val fullWeekOrdered = arrayListOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val selectedDays = arrayListOf<Int>()
        for (item in fullWeekOrdered)
        {
            if (days!!.contains(item))
                selectedDays.add(fullWeekOrdered.indexOf(item)+1)
        }

        if (selectedDays.isEmpty()) // in case the user uncheck all boxes... do nothing.
            return 0


        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        val datePassed = if (calendar.get(Calendar.HOUR_OF_DAY)*60 + calendar.get(Calendar.MINUTE) >= time ) 1 else 0
        var nextSelectedDay = currentDay + datePassed
        var i = 0 + datePassed
        while (!selectedDays.contains(nextSelectedDay))
        {
            nextSelectedDay = (nextSelectedDay)%7 + 1
            i++
        }
        // We found out the next selected day in the list.
        // we must move 'i' days forward
        calendar.isLenient = true
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH) + i, hourOfDay, minute)

        Log.d(tag, calendar.toString())


        return calendar.timeInMillis
    }
}