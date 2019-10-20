package io.r_a_d.radio2

import androidx.lifecycle.MutableLiveData

class PlayerStore {

    val isPlaying: MutableLiveData<Boolean> = MutableLiveData()
    val isMeantToPlay: MutableLiveData<Boolean> = MutableLiveData()
    val isServiceStarted: MutableLiveData<Boolean> = MutableLiveData()
    val isBound: MutableLiveData<Boolean> = MutableLiveData()
    val songTitle: MutableLiveData<String> = MutableLiveData()
    val songArtist: MutableLiveData<String> = MutableLiveData()
    val volume: MutableLiveData<Int> = MutableLiveData()

    init {
        isPlaying.value = false
        isMeantToPlay.value = false
        isServiceStarted.value = false
        isBound.value = false
        songTitle.value = "Stopped."
        songArtist.value = ""
        volume.value = 100 //TODO: make some settings screen to retain user preference for volume
    }

    companion object {
        val instance by lazy {
            PlayerStore()
        }
    }
}