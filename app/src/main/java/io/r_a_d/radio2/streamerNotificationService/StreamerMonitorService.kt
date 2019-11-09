package io.r_a_d.radio2.streamerNotificationService

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import io.r_a_d.radio2.R
import io.r_a_d.radio2.StreamerMonitorTick
import io.r_a_d.radio2.preferenceStore
import io.r_a_d.radio2.tag
import java.util.Timer

class StreamerMonitorService : Service() {

    // the companion object is meant to access the streamerName (as MutableLiveData)
    companion object {
        val instance = StreamerMonitorService()
    }

    val streamerName = MutableLiveData<String>()
    private lateinit var streamerMonitorNotification : ServiceNotification
    private val streamerMonitorServiceId = 2
    private val tickerPeriod : Long = 4 // seconds
    private val lightTicker: Timer = Timer()

    init {
        streamerName.value = ""
    }

    private val streamerNameObserver: Observer<String> = Observer {
        Log.d(tag, "notification updated")
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


        streamerMonitorNotification.update(this)

    }

    override fun onBind(intent: Intent): IBinder? {
        return null     // no binding allowed nor needed
    }

    override fun onCreate() {
        super.onCreate()

        with(PreferenceManager.getDefaultSharedPreferences(this).edit()){
            remove("streamerName")
            commit() // I commit on main thread to be sure it's been updated before continuing.
        }

        streamerMonitorNotification = ServiceNotification(
            notificationChannelId = this.getString(R.string.streamerServiceChannelId),
            notificationChannel = R.string.streamerServiceChannel,
            notificationId = 2,
            notificationImportance = NotificationCompat.PRIORITY_LOW
        )
        streamerMonitorNotification.create(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForeground(streamerMonitorServiceId, streamerMonitorNotification.notification)


        instance.streamerName.observeForever(streamerNameObserver)

        lightTicker.scheduleAtFixedRate(StreamerMonitorTick(), 1 * 1000, tickerPeriod * 1000)

        Log.d(tag, "streamerMonitor created")
    }

    override fun startService(service: Intent?): ComponentName? {
        return super.startService(service)
    }

    override fun onDestroy() {
        instance.streamerName.removeObserver(streamerNameObserver)
        lightTicker.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }
}
