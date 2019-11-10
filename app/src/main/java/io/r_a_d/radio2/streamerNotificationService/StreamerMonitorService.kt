package io.r_a_d.radio2.streamerNotificationService


import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import androidx.work.*
import io.r_a_d.radio2.Actions
import io.r_a_d.radio2.R
import io.r_a_d.radio2.tag
import java.util.Timer
import java.util.concurrent.TimeUnit

class StreamerMonitorService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        return null     // no binding allowed nor needed
    }
    private val streamerNameObserver: Observer<String> = Observer {
        val previousStreamer: String
        if (PreferenceManager.getDefaultSharedPreferences(this).contains("streamerName"))
        {
            previousStreamer = PreferenceManager.getDefaultSharedPreferences(this).getString("streamerName", "") ?: ""
            if (previousStreamer != it && previousStreamer != "")
            {
                // notify
                val newStreamer = StreamerNotification(
                    notificationChannelId = this.getString(R.string.streamerNotificationChannelId),
                    notificationChannel = R.string.streamerNotificationChannel,
                    notificationId = 3,
                    notificationImportance = NotificationCompat.PRIORITY_DEFAULT
                )
                newStreamer.create(this)
                newStreamer.show()
            }
        }

        with(PreferenceManager.getDefaultSharedPreferences(this).edit()){
            putString("streamerName", it)
            apply()
        }
    }

    override fun onCreate() {
        super.onCreate()
        val streamerMonitorNotification = ServiceNotification(
            notificationChannelId = this.getString(R.string.streamerServiceChannelId),
            notificationChannel = R.string.streamerServiceChannel,
            notificationId = 2,
            notificationImportance = NotificationCompat.PRIORITY_LOW
        )
        streamerMonitorNotification.create(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            streamerMonitorNotification.update()
            streamerMonitorNotification.show()
            startForeground(2, streamerMonitorNotification.notification)
        }

        WorkerStore.instance.tickerPeriod = 60 *
                (if (PreferenceManager.getDefaultSharedPreferences(this).contains("streamerMonitorPeriodPref"))
                    Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("streamerMonitorPeriodPref", "15")!!).toLong()
                else
                    15)
        Log.d(tag, "tickerPeriod = ${WorkerStore.instance.tickerPeriod}")

        with(PreferenceManager.getDefaultSharedPreferences(this).edit()){
            remove("streamerName")
            commit() // I commit on main thread to be sure it's been updated before continuing.
        }
        startAlarm(this)

        Log.d(tag, "streamerMonitor created")

        WorkerStore.instance.streamerName.observeForever(streamerNameObserver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra("action")) {
            Actions.NOTIFY.name -> {
                Log.d(tag, "alarm fire" + Actions.NOTIFY.name)
                fetchStreamer(this)
                startAlarm(this) // schedule next alarm
            }
            Actions.KILL.name -> {stopForeground(true); stopSelf()}
        }
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        WorkerStore.instance.streamerName.removeObserver(streamerNameObserver)
        stopAlarm(this)
        super.onDestroy()
    }
}
