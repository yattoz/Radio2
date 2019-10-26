package io.r_a_d.radio2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.MutableLiveData
import org.json.JSONObject


class PlayerStore {

    val isPlaying: MutableLiveData<Boolean> = MutableLiveData()
    val isServiceStarted: MutableLiveData<Boolean> = MutableLiveData()
    val songTitle: MutableLiveData<String> = MutableLiveData()
    val songArtist: MutableLiveData<String> = MutableLiveData()
    val volume: MutableLiveData<Int> = MutableLiveData()
    val playbackState: MutableLiveData<Int> = MutableLiveData()
    val startTime: MutableLiveData<Long> = MutableLiveData()
    val stopTime: MutableLiveData<Long> = MutableLiveData()
    val currentTime: MutableLiveData<Long> = MutableLiveData()
    val streamerPicture: MutableLiveData<Bitmap> = MutableLiveData()
    val streamerName: MutableLiveData<String> = MutableLiveData()

    init {
        playbackState.value = PlaybackStateCompat.STATE_STOPPED
        isPlaying.value = false
        isServiceStarted.value = false
        songTitle.value = ""
        songArtist.value = ""
        streamerName.value = ""
        volume.value = 100 //TODO: make some settings screen to retain user preference for volume
        startTime.value =  System.currentTimeMillis()
        stopTime.value = System.currentTimeMillis() + 1000
        currentTime.value = System.currentTimeMillis()
    }

    fun initPicture(c: Context) {
        streamerPicture.value = BitmapFactory.decodeResource(c.resources, R.drawable.actionbar_logo)
    }

    fun updateApi(resMain: JSONObject) {
        startTime.value = resMain.getLong("start_time")*1000
        stopTime.value = resMain.getLong("end_time")*1000

        // I noticed that the server has a big (5 seconds !!) offset for current time.
        // But relying on local time makes it bug with poorly clocked devices
        currentTime.value = (resMain.getLong("current") - 5)*1000
        streamerName.value = resMain.getJSONObject("dj").getString("djname")

        // If we're not in PLAYING state, update title / artist metadata. If we're playing, the ICY will take care of that.
        if (PlayerStore.instance.playbackState.value != PlaybackStateCompat.STATE_PLAYING)
        {
            val data = resMain.getString("np")
            val hyphenPos = data.indexOf(" - ")
            try {
                if (hyphenPos < 0)
                    throw ArrayIndexOutOfBoundsException()
                songTitle.value = data.substring(hyphenPos + 3)
                songArtist.value = data.substring(0, hyphenPos)
            } catch (e: Exception) {
                songTitle.value = data
                songArtist.value = ""
            }
        }
        Log.d("PlayerStore", "store updated")
    }

    companion object {
        val instance by lazy {
            PlayerStore()
        }
    }
}