package io.r_a_d.radio2

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

class NowPlayingNotification {

    // ########################################
    // ###### NOW PLAYING NOTIFICATION ########
    // ########################################

    // Define the notification in android's swipe-down menu
    lateinit var notification: Notification
    private lateinit var notificationManager: NotificationManager
    private lateinit var builder: NotificationCompat.Builder
    private val notificationChannelId = "io.r_a_d.radio2.NOTIFICATIONS"
    private val notificationId: Int = 1

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createNotificationChannel(c: Context): String {
        val chanName = R.string.nowPlayingNotificationChannel
        val chan = NotificationChannel(this.notificationChannelId, c.getString(chanName), NotificationManager.IMPORTANCE_LOW)
        chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

        notificationManager.createNotificationChannel(chan)

        return this.notificationChannelId
    }

    fun create(c: Context, m: MediaSessionCompat) {
        notificationManager = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationIntent = Intent(c, MainActivity::class.java)
        // The PendingIntent will launch the SAME activity
        // thanks to the launchMode specified in the Manifest : android:launchMode="singleTop"
        val pendingIntent = PendingIntent.getActivity(
            c, 0,
            notificationIntent, 0
        )
        var channelID = ""

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelID = createNotificationChannel(c)
        }
        builder = NotificationCompat.Builder(c, channelID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setSmallIcon(R.drawable.lollipop_logo)
            builder.color = -0x20b3c6
        } else {
            builder.setSmallIcon(R.drawable.normal_logo)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }

        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(m.sessionToken)
                .setShowActionsInCompactView(0)
        )
        builder.priority = NotificationCompat.PRIORITY_LOW // we don't want the phone to ring every time the notification gets updated.


        // The PendingIntent will launch the SAME activity
        // thanks to the launchMode specified in the Manifest : android:launchMode="singleTop"
        builder.setContentIntent(pendingIntent)

        // got it right
        val delIntent = Intent(c, RadioService::class.java)
        delIntent.putExtra("action", Actions.KILL.name)
        val deleteIntent = PendingIntent.getService(c, 0, delIntent, PendingIntent.FLAG_CANCEL_CURRENT)
        builder.setDeleteIntent(deleteIntent)

        // Create the notification with the update(c) call (calls .notify at the end)
        update(c)
    }

    fun update(c: Context, isUpdatingNotificationButton: Boolean = false) {

        if (isUpdatingNotificationButton)
            builder.mActions.clear()

        // Title : Title of notification (usu. songArtist is first)
        // Text : Text of the notification (usu. songTitle is second)
        builder.setContentTitle(PlayerStore.instance.songArtist.value)
        builder.setContentText(PlayerStore.instance.songTitle.value)
        // As subText, we show when the player is stopped. This is a friendly reminder that the metadata won't get updated.
        // Maybe later we could replace it by a nice progressBar? Would it be interesting to have one here? I don't know.
        if (PlayerStore.instance.playbackState.value == PlaybackStateCompat.STATE_STOPPED)
            builder.setSubText("Stopped")
        else
            builder.setSubText(null)

        // TODO define icon in notification. I thought it'd be nice to have the streamer picture.
        // The streamer picture should be downloaded and converted to Bitmap in another thread, as network tasks are forbidden on main thread.
        // See : https://developer.android.com/reference/android/os/NetworkOnMainThreadException
        //// builder.setLargeIcon(icon)

        if (builder.mActions.isEmpty()) {
            val intent = Intent(c, RadioService::class.java)
            val action: NotificationCompat.Action

            action = if (PlayerStore.instance.playbackState.value == PlaybackStateCompat.STATE_PLAYING) {
                intent.putExtra("action", Actions.NPAUSE.name)
                val pendingButtonIntent = PendingIntent.getService(c, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                NotificationCompat.Action.Builder(R.drawable.exo_controls_pause, "Pause", pendingButtonIntent).build()
            } else {
                intent.putExtra("action", Actions.PLAY.name)
                val pendingButtonIntent = PendingIntent.getService(c, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                NotificationCompat.Action.Builder(R.drawable.exo_controls_play,"Play", pendingButtonIntent).build()
            }
            builder.addAction(action)
        }
        notification = builder.build()
        notificationManager.notify(notificationId, notification)
    }

    fun clear()
    {
        notificationManager.cancel(notificationId)
    }
}