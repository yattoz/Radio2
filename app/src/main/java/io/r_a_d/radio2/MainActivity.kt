package io.r_a_d.radio2

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.setupActionBarWithNavController
import android.content.Intent
import android.util.Log
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import com.google.android.material.bottomnavigation.LabelVisibilityMode.LABEL_VISIBILITY_LABELED
import io.r_a_d.radio2.playerstore.PlayerStore

import java.util.Timer

/* Log to file import
import android.os.Environment
import java.io.File
import java.io.IOException
*/

class MainActivity : AppCompatActivity() {

    private val clockTicker: Timer = Timer()
    private var currentNavController: LiveData<NavController>? = null

    /**
     * Called on first creation and when restoring state.
     */
    private fun setupBottomNavigationBar() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNavigationView.labelVisibilityMode = LABEL_VISIBILITY_LABELED

        //val navGraphIds = listOf(R.navigation.home, R.navigation.list, R.navigation.form)
        val navGraphIds = listOf(R.navigation.navigation_nowplaying, R.navigation.navigation_songs,
            R.navigation.navigation_news, R.navigation.navigation_chat)

        // Setup the bottom navigation view with a list of navigation graphs
        val controller = bottomNavigationView.setupWithNavController(
            navGraphIds = navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_container,
            intent = intent
        )

        // Whenever the selected controller changes, setup the action bar.
        controller.observe(this, Observer { navController ->
            setupActionBarWithNavController(navController)
        })
        currentNavController = controller
    }

    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp() ?: false
    }



    // #####################################
    // ######### LIFECYCLE CALLBACKS #######
    // #####################################

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState ?: Bundle())
        // Now that BottomNavigationBar has restored its instance state
        // and its selectedItemId, we can proceed with setting up the
        // BottomNavigationBar with Navigation
        setupBottomNavigationBar()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // UI Launch
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            setupBottomNavigationBar()
        } // Else, need to wait for onRestoreInstanceState

        /*
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.labelVisibilityMode = LABEL_VISIBILITY_LABELED

        val navController = findNavController(R.id.nav_host_fragment)
        navController.saveState()
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
            R.id.navigation_nowplaying, R.id.navigation_songs, R.id.navigation_dashboard, R.id.navigation_notifications))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

         */

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
            Log.d(tag, "skipped initialization")
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
            Log.d(tag, "Sending intent ${a.name}")
            startService(i)
    }


    // ####################################
    // ###### SERVICE BINDER MANAGER ######
    // ####################################

    // NO BINDERS, only intents. That's the magic.
    // Avoid code duplication, keep a single entry point to modify the service, and manage the service independently
    // (no coupling between service and activity, as it should be ! Cause the notification makes changes too.)


    /*
    // ####################################
    // ####### LOGGING TO FILE ############
    // ####################################

    // Checks if external storage is available for read and write
    private val isExternalStorageWritable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

    // Checks if external storage is available to at least read
    private val isExternalStorageReadable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
        }


    private fun logToFile()
    {

        // Logging
        when {
            isExternalStorageWritable -> {

                val appDirectory = Environment.getExternalStorageDirectory()
                // File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/MyPersonalAppFolder")
                val logDirectory = File("$appDirectory/log")
                val logFile = File(logDirectory, "logcat" + System.currentTimeMillis() + ".txt")
                Log.d(
                    tag,
                    "appDirectory : $appDirectory, logDirectory : $logDirectory, logFile : $logFile"
                )

                // create app folder
                if (!appDirectory.exists()) {
                    appDirectory.mkdir()
                    Log.d(tag, "$appDirectory created")
                }

                // create log folder
                if (!logDirectory.exists()) {
                    logDirectory.mkdir()
                    Log.d(tag, "$logDirectory created")
                }

                // clear the previous logcat and then write the new one to the file
                try {
                    Runtime.getRuntime().exec("logcat -c")
                    Runtime.getRuntime().exec("logcat -v time -f $logFile *:E $tag:V ")
                    Log.d(tag, "logcat started")
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
            isExternalStorageReadable -> {
                // only readable
            }
            else -> {
                // not accessible
            }
        }
    }

     */

}
