package io.r_a_d.radio2

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
import java.net.URLDecoder
import kotlin.math.exp
import kotlin.math.ln


class RadioService : MediaBrowserServiceCompat() {

    private var isForeground: Boolean = false
    private val binder : IBinder = RadioBinder()
    private val radioTag = "======RadioService====="
    private lateinit var nowPlayingNotification: NowPlayingNotification
    private val radioServiceId = 1

    inner class RadioBinder : Binder() {
        fun getService(): RadioService = this@RadioService
    }

    // Define the broadcast receiver to handle any broadcasts
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != null && action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                val i = Intent(context, RadioService::class.java)
                i.putExtra("action", Actions.STOP.name)
                context.startService(i)
            }
            if (action != null && action == Actions.KILL.name)
            {
                Log.d(radioTag, "received stop self")
                stopSelf()
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
                if(!PlayerStore.instance.isPlaying.value!! && headsetPluggedIn)
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
        filter.addAction(Actions.KILL.name)
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
            //// unused intents.
            //Actions.MUTE.name -> setVolume(0)
            //Actions.UN_MUTE.name -> setVolume(PlayerStore.instance.volume.value)
        }
        Log.d(radioTag, "intent received : " + intent.getStringExtra("action"))
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        if (!PlayerStore.instance.isPlaying.value!!) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
        Log.d(radioTag, "task removed")
    }

    override fun onDestroy() {
        super.onDestroy()
        PlayerStore.instance.isServiceStarted.value = false

        // We want to kill the notification on API24+ if the state is STOPPED (and not PAUSE)
        if (mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_STOPPED)
        {
            Log.d(radioTag, "stated was stopped, killing notification")
            nowPlayingNotification.clear()
        }

        player.stop()
        player.release()
        unregisterReceiver(receiver)

        Log.d(radioTag, "destroyed")
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
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

    // Define the listener that will control what happens when focus is changed such
    // as when headphones are unplugged
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
        // Set audio attribute to manage audio focus - only works on API21+
        // manageAudioFocus()

        player.addMetadataOutput {
            for (i in 0 until it.length()) {
                val entry  = it.get(i)
                if (entry is IcyHeaders) {
                    Log.d(radioTag, "onMetadata: IcyHeaders $entry")
                }
                if (entry is IcyInfo) {
                    Log.d(radioTag, "onMetadata: IcyInfo ${entry.rawMetadata}")
                    val hyphenPos = entry.title!!.indexOf(" - ")
                    try {
                        if (hyphenPos < 0)
                            throw ArrayIndexOutOfBoundsException()
                        //no need for URLDecoder.decode(entry.title!!.substring(...), "UTF-8") anymore
                        PlayerStore.instance.songTitle.value = URLDecoder.decode(entry.title!!.substring(hyphenPos + 3), "UTF-8")
                        PlayerStore.instance.songArtist.value = URLDecoder.decode(entry.title!!.substring(0, hyphenPos), "UTF-8")
                        nowPlayingNotification.update(this)
                    } catch (e: Exception) {
                        PlayerStore.instance.songTitle.value = entry.title
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
            .createMediaSource(Uri.parse(getString(R.string.STREAM_URL)))
    }
    /*
    // Note : dealing with audio focus manually (consistent for <API21).
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun manageAudioFocus()
    {
        val audioAttributes = AudioAttributes.Builder()
        audioAttributes.setContentType(C.CONTENT_TYPE_MUSIC)
        audioAttributes.setUsage(C.USAGE_MEDIA)
        val audioAttributesBuilt = audioAttributes.build()
        player.setAudioAttributes(audioAttributesBuilt, true)
    }
     */

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

        if (!isForeground)
        {
            startForeground(radioServiceId, nowPlayingNotification.notification)
            isForeground = true
        }

        // Reinitialize media player. Otherwise the playback doesn't resume when beginPlaying. Dunno why.
        // Prepare the player with the source.
        player.prepare(radioMediaSource)

        PlayerStore.instance.isPlaying.value = true
        PlayerStore.instance.isMeantToPlay.value = true //necessary if restarted from notification
        // START PLAYBACK, LET'S ROCK
        player.playWhenReady = true

        nowPlayingNotification.update(this, isUpdatingNotificationButton = true)

        playbackStateBuilder.setState(
            PlaybackStateCompat.STATE_PLAYING,
            0,
            1.0f,
            SystemClock.elapsedRealtime()
        )
        mediaSession.setPlaybackState(playbackStateBuilder.build())
        Log.d(radioTag, "begin playing")
    }

    // toggling function only meant to be called from the notification.
    // when pausing the stream, the notification should not disappear because we might want to
    // start the stream again from it.
    private fun pausePlaying()
    {

        PlayerStore.instance.isPlaying.value = false
        PlayerStore.instance.isMeantToPlay.value = false
        player.stop()

        // Currently we allow the notification to be detached and removed for API24+.
        // For lower APIs, the notification must stay bound.

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
            isForeground = false
        }
        else
        {
            //stopForeground(true)
            Log.i(radioTag, "API23- must keep the notification bound. Destroy it by stopping the stream and removing the task.")
        }

        nowPlayingNotification.update(this, isUpdatingNotificationButton = true)

        playbackStateBuilder.setState(
            PlaybackStateCompat.STATE_PAUSED,
            0,
            1.0f,
            SystemClock.elapsedRealtime()
        )
        mediaSession.setPlaybackState(playbackStateBuilder.build())
        Log.d(radioTag, "paused")
    }

    fun stopPlaying()
    {
        // STOP THE PLAYBACK
        player.stop()
        PlayerStore.instance.isPlaying.value = false
        PlayerStore.instance.isMeantToPlay.value = false
        if (isForeground) {
            stopForeground(false)
            nowPlayingNotification.update(this, isUpdatingNotificationButton = true)

            isForeground = false
            playbackStateBuilder.setState(
                PlaybackStateCompat.STATE_STOPPED,
                0,
                1.0f,
                SystemClock.elapsedRealtime()
            )
            mediaSession.setPlaybackState(playbackStateBuilder.build())
            Log.d(radioTag, "stopped")
        }
    }

    fun setVolume(v: Int) {
        // re-shaped volume setter with a logarithmic (ln) function.
        // it sounds more natural this way. Adjust coefficient to change the slope.
        val c : Float = 3.toFloat()
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
            // explicit handling of Media Buttons (for example bluetooth commands)
            val keyEvent = mediaButtonEvent?.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            if (keyEvent == null || ((keyEvent.action) != KeyEvent.ACTION_DOWN)) {
                return false
            }

            when (keyEvent.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> {

                    //if (keyEvent.repeatCount > 0) {
                    //    return false
                    //} else {
                        if (PlayerStore.instance.isPlaying.value!!)
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
    }
}
