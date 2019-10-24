package io.r_a_d.radio2

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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

        builder.priority = NotificationCompat.PRIORITY_LOW // we don't want the phone to ring every time the notification gets updated.

        // The PendingIntent will launch the SAME activity
        // thanks to the launchMode specified in the Manifest : android:launchMode="singleTop"
        builder.setContentIntent(pendingIntent)

        // got it right
        val delIntent = Intent(c, RadioService::class.java)
        delIntent.putExtra("action", Actions.KILL.name)
        val deleteIntent = PendingIntent.getService(c, 0, delIntent, PendingIntent.FLAG_NO_CREATE)
        builder.setDeleteIntent(deleteIntent)

        builder.setStyle(
            androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle()
            //androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(m.sessionToken)
                .setShowActionsInCompactView(0)
                .setCancelButtonIntent(deleteIntent)
        )
        builder.setColorized(true)
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
        if (PlayerStore.instance.playbackState.value == PlaybackStateCompat.STATE_STOPPED) {
            builder.setSubText("Stopped")
            builder.setShowWhen(false)
        }
        else {
            builder.setSubText(null)
            builder.setShowWhen(true)
        }

        builder.setLargeIcon(PlayerStore.instance.streamerPicture.value)

        // Note : I was unreasonably triggered by the fact that the stop icon was smaller than the others.
        // So I downloaded and used the icons from https://materialdesignicons.com/ (version Android 4, Holo Dark)
        if (builder.mActions.isEmpty()) {
            val intent = Intent(c, RadioService::class.java)
            val playPauseAction: NotificationCompat.Action

            playPauseAction = if (PlayerStore.instance.playbackState.value == PlaybackStateCompat.STATE_PLAYING) {
                intent.putExtra("action", Actions.PAUSE.name)
                val pendingButtonIntent = PendingIntent.getService(c, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                NotificationCompat.Action.Builder(R.drawable.ic_pause, "Pause", pendingButtonIntent).build()
            } else {
                intent.putExtra("action", Actions.PLAY.name)
                val pendingButtonIntent = PendingIntent.getService(c, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                NotificationCompat.Action.Builder(R.drawable.ic_play,"Play", pendingButtonIntent).build()
            }
            builder.addAction(playPauseAction)
            intent.putExtra("action", Actions.KILL.name)
            val pendingButtonIntent = PendingIntent.getService(c, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val stopAction = NotificationCompat.Action.Builder(R.drawable.ic_stop,"Stop", pendingButtonIntent).build()
            builder.addAction(stopAction)
        }
        notification = builder.build()
        notificationManager.notify(notificationId, notification)
    }


    fun clear()
    {
        notificationManager.cancel(notificationId)
    }
}