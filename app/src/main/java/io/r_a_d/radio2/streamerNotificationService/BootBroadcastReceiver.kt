package io.r_a_d.radio2.streamerNotificationService

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import io.r_a_d.radio2.Actions
import io.r_a_d.radio2.Async
import io.r_a_d.radio2.R
import io.r_a_d.radio2.tag
import org.json.JSONObject
import java.net.URL


class BootBroadcastReceiver : BroadcastReceiver(){

    override fun onReceive(context: Context, arg1: Intent) {
        if (arg1.action == Intent.ACTION_BOOT_COMPLETED)
        {
            WorkerStore.instance.init(context)
            startStreamerMonitor(context)
        }
    }
}

fun stopAlarm(c: Context) {
    val alarmMgr = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
}

fun startAlarm(c: Context){
    // Hopefully your alarm will have a lower frequency than this!

    val alarmIntent = Intent(c, StreamerMonitorService::class.java).let { intent ->
        intent.putExtra("action", Actions.NOTIFY.name)
        PendingIntent.getService(c, 0, intent, 0)
    }

    val alarmMgr = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> alarmMgr.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + WorkerStore.instance.tickerPeriod * 1000,
            alarmIntent
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> alarmMgr.setExact(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + WorkerStore.instance.tickerPeriod * 1000,
            alarmIntent
        )
        else -> alarmMgr.set(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + WorkerStore.instance.tickerPeriod * 1000,
            alarmIntent
        )
    }
}


fun stopStreamerMonitor(context: Context)
{
    val intent = Intent(context, StreamerMonitorService::class.java)
    intent.putExtra("action", Actions.KILL.name)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startService(intent)
    } else {
        context.startService(intent)
    }
    WorkerStore.instance.isServiceStarted.value = false
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

    val intent = Intent(context, StreamerMonitorService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
    WorkerStore.instance.isServiceStarted.value = true
    Log.i(tag, "Service started on boot")
}

fun fetchStreamer(applicationContext: Context) {
    val urlToScrape = "https://r-a-d.io/api"
    val scrape : (Any?) -> String =
        {
            URL(urlToScrape).readText()
        }
    val post: (parameter: Any?) -> Unit = {
        val result = JSONObject(it as String)
        if (!result.isNull("main"))
        {
            val name = result.getJSONObject("main").getJSONObject("dj").getString("djname")
            WorkerStore.instance.streamerName.postValue(name)
        }
    }

    // notify
    val t = ServiceNotification(
        notificationChannelId = applicationContext.getString(R.string.streamerServiceChannelId),
        notificationChannel = R.string.streamerServiceChannel,
        notificationId = 2,
        notificationImportance = NotificationCompat.PRIORITY_LOW
    )
    t.create(applicationContext)
    t.show()

    try{
        Async(scrape, post)
        Log.d(tag, "enqueue next work in ${WorkerStore.instance.tickerPeriod} seconds")
    } catch (e: Exception) {
    }
}

/*
class StreamerFetchWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    override fun doWork(): Result {

    }
}

fun createWorker() : WorkRequest {

    val streamerFetchWorkRequest = OneTimeWorkRequestBuilder<StreamerFetchWorker>()
    // Add constraints.
    // the general rule is, for a given tickerPeriod, we want the task to be run within tickerPeriod/10
    // we allow a 10% jitter on the task execution to let the OS deal with it.
    // To deal with 5-minute (hence cannot be divided by 10) I do *100 and count it as milliseconds.
    val constraints = Constraints.Builder().apply {
        setRequiresDeviceIdle(false)
        //.setRequiredNetworkType(NetworkType.CONNECTED)
        setTriggerContentMaxDelay(WorkerStore.instance.tickerPeriod * 100, TimeUnit.MILLISECONDS)
    }.build()

    streamerFetchWorkRequest.apply {
        setInitialDelay(0, TimeUnit.SECONDS)
        setBackoffCriteria(
            BackoffPolicy.LINEAR,
            OneTimeWorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS * 6 , // 30s * 6 = 3mn.
            TimeUnit.MILLISECONDS)
        addTag("streamerFetchTag")
    }

    return streamerFetchWorkRequest.setConstraints(constraints).build()
}


 */
