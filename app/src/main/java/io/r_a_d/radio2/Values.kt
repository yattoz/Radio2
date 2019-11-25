package io.r_a_d.radio2

import android.content.SharedPreferences
import android.content.res.ColorStateList

const val tag = "io.r_a_d.radio2"
const val noConnectionValue = "No connection"
var colorBlue: Int = 0
var colorWhited: Int = 0
var colorGreenList: ColorStateList? = ColorStateList.valueOf(0)
var colorRedList: ColorStateList? = ColorStateList.valueOf(0)
var colorGreenListCompat : ColorStateList? = ColorStateList.valueOf(0)

lateinit var preferenceStore : SharedPreferences

const val minBufferMillis = 15 * 1000 // Default value
const val maxBufferMillis = 50 * 1000 // Default value
const val bufferForPlayback = 4 * 1000 // Default is 2.5s.
// Increasing it makes it more robust to short connection loss, at the expense of latency when we press Play. 4s seems reasonable to me.
const val bufferForPlaybackAfterRebuffer = 7 * 1000 // Default is 5s.