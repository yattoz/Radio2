package io.r_a_d.radio2

import android.content.Intent
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window

import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.bottomnavigation.BottomNavigationView

abstract class BaseActivity : AppCompatActivity() {
    private val keyboardLayoutListener : ViewTreeObserver.OnGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        val viewHeight = (rootLayout?.rootView?.height ?: 0)
        val height =  ((rootLayout?.height ?: 0))
        val contentViewTop : Int = window.findViewById<View>(Window.ID_ANDROID_CONTENT).top
        val contentViewBottom : Int = window.findViewById<View>(Window.ID_ANDROID_CONTENT).bottom

        Log.d(tag, "$contentViewTop, $viewHeight, $height, $contentViewBottom")


        val broadcastManager = LocalBroadcastManager.getInstance(this@BaseActivity)

        if(height <= viewHeight * 2 / 3){
            val keyboardHeight = viewHeight - height
            onShowKeyboard(keyboardHeight)

            val intent = Intent("KeyboardWillShow")
            intent.putExtra("KeyboardHeight", keyboardHeight)
            broadcastManager.sendBroadcast(intent)
        } else {
            onHideKeyboard()

            val intent = Intent("KeyboardWillHide")
            broadcastManager.sendBroadcast(intent)
        }
    }

    private var keyboardListenersAttached = false
    private var rootLayout: ViewGroup? = null
/*
    protected var onShowKeyboard : (keyboardHeight: Int) -> Unit = {

    }
    protected var onHideKeyboard : () -> Unit = {

    }


 */
// keyboard stuff
    private fun onShowKeyboard(keyboardHeight: Int) {
    // do things when keyboard is shown
    val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)
    bottomNavigationView.visibility = View.GONE
    Log.d(tag, "bottomNav visibility set to GONE (height $keyboardHeight)")
}

     private fun onHideKeyboard() {
        // do things when keyboard is hidden
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNavigationView.visibility = View.VISIBLE
        Log.d(tag, "bottomNav visibility set to VISIBLE")

    }

    protected fun attachKeyboardListeners() {

        if (keyboardListenersAttached) {
            return
        }

        rootLayout = findViewById(R.id.rootLayout)
        rootLayout!!.viewTreeObserver.addOnGlobalLayoutListener(keyboardLayoutListener)

        keyboardListenersAttached = true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (keyboardListenersAttached) {
            rootLayout?.viewTreeObserver?.removeOnGlobalLayoutListener(keyboardLayoutListener)
        }
    }
}