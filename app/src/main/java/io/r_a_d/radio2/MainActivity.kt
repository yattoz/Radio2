package io.r_a_d.radio2

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import android.content.Intent
import android.util.Log
import com.google.android.material.bottomnavigation.LabelVisibilityMode.LABEL_VISIBILITY_LABELED
import io.r_a_d.radio2.playerstore.PlayerStore
import java.util.*

class MainActivity : AppCompatActivity() {

    private val clockTicker: Timer = Timer()
    private val activityTag = "=====MainActivity======"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        // timers
        // the clockTicker is used to update the UI. It's OK if it dies when the app loses focus.
        // It should be possible to make this to alleviate the CPU charge. Not that a ticker is heavy on resources, but still.
        clockTicker.schedule(
            Tick(),
            500,
            500
        )

        PlayerStore.instance.fetchApi()

        // Post-UI Launch
        if (savedInstanceState?.getBoolean("isInitialized") == true || PlayerStore.instance.isInitialized)
        {
            Log.d(activityTag, "skipped initialization")
            return
        }

        // if the service is not started, start it in STOP mode.
        // It's not a dummy action : with STOP mode, the player does not buffer audio (and does not use data connection without the user's consent).
        // this is useful since the service must be started to register bluetooth devices buttons.
        // (in case someone opens the app then pushes the PLAY button from their bluetooth device)
        if(!PlayerStore.instance.isServiceStarted.value!!)
            actionOnService(Actions.STOP)

        // initialize some API data
        PlayerStore.instance.initPicture(this)
        PlayerStore.instance.streamerName.value = "" // initializing the streamer name will trigger an initApi from the observer in the Service.
    }

    override fun onDestroy() {
        clockTicker.cancel()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("isInitialized", true)
        super.onSaveInstanceState(outState)
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
