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
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.util.Util.getUserAgent
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory

import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.KeyEvent
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.google.android.exoplayer2.metadata.icy.*
import kotlin.math.exp
import kotlin.math.ln


class RadioService : MediaBrowserServiceCompat() {

    private var isForeground: Boolean = false
    private val radioTag = "======RadioService====="
    private lateinit var nowPlayingNotification: NowPlayingNotification
    private val radioServiceId = 1


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
                0 -> Log.d(radioTag, "Headset is unplugged")
                1 -> { Log.d(radioTag, "Headset is plugged"); headsetPluggedIn = true  }
                else -> Log.d(radioTag, "I have no idea what the headset state is")
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
                    Log.d(radioTag, "Can't get state?")
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

        nowPlayingNotification = NowPlayingNotification()
        nowPlayingNotification.create(this, mediaSession)

        PlayerStore.instance.isServiceStarted.value = true
        Log.d(radioTag, "created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getStringExtra("action") == null)
            return super.onStartCommand(intent, flags, startId)

        if (MediaButtonReceiver.handleIntent(mediaSession, intent) != null)
            return super.onStartCommand(intent, flags, startId)

        when (intent.getStringExtra("action")) {
            Actions.PLAY.name -> beginPlaying()
            Actions.STOP.name -> stopPlaying()
            Actions.NPAUSE.name -> pausePlaying()
            Actions.VOLUME.name -> setVolume(intent.getIntExtra("value", 100))
            Actions.KILL.name -> stopSelf()
            //// unused intents.
            //Actions.MUTE.name -> setVolume(0)
            //Actions.UN_MUTE.name -> setVolume(PlayerStore.instance.volume.value)
        }
        Log.d(radioTag, "intent received : " + intent.getStringExtra("action"))
        super.onStartCommand(intent, flags, startId)
        // The service must be re-created if it is destroyed by the system. This allows the user to keep actions like Bluetooth and headphones plug available.
        return Service.START_STICKY_COMPATIBILITY
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        if (mediaSession.controller.playbackState.state != PlaybackStateCompat.STATE_PLAYING) {
            nowPlayingNotification.clear()
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
        Log.d(radioTag, "task removed")
    }

    override fun onDestroy() {
        super.onDestroy()
        player.stop()
        player.release()
        unregisterReceiver(receiver)
        PlayerStore.instance.isServiceStarted.value = false
        Log.d(radioTag, "destroyed")
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

    private fun setupMediaPlayer(){
        player = ExoPlayerFactory.newSimpleInstance(this)
        player.addMetadataOutput {
            for (i in 0 until it.length()) {
                val entry  = it.get(i)
                if (entry is IcyHeaders) {
                    Log.d(radioTag, "onMetadata: IcyHeaders $entry")
                }
                if (entry is IcyInfo) {
                    Log.d(radioTag, "onMetadata: Title ----> ${entry.title}")
                    // Note : Kotlin supports UTF-8 by default.
                    val data = entry.title!!
                    val hyphenPos = data.indexOf(" - ")
                    try {
                        if (hyphenPos < 0)
                            throw ArrayIndexOutOfBoundsException()
                        PlayerStore.instance.songTitle.value = data.substring(hyphenPos + 3)
                        PlayerStore.instance.songArtist.value = data.substring(0, hyphenPos)
                        nowPlayingNotification.update(this)
                    } catch (e: Exception) {
                        PlayerStore.instance.songTitle.value = data
                        PlayerStore.instance.songArtist.value = ""
                    }
                }
            }
        }

        // Produces DataSource instances through which media data is loaded.
        val dataSourceFactory = DefaultDataSourceFactory(
            this,
            getUserAgent(this, getString(R.string.app_name))
        )
        // This is the MediaSource representing the media to be played.
        radioMediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.parse(getString(R.string.STREAM_URL_RADIO)))
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

    fun beginPlaying()
    {
        // the old requestAudioFocus is deprecated on API26+. Using AudioManagerCompat library for consistent code across versions
        val result = AudioManagerCompat.requestAudioFocus(audioManager, audioFocusRequest)
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return
        }

        if (mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING)
            return // nothing to do here
        PlayerStore.instance.playbackState.value = PlaybackStateCompat.STATE_PLAYING

        if (!isForeground)
        {
            startForeground(radioServiceId, nowPlayingNotification.notification)
            isForeground = true
        }

        // Reinitialize media player. Otherwise the playback doesn't resume when beginPlaying. Dunno why.
        // Prepare the player with the source.
        player.prepare(radioMediaSource)

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
        Log.d(radioTag, "begin playing")
    }

    private fun pausePlaying()
    {
        stopPlaying()
    }

    // stop playing but keep the notification.
    // API23+ can keep the notification. API22- must 'blink' it.
    fun stopPlaying()
    {
        if (mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_STOPPED)
            return // nothing to do here
        PlayerStore.instance.playbackState.value = PlaybackStateCompat.STATE_STOPPED

        // STOP THE PLAYBACK
        player.stop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            stopForeground(false)
        else {
            stopForeground(true)
            nowPlayingNotification.clear()
            nowPlayingNotification.create(this, mediaSession)
            //startForeground(radioServiceId, nowPlayingNotification.notification)
        }

        nowPlayingNotification.update(this, true)
        playbackStateBuilder.setState(
            PlaybackStateCompat.STATE_STOPPED,
            0,
            1.0f,
            SystemClock.elapsedRealtime()
        )
        Log.d(radioTag, "stopped")
        isForeground = false
        mediaSession.setPlaybackState(playbackStateBuilder.build())
    }

    fun setVolume(v: Int) {
        // re-shaped volume setter with a logarithmic (ln) function.
        // I think it sounds more natural this way. Adjust coefficient to change the function shape.
        // visualize it on any graphic calculator if you're unsure.
        val c : Float = 2.toFloat()
        val x = v.toFloat()/100
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
            if (PlayerStore.instance.isServiceStarted.value!!) {
                // explicit handling of Media Buttons (for example bluetooth commands)
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
                        if (mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING)
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
                return true
            }
            return false
        }
    }
}
