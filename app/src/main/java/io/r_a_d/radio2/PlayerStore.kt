package io.r_a_d.radio2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject


class PlayerStore : ViewModel() {

    val isPlaying: MutableLiveData<Boolean> = MutableLiveData()
    val isServiceStarted: MutableLiveData<Boolean> = MutableLiveData()
    val volume: MutableLiveData<Int> = MutableLiveData()
    val playbackState: MutableLiveData<Int> = MutableLiveData()
    val currentTime: MutableLiveData<Long> = MutableLiveData()
    val streamerPicture: MutableLiveData<Bitmap> = MutableLiveData()
    val streamerName: MutableLiveData<String> = MutableLiveData()
    val currentSong : Song = Song()
    val lastPlayed : ArrayList<Song> = ArrayList<Song>()
    val queue : ArrayList<Song> = ArrayList<Song>()

    init {
        playbackState.value = PlaybackStateCompat.STATE_STOPPED
        isPlaying.value = false
        isServiceStarted.value = false
        streamerName.value = ""
        volume.value = 100 //TODO: make some settings screen to retain user preference for volume
        currentTime.value = System.currentTimeMillis()
    }

    fun initPicture(c: Context) {
        streamerPicture.value = BitmapFactory.decodeResource(c.resources, R.drawable.actionbar_logo)
    }

    fun updateApi(resMain: JSONObject) {
        currentSong.startTime.value = resMain.getLong("start_time")*1000
        currentSong.stopTime.value = resMain.getLong("end_time")*1000

        // I noticed that the server has a big (5 seconds !!) offset for current time.
        // But relying on local time makes it bug with poorly clocked devices
        currentTime.value = (resMain.getLong("current") - 5)*1000
        streamerName.value = resMain.getJSONObject("dj").getString("djname")

        // If we're not in PLAYING state, update title / artist metadata. If we're playing, the ICY will take care of that.
        if (PlayerStore.instance.playbackState.value != PlaybackStateCompat.STATE_PLAYING)
        {
            val data = resMain.getString("np")
            //val data = "Anzai Yukari, Fujita Akane, Noguchi Yuri, Numakura Manami, Suzaki Aya, Uchida Aya - Spatto! Spy & Spice" // TODO DEBUG with a big title.
            val hyphenPos = data.indexOf(" - ")
            try {
                if (hyphenPos < 0)
                    throw ArrayIndexOutOfBoundsException()
                currentSong.title.value = data.substring(hyphenPos + 3)
                currentSong.artist.value = data.substring(0, hyphenPos)
            } catch (e: Exception) {
                currentSong.title.value = data
                currentSong.artist.value = ""
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