package io.r_a_d.radio2.ui.songs

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.viewpager.widget.ViewPager

class SongsViewModel : ViewModel()
{
    var isInitialized: Boolean = false
    lateinit var root: View
    lateinit var viewPager: ViewPager
}