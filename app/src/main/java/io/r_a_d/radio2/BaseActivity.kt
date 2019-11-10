package io.r_a_d.radio2

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window

import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.text.Cue
import com.google.android.material.bottomnavigation.BottomNavigationView

abstract class BaseActivity : AppCompatActivity() {
    private val keyboardLayoutListener : ViewTreeObserver.OnGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        val viewHeight = (rootLayout?.rootView?.height ?: 0)
        val viewWidth = (rootLayout?.rootView?.width ?: 0)

        val height =  ((rootLayout?.height ?: 0))
        val width =  ((rootLayout?.width ?: 0))

        Log.d(tag, "$viewWidth, $viewHeight, $width, $height, ${viewHeight.toDouble()/viewWidth.toDouble()}, ${height.toDouble()/width.toDouble()}")



        val broadcastManager = LocalBroadcastManager.getInstance(this@BaseActivity)

        if(height <= viewHeight * 2 / 3 /*height.toDouble()/width.toDouble() < 1.20 */){
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

        // modify layout to adapt for portrait/landscape
        if (viewHeight.toDouble()/viewWidth.toDouble() < 1)
        {
            onOrientation(isLandscape = true)
        } else {
            onOrientation(isLandscape = false)
        }
    }

    private var keyboardListenersAttached = false
    private var rootLayout: ViewGroup? = null

    private fun onOrientation(isLandscape: Boolean = false) {
        val parentLayout = findViewById<ConstraintLayout>(R.id.parentNowPlaying)
        val constraintSet = ConstraintSet()
        constraintSet.clone(parentLayout)

        if (isLandscape)
        {
            /*
            block 1:
                    app:layout_constraintBottom_toBottomOf="parent"
                    app:layout_constraintEnd_toEndOf="@id/splitHorizontalLayout"
            block 2:
                    app:layout_constraintTop_toTopOf="parent"
                    app:layout_constraintStart_toEndOf="@id/splitHorizontalLayout"
             */
            constraintSet.connect(R.id.layoutBlock1, ConstraintSet.BOTTOM, R.id.parentNowPlaying, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.layoutBlock1, ConstraintSet.END, R.id.splitHorizontalLayout, ConstraintSet.END)
            constraintSet.connect(R.id.layoutBlock2, ConstraintSet.TOP, R.id.parentNowPlaying, ConstraintSet.TOP)
            constraintSet.connect(R.id.layoutBlock2, ConstraintSet.START, R.id.splitHorizontalLayout, ConstraintSet.END)
            constraintSet.setMargin(R.id.layoutBlock1, ConstraintSet.END, 16)
            constraintSet.setMargin(R.id.layoutBlock2, ConstraintSet.START, 16)
        } else {
            constraintSet.connect(R.id.layoutBlock1, ConstraintSet.BOTTOM, R.id.splitVerticalLayout, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.layoutBlock1, ConstraintSet.END, R.id.parentNowPlaying, ConstraintSet.END)
            constraintSet.connect(R.id.layoutBlock2, ConstraintSet.TOP, R.id.splitVerticalLayout, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.layoutBlock2, ConstraintSet.START, R.id.parentNowPlaying, ConstraintSet.START)
            constraintSet.setMargin(R.id.layoutBlock1, ConstraintSet.END, 0)
            constraintSet.setMargin(R.id.layoutBlock2, ConstraintSet.START, 0)
        }

        // block 1
        /*
                app:layout_constraintBottom_toTopOf="@id/nowPlayingGuideline"
        app:layout_constraintEnd_toEndOf="parent"
        >
        <!--
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="@id/splitHorizontalLayout"
        android:layout_marginRight="8dp"
        android:layout_marginEnd="8dp"
        -->
         */

        // blokc 2
        /*
        app:layout_constraintTop_toBottomOf="@id/layoutBlock1"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"

        >
        <!--

        android:layout_marginLeft="8dp"
        android:layout_marginStart="8dp"
        -->

         */
        constraintSet.applyTo(parentLayout)

    }


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