package io.r_a_d.radio2

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import android.content.Intent
import android.media.session.PlaybackState
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.google.android.material.bottomnavigation.LabelVisibilityMode.LABEL_VISIBILITY_LABELED
import androidx.lifecycle.Observer
import java.util.*

class MainActivity : AppCompatActivity() {

    private val clockTicker: Timer = Timer()
    private val activityTag = "=====MainActivity======"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pre-UI launch
        val apiTicker = Timer()



        // UI Launch
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.labelVisibilityMode = LABEL_VISIBILITY_LABELED

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
            R.id.navigation_nowplaying, R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Post-UI Launch

        if(!PlayerStore.instance.isServiceStarted.value!!)
        {
            // if the service is not started, start it in STOP mode.
            // It's not a dummy action : with STOP mode, the player does not buffer audio (and does not use data connection without the user's consent).
            // this is useful since the service must be started to register bluetooth devices buttons.
            // (in case someone opens the app then pushes the PLAY button from their bluetooth device)
            val i = Intent(this, RadioService::class.java)
            i.putExtra("action", Actions.STOP.name)
            Log.d(activityTag, "Starting the service in foreground")
            startService(i)
        }

        PlayerStore.instance.isPlaying.observe(this, Observer { newValue ->
            if (newValue)
                actionOnService(Actions.PLAY)
            else
                actionOnService(Actions.STOP)
        })

        PlayerStore.instance.volume.observe(this, Observer { newValue ->
            actionOnService(Actions.VOLUME, newValue)
        })

        val apiFetchTick = ApiFetchTick()
        apiTicker.schedule(apiFetchTick,100,10 * 1000)

        PlayerStore.instance.currentSong.title.observe(this, Observer {
            if (PlayerStore.instance.playbackState.value == PlaybackStateCompat.STATE_PLAYING)
            {
                ApiDataFetcher(getString(R.string.MAIN_API)).fetch()
                Log.d(activityTag, "Song changed, API fetched")
            }
        })

        PlayerStore.instance.initPicture(this)

        clockTicker.schedule(
            Tick(),
            500,
            500
        )

        //val monitor = StreamerMonitorWorkerWrap()
        //monitor.enqueuePeriodic(this)
    }

    override fun onDestroy() {
        clockTicker.cancel()
        super.onDestroy()
    }

    // ####################################
    // ####### SERVICE PLAY / PAUSE #######
    // ####################################


    private fun actionOnService(a: Actions, v: Int = 0)
    {
            val i = Intent(this, RadioService::class.java)
            i.putExtra("action", a.name)
            i.putExtra("value", v)
            Log.d(activityTag, "Sending intent ${a.name}")
            startService(i)
    }

    // ####################################
    // ###### SERVICE BINDER MANAGER ######
    // ####################################

    // NO BINDERS, only intents. That's the magic.
    // Avoid code duplication, keep a single entry point to modify the service, and manage the service independently
    // (no coupling between service and activity, as it should be ! Cause the notification makes changes too.)
}
