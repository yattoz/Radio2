package io.r_a_d.radio2

import androidx.lifecycle.MutableLiveData

class Song {
    val title: MutableLiveData<String> = MutableLiveData()
    val artist: MutableLiveData<String> = MutableLiveData()
    val startTime: MutableLiveData<Long> = MutableLiveData()
    val stopTime: MutableLiveData<Long> = MutableLiveData()

    init {
        title.value = ""
        artist.value = ""
        startTime.value =  System.currentTimeMillis()
        stopTime.value = System.currentTimeMillis() + 1000
    }
}