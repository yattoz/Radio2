package io.r_a_d.radio2

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import android.media.AudioManager
import android.os.*
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.util.Util.getUserAgent
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory

import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.metadata.icy.*
import io.r_a_d.radio2.playerstore.PlayerStore
import java.util.*
import kotlin.math.exp
import kotlin.math.ln
import kotlin.system.exitProcess


class RadioService : MediaBrowserServiceCompat() {

    private val radioTag = "======RadioService====="
    private lateinit var nowPlayingNotification: NowPlayingNotification
    private val radioServiceId = 1
    private var numberOfSongs = 0
    private val apiTicker: Timer = Timer()
    private var isAlarmStopped = false

    // Define the broadcast receiver to handle any broadcasts
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != null && action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                val i = Intent(context, RadioService::class.java)
                i.putExtra("action", Actions.STOP.name)
                context.startService(i)
            }
            if (action != null && action == Intent.ACTION_HEADSET_PLUG)
            {
                var headsetPluggedIn = false

                // In the Intent state there's the value whether the headphones are plugged or not.
                // This *should* work in any case...
                when (intent.getIntExtra("state", -1)) {
                0 -> Log.d(tag, radioTag + "Headset is unplugged")
                1 -> { Log.d(tag, radioTag + "Headset is plugged"); headsetPluggedIn = true  }
                else -> Log.d(tag, radioTag + "I have no idea what the headset state is")
                }
                /*
                val am = getSystemService(AUDIO_SERVICE) as AudioManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                    for (d in devices)
                    {
                        if (d.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || d.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
                            headsetPluggedIn = true
                    }
                }
                else
                {
                    Log.d(tag, radioTag + "Can't get state?")
                }

                 */
                if((mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_STOPPED
                    || mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_PAUSED)
                    && headsetPluggedIn)
                    beginPlaying()
            }
        }
    }

    // ##################################################
    // ############## LIFECYCLE CALLBACKS ###############
    // ##################################################

    private val titleObserver: Observer<String> = Observer {
        if (PlayerStore.instance.playbackState.value == PlaybackStateCompat.STATE_PLAYING)
        {
            Log.d(tag, radioTag + "SONG CHANGED AND PLAYING")
            // we activate latency compensation only if it's been at least 2 songs...
            PlayerStore.instance.fetchApi(numberOfSongs >= 2)
        }
        nowPlayingNotification.update(this)
    }

    private val volumeObserver: Observer<Int> = Observer {
        setVolume(it)
    }

    private val isMutedObserver: Observer<Boolean> = Observer {
        setVolume(
            if (it)
                null
            else
                -1
        )
    }

    private val isPlayingObserver: Observer<Boolean> = Observer {
        if (it)
            beginPlaying()
        else
            stopPlaying()
    }

    private val startTimeObserver = Observer<Long> {
        // We're listening to startTime to determine if we have to update Queue and Lp.
        // this is because startTime is set by the API and never by the ICY, so both cases are covered (playing and stopped)
        // should be OK even when a new streamer comes in.
        if (it != PlayerStore.instance.currentSongBackup.startTime.value) // we have a new song
        {
            PlayerStore.instance.updateLp()
            PlayerStore.instance.updateQueue()
        }
    }

    private val streamerObserver = Observer<String> {
        val wait: (Any?) -> Any = {
            if (PlayerStore.instance.streamerName.value != "")
                Thread.sleep(2000) // we wait 2 seconds for the Queue to populate
        }
        val post: (Any?) -> Unit = {
            PlayerStore.instance.initApi()
            nowPlayingNotification.update(this) // should update the streamer icon
        }
        Async(wait, post)
    }


    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(null)
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        // Clients can connect, but you can't browse internet radio
        // so onLoadChildren returns nothing. This disables the ability to browse for content.
        return BrowserRoot(getString(R.string.MEDIA_ROOT_ID), null)
    }

    override fun onCreate() {
        super.onCreate()

        // Define managers
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        //define the audioFocusRequest
        val audioFocusRequestBuilder = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
        audioFocusRequestBuilder.setOnAudioFocusChangeListener(focusChangeListener)
        val audioAttributes = AudioAttributesCompat.Builder()
        audioAttributes.setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
        audioAttributes.setUsage(AudioAttributesCompat.USAGE_MEDIA)
        audioFocusRequestBuilder.setAudioAttributes(audioAttributes.build())
        audioFocusRequest = audioFocusRequestBuilder.build()

        // This stuff is for the broadcast receiver
        val filter = IntentFilter()
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        filter.addAction(Intent.ACTION_HEADSET_PLUG)
        registerReceiver(receiver, filter)

        // setup media player
        setupMediaPlayer()
        createMediaSession()

        nowPlayingNotification = NowPlayingNotification(
            notificationChannelId = this.getString(R.string.nowPlayingChannelId),
            notificationChannel = R.string.nowPlayingNotificationChannel,
            notificationId = 1,
            notificationImportance = NotificationCompat.PRIORITY_LOW
        )
        nowPlayingNotification.create(this, mediaSession)


        PlayerStore.instance.streamerName.observeForever(streamerObserver)
        PlayerStore.instance.currentSong.title.observeForever(titleObserver)
        PlayerStore.instance.currentSong.startTime.observeForever(startTimeObserver)
        PlayerStore.instance.volume.observeForever(volumeObserver)
        PlayerStore.instance.isPlaying.observeForever(isPlayingObserver)
        PlayerStore.instance.isMuted.observeForever(isMutedObserver)


        startForeground(radioServiceId, nowPlayingNotification.notification)

        // start ticker for when the player is stopped
        apiTicker.schedule(ApiFetchTick(),10 * 1000,10 * 1000)

        PlayerStore.instance.isServiceStarted.value = true
        Log.d(tag, radioTag + "created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getStringExtra("action") == null)
            return super.onStartCommand(intent, flags, startId)

        if (MediaButtonReceiver.handleIntent(mediaSession, intent) != null)
            return super.onStartCommand(intent, flags, startId)

        when (intent.getStringExtra("action")) {
            Actions.PLAY.name -> beginPlaying()
            Actions.STOP.name -> { isAlarmStopped = true; stopPlaying() }
            Actions.PAUSE.name -> { isAlarmStopped = true; pausePlaying() }
            Actions.VOLUME.name -> setVolume(intent.getIntExtra("value", 100))
            Actions.KILL.name -> {stopForeground(true); stopSelf(); return Service.START_NOT_STICKY}
            Actions.NOTIFY.name -> nowPlayingNotification.update(this)
            Actions.PLAY_OR_FALLBACK.name -> beginPlayingOrFallback()
        }
        Log.d(tag, radioTag + "intent received : " + intent.getStringExtra("action"))
        super.onStartCommand(intent, flags, startId)
        // The service must be re-created if it is destroyed by the system. This allows the user to keep actions like Bluetooth and headphones plug available.
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        if (mediaSession.controller.playbackState.state != PlaybackStateCompat.STATE_PLAYING) {
            nowPlayingNotification.clear()
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
        Log.d(tag, radioTag + "task removed")
    }

    override fun onDestroy() {
        super.onDestroy()
        player.stop()
        player.release()
        unregisterReceiver(receiver)
        PlayerStore.instance.currentSong.title.removeObserver(titleObserver)
        PlayerStore.instance.currentSong.startTime.removeObserver(startTimeObserver)
        PlayerStore.instance.volume.removeObserver(volumeObserver)
        PlayerStore.instance.isPlaying.removeObserver(isPlayingObserver)
        PlayerStore.instance.isMuted.removeObserver(isMutedObserver)


        mediaSession.isActive = false
        mediaSession.setMediaButtonReceiver(null)

        mediaSession.release()

        PlayerStore.instance.isServiceStarted.value = false
        PlayerStore.instance.isInitialized = false

        apiTicker.cancel() // stops the timer.
        Log.d(tag, radioTag + "destroyed")
        // if the service is destroyed, the application had become useless.
        exitProcess(0)
    }

    // ########################################
    // ######## AUDIO FOCUS MANAGEMENT ########
    //#########################################

    // Define the managers
    private var telephonyManager: TelephonyManager? = null
    private lateinit var audioManager: AudioManager
    private lateinit var audioFocusRequest: AudioFocusRequestCompat

    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, incomingNumber: String) {
            super.onCallStateChanged(state, incomingNumber)

            if (state != TelephonyManager.CALL_STATE_IDLE) {
                setVolume(0)
            } else {
                setVolume(PlayerStore.instance.volume.value!!)
            }
        }
    }

    // Define the listener that will control what happens when focus is changed
    private val focusChangeListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> setVolume(20) //20%
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> setVolume(0)
                AudioManager.AUDIOFOCUS_LOSS -> stopPlaying()
                AudioManager.AUDIOFOCUS_GAIN -> setVolume(PlayerStore.instance.volume.value!!)
                else -> {}
            }
        }

    // ########################################
    // ######## MEDIA PLAYER / SESSION ########
    // ########################################

    private lateinit var mediaSession : MediaSessionCompat
    private lateinit var playbackStateBuilder: PlaybackStateCompat.Builder
    private lateinit var metadataBuilder: MediaMetadataCompat.Builder
    private lateinit var player: SimpleExoPlayer
    private lateinit var radioMediaSource: ProgressiveMediaSource
    private lateinit var fallbackMediaSource: ProgressiveMediaSource

    private fun setupMediaPlayer(){
        player = ExoPlayerFactory.newSimpleInstance(this)
        player.addMetadataOutput {
            for (i in 0 until it.length()) {
                val entry  = it.get(i)
                if (entry is IcyHeaders) {
                    Log.d(tag, radioTag + "onMetadata: IcyHeaders $entry")
                }
                if (entry is IcyInfo) {
                    Log.d(tag, radioTag + "onMetadata: Title ----> ${entry.title}")
                    // Note : Kotlin supports UTF-8 by default.
                    numberOfSongs++
                    val data = entry.title!!
                    PlayerStore.instance.currentSong.setTitleArtist(data)
                }
                val d : Long = ((PlayerStore.instance.currentSong.stopTime.value?.minus(PlayerStore.instance.currentSong.startTime.value!!) ?: 0) / 1000)
                val duration = if (d > 0) d - (PlayerStore.instance.latencyCompensator) else 0
                metadataBuilder.putString(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    PlayerStore.instance.currentSong.title.value
                )
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, PlayerStore.instance.currentSong.artist.value)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)

                mediaSession.setMetadata(metadataBuilder.build())

                val intent = Intent("com.android.music.metachanged")
                intent.putExtra("artist", PlayerStore.instance.currentSong.artist.value)
                intent.putExtra("track", PlayerStore.instance.currentSong.title.value)
                intent.putExtra("duration", duration)
                intent.putExtra("position", 0)
                sendBroadcast(intent)
            }
        }
        // this listener allows to reset numberOfSongs if the connection is lost.
        player.addListener(exoPlayerEventListener)

        // Produces DataSource instances through which media data is loaded.
        val dataSourceFactory = DefaultDataSourceFactory(
            this,
            getUserAgent(this, getString(R.string.app_name))
        )
        // This is the MediaSource representing the media to be played.
        radioMediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.parse(getString(R.string.STREAM_URL_RADIO)))

        fallbackMediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.parse("file:///android_asset/the_stream_is_down.mp3"))
    }

    private fun createMediaSession() {
        mediaSession = MediaSessionCompat(this, "RadioMediaSession")
        // Deprecated flags
        // mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS and MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
        mediaSession.isActive = true
        mediaSession.setCallback(mediaSessionCallback)
        playbackStateBuilder = PlaybackStateCompat.Builder()
        playbackStateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
            .setState(PlaybackStateCompat.STATE_STOPPED, 0, 1.0f, SystemClock.elapsedRealtime())

        metadataBuilder = MediaMetadataCompat.Builder()
        mediaSession.setPlaybackState(playbackStateBuilder.build())
    }

    // ########################################
    // ######### SERVICE START/STOP ###########
    // ########################################

    // this function is playing the stream if available, or a default sound if there's a problem.
    private fun beginPlayingOrFallback()
    {
        beginPlaying(isRinging = true, isFallback = false)
        val wait: (Any?) -> Any = {
            /*
            Here we lower the isAlarmStopped flag and we wait for 12s.
            If the player stops the alarm (by calling an intent), the isAlarmStopped flag will be raised.
             */
            isAlarmStopped = false // reset the flag
            var i = 0
            while (i < 12)
            {
                Thread.sleep(1000)
                i++
            }
        }
        val post: (Any?) -> Unit = {
            // we verify : if the player is not playing, and if the user didn't stop it, it means that there's a network issue.
            // So we use the fallback sound to wake up the user!!
            // (note: player.isPlaying is only accessible on main thread, so we can't check in the wait() lambda)
            if (!player.isPlaying && !isAlarmStopped)
            {
                beginPlaying(isRinging = true, isFallback = true)
            }
        }
        Async(wait, post)
    }

    fun beginPlaying(isRinging: Boolean = false, isFallback: Boolean = false)
    {
        if (isRinging)
        {
            //define the audioFocusRequest
            val audioFocusRequestBuilder = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
            audioFocusRequestBuilder.setOnAudioFocusChangeListener(focusChangeListener)
            val audioAttributes = AudioAttributesCompat.Builder()
            audioAttributes.setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
            audioAttributes.setUsage(AudioAttributesCompat.USAGE_ALARM)
            audioFocusRequestBuilder.setAudioAttributes(audioAttributes.build())
            audioFocusRequest = audioFocusRequestBuilder.build()
            player.audioAttributes = com.google.android.exoplayer2.audio.AudioAttributes
                .Builder()
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_ALARM)
                .build()
        } else {
            //define the audioFocusRequest
            val audioFocusRequestBuilder = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
            audioFocusRequestBuilder.setOnAudioFocusChangeListener(focusChangeListener)
            val audioAttributes = AudioAttributesCompat.Builder()
            audioAttributes.setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
            audioAttributes.setUsage(AudioAttributesCompat.USAGE_MEDIA)
            audioFocusRequestBuilder.setAudioAttributes(audioAttributes.build())
            audioFocusRequest = audioFocusRequestBuilder.build()
            player.audioAttributes = com.google.android.exoplayer2.audio.AudioAttributes
                .Builder()
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()
        }
        // the old requestAudioFocus is deprecated on API26+. Using AudioManagerCompat library for consistent code across versions
        val result = AudioManagerCompat.requestAudioFocus(audioManager, audioFocusRequest)
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return
        }

        if (mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING)
            return // nothing to do here
        PlayerStore.instance.playbackState.value = PlaybackStateCompat.STATE_PLAYING

        // Reinitialize media player. Otherwise the playback doesn't resume when beginPlaying. Dunno why.
        // Prepare the player with the source.
        if (isFallback)
        {
            player.prepare(fallbackMediaSource)
            player.repeatMode = ExoPlayer.REPEAT_MODE_ALL
        }
        else {
            player.prepare(radioMediaSource)
            player.repeatMode = ExoPlayer.REPEAT_MODE_OFF
        }

        // START PLAYBACK, LET'S ROCK
        player.playWhenReady = true
        nowPlayingNotification.update(this, true)

        playbackStateBuilder.setState(
            PlaybackStateCompat.STATE_PLAYING,
            0,
            1.0f,
            SystemClock.elapsedRealtime()
        )
        mediaSession.setPlaybackState(playbackStateBuilder.build())
        Log.d(tag, radioTag + "begin playing")
    }

    private fun pausePlaying()
    {
        stopPlaying()
    }

    // stop playing but keep the notification.
    fun stopPlaying()
    {
        if (mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_STOPPED)
            return // nothing to do here
        PlayerStore.instance.playbackState.value = PlaybackStateCompat.STATE_STOPPED

        // STOP THE PLAYBACK
        player.stop()

        nowPlayingNotification.update(this, true)
        playbackStateBuilder.setState(
            PlaybackStateCompat.STATE_STOPPED,
            0,
            1.0f,
            SystemClock.elapsedRealtime()
        )
        Log.d(tag, radioTag + "stopped")

        mediaSession.setPlaybackState(playbackStateBuilder.build())
    }

    fun setVolume(vol: Int?) {
        var v = vol
        when(v)
        {
            null -> { player.volume = 0f ; return } // null means "mute"
            -1 -> v = PlayerStore.instance.volume.value // -1 means "restore previous volume"
        }

        // re-shaped volume setter with a logarithmic (ln) function.
        // I think it sounds more natural this way. Adjust coefficient to change the function shape.
        // visualize it on any graphic calculator if you're unsure.
        val c : Float = 2.toFloat()
        val x : Float = v!!.toFloat()/100
        player.volume = -(1/c)* ln(1-(1- exp(-c))*x)
    }

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            beginPlaying()
        }

        override fun onPause() {
            pausePlaying()
        }

        override fun onStop() {
            stopPlaying()
        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            // explicit handling of Media Buttons (for example bluetooth commands)
            // The hardware key on a corded headphones are handled in the MainActivity (for <API21)
            if (PlayerStore.instance.isServiceStarted.value!!) {
                val keyEvent =
                    mediaButtonEvent?.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                if (keyEvent == null || ((keyEvent.action) != KeyEvent.ACTION_DOWN)) {
                    return false
                }

                when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> {
                        //// Is this some kind of debouncing ? I'm not sure.
                        //if (keyEvent.repeatCount > 0) {
                        //    return false
                        //} else {
                        if (PlayerStore.instance.playbackState.value == PlaybackStateCompat.STATE_PLAYING)
                            pausePlaying()
                        else
                            beginPlaying()
                        //}
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_STOP -> stopPlaying()
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> pausePlaying()
                    KeyEvent.KEYCODE_MEDIA_PLAY -> beginPlaying()
                    else -> return false // these actions are the only ones we acknowledge.
                }

            }
            return false
        }
    }

    private val exoPlayerEventListener = object : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            super.onPlayerStateChanged(playWhenReady, playbackState)
            numberOfSongs = 0
            var state = ""
            when(playbackState)
            {
                Player.STATE_BUFFERING -> state = "Player.STATE_BUFFERING"
                Player.STATE_IDLE -> {
                    state = "Player.STATE_IDLE"
                    // inform the PlayerStore that the playback has stopped. This enables the ticker, triggers API fetch, and updates UI in no-network state.
                    if (PlayerStore.instance.playbackState.value != PlaybackStateCompat.STATE_STOPPED)
                    {
                        PlayerStore.instance.playbackState.postValue(PlaybackStateCompat.STATE_STOPPED)
                        PlayerStore.instance.isPlaying.postValue(false)
                    }
                }
                Player.STATE_ENDED -> state = "Player.STATE_ENDED"
                Player.STATE_READY -> state = "Player.STATE_READY"
            }
            Log.d(tag, radioTag + "Player changed state: ${state}. numberOfSongs reset.")
        }
    }



}
