package io.r_a_d.radio2

import androidx.lifecycle.MutableLiveData

class Song {
    val title: MutableLiveData<String> = MutableLiveData()
    val artist: MutableLiveData<String> = MutableLiveData()

    val type: MutableLiveData<Int> = MutableLiveData()
    val startTime: MutableLiveData<Long> = MutableLiveData()
    val stopTime: MutableLiveData<Long> = MutableLiveData()

    init {
        title.value = ""
        artist.value = ""
        type.value = 0
        startTime.value =  System.currentTimeMillis()
        stopTime.value = System.currentTimeMillis() + 1000
    }

    override fun toString() : String {
        return "${artist.value} - ${title.value} | type=${type.value} | times ${startTime.value} - ${stopTime.value}\n"
    }

    fun setTitleArtist(data: String)
    {
        //val data = "Anzai Yukari, Fujita Akane, Noguchi Yuri, Numakura Manami, Suzaki Aya, Uchida Aya - Spatto! Spy & Spice" // TODO DEBUG with a big title.
        val hyphenPos = data.indexOf(" - ")
        try {
            if (hyphenPos < 0)
                throw ArrayIndexOutOfBoundsException()
            artist.value = data.substring(0, hyphenPos)
            title.value = data.substring(hyphenPos + 3)
        } catch (e: Exception) {
            artist.value = ""
            title.value = data
        }
    }

    fun copy(song: Song) {
        this.title.value = song.title.value
        this.artist.value = song.artist.value
        this.startTime.value = song.startTime.value
        this.stopTime.value = song.stopTime.value
        this.type.value = song.type.value
    }
}