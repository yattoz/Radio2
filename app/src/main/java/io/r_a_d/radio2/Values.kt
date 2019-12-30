package io.r_a_d.radio2

import android.content.SharedPreferences
import android.content.res.ColorStateList

const val tag = "io.r_a_d.radio2"
const val noConnectionValue = "No connection"

val weekdaysArray : Array<String> = arrayOf( "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

// Below this line is only automatically programmed values. Unless your week does not start with Monday, you don't need to change this.

val weekdays = ArrayList<String>().apply { weekdaysArray.forEach { add(it) } }
val weekdaysSundayFirst = ArrayList<String>().apply {
    weekdays.forEach {
        add(it)
    }
    val lastDay = last()
    removeAt(size - 1)
    add(0, lastDay)
}

var colorBlue: Int = 0
var colorWhited: Int = 0
var colorGreenList: ColorStateList? = ColorStateList.valueOf(0)
var colorRedList: ColorStateList? = ColorStateList.valueOf(0)
var colorGreenListCompat : ColorStateList? = ColorStateList.valueOf(0)

lateinit var preferenceStore : SharedPreferences