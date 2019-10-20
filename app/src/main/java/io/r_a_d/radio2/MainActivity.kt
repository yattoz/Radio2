package io.r_a_d.radio2

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.google.android.material.bottomnavigation.LabelVisibilityMode.LABEL_VISIBILITY_LABELED
import androidx.lifecycle.Observer


class MainActivity : AppCompatActivity() {

    private var service: RadioService = RadioService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        if(PlayerStore.instance.isServiceStarted.value!!)
        {
            Log.d(activityTag, "bind at activity startup")
            bindToService()
            Log.d(activityTag, "after startup: bound = ${PlayerStore.instance.isBound.value}")
        } else {
            // if the service is not started, start it in STOP mode.
            // It's not a dummy action : with STOP mode, the player does not buffer audio (and does not use data connection without the user's consent).
            // this is useful since the service must be started to register bluetooth devices buttons.
            // (in case someone opens the app then pushes the PLAY button from their bluetooth device)
            val i = Intent(this, RadioService::class.java)
            i.putExtra("action", Actions.STOP.name)
            Log.d("MainActivity", "Starting the service in foreground")
            startService(i)
        }

        PlayerStore.instance.isMeantToPlay.observe(this, Observer { newValue ->
            if (newValue != PlayerStore.instance.isPlaying.value)
                syncPlayPause()
        })

        PlayerStore.instance.volume.observe(this, Observer { newValue ->
            actionOnService(Actions.VOLUME, newValue)
        })
    }

    override fun onDestroy() {
        unbindFromService()
        super.onDestroy()
    }

    // ####################################
    // ####### SERVICE PLAY / PAUSE #######
    // ####################################

    private fun syncPlayPause()
    {
        if (!PlayerStore.instance.isPlaying.value!!)
        {
            bindToService()
            actionOnService(Actions.PLAY)
        }
        else if (PlayerStore.instance.isPlaying.value!!)
        {
            actionOnService(Actions.STOP)
            unbindFromService()
        }
    }

    private fun actionOnService(a: Actions, v: Int = 0)
    {
        if (PlayerStore.instance.isServiceStarted.value!! && PlayerStore.instance.isBound.value!!) {
            when (a) {
                Actions.PLAY -> service.beginPlaying()
                Actions.STOP -> service.stopPlaying()
                Actions.VOLUME -> service.setVolume(v)
                else -> return
            }
        } else {
            val i = Intent(this, RadioService::class.java)
            i.putExtra("action", a.name)
            i.putExtra("value", v)
            Log.d("MainActivity", "Starting the service in foreground")
            startService(i)
        }
    }

    // ####################################
    // ###### SERVICE BINDER MANAGER ######
    // ####################################

    private fun bindToService() {
        if (!PlayerStore.instance.isBound.value!!) {
            val i = Intent(this , RadioService::class.java)
            bindService(i, connection, Context.BIND_AUTO_CREATE)
            Log.d(activityTag, "call bind")
        }
    }

    private fun unbindFromService() {
        if (PlayerStore.instance.isBound.value!!) {
            unbindService(connection)
            Log.d(activityTag, "call unbind")
            PlayerStore.instance.isBound.value = false
            Log.d(activityTag, "bound = ${PlayerStore.instance.isBound.value}")
        }
    }

    val activityTag = "=====MainActivity======"

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, a_service: IBinder) {
            val binder = a_service as RadioService.RadioBinder
            service = binder.getService()
            PlayerStore.instance.isBound.value = true
            Log.d(activityTag, "bound = ${PlayerStore.instance.isBound.value}")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            PlayerStore.instance.isBound.value = false
            Log.d(activityTag, "unbound (disconnected)")
        }
    }
}
