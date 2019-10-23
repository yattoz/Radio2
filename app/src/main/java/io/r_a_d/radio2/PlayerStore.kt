package io.r_a_d.radio2

import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.MutableLiveData

class PlayerStore {

    val isPlaying: MutableLiveData<Boolean> = MutableLiveData()
    val isServiceStarted: MutableLiveData<Boolean> = MutableLiveData()
    val songTitle: MutableLiveData<String> = MutableLiveData()
    val songArtist: MutableLiveData<String> = MutableLiveData()
    val volume: MutableLiveData<Int> = MutableLiveData()
    val playbackState: MutableLiveData<Int> = MutableLiveData()

    init {
        playbackState.value = PlaybackStateCompat.STATE_STOPPED
        isPlaying.value = false
        isServiceStarted.value = false
        songTitle.value = ""
        songArtist.value = ""
        volume.value = 100 //TODO: make some settings screen to retain user preference for volume
    }

    companion object {
        val instance by lazy {
            PlayerStore()
        }
    }
}