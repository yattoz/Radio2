package io.r_a_d.radio2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.MutableLiveData
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.*


class PlayerStore {

    val isPlaying: MutableLiveData<Boolean> = MutableLiveData()
    val isServiceStarted: MutableLiveData<Boolean> = MutableLiveData()
    val volume: MutableLiveData<Int> = MutableLiveData()
    val playbackState: MutableLiveData<Int> = MutableLiveData()
    val currentTime: MutableLiveData<Long> = MutableLiveData()
    val streamerPicture: MutableLiveData<Bitmap> = MutableLiveData()
    val streamerName: MutableLiveData<String> = MutableLiveData()
    val currentSong : Song = Song()
    val currentSongBackup: Song = Song()
    val lp : ArrayDeque<Song> = ArrayDeque()
    val queue : ArrayDeque<Song> = ArrayDeque()
    var isQueueUpdated: MutableLiveData<Boolean> = MutableLiveData()
    private val urlToScrape = "https://r-a-d.io/api"
    var latencyCompensator : Long = 0
    var isInitialized: Boolean = false

    init {
        playbackState.value = PlaybackStateCompat.STATE_STOPPED
        isPlaying.value = false
        isServiceStarted.value = false
        streamerName.value = ""
        volume.value = 100 //TODO: make some settings screen to retain user preference for volume
        currentTime.value = System.currentTimeMillis()
        isQueueUpdated.value = false

    }

    fun initPicture(c: Context) {
        streamerPicture.value = BitmapFactory.decodeResource(c.resources, R.drawable.actionbar_logo)
    }

    private fun updateApi(resMain: JSONObject, isCompensatingLatency : Boolean = false) {
        // If we're not in PLAYING state, update title / artist metadata. If we're playing, the ICY will take care of that.
        if (playbackState.value != PlaybackStateCompat.STATE_PLAYING)
            currentSong.setTitleArtist(resMain.getString("np"))

        // only update the value if the song has changed. This avoids to trigger observers when they shouldn't be triggered
        if (currentSong.startTime.value != resMain.getLong("start_time")*1000)
            currentSong.startTime.value = resMain.getLong("start_time")*1000

        // I noticed that the server has a big (3 to 9 seconds !!) offset for current time.
        // we can measure it when the player is playing, to compensate it and have our progress bar perfectly timed
        // latencyCompensator is set to null when beginPlaying() (we can't measure it at the moment we start playing, since we're in the middle of a song),
        // at this moment, we set it to 0. Then, next time the updateApi is called when we're playing, we measure the latency and we set out latencyComparator.
        if(isCompensatingLatency)
        {
            latencyCompensator = resMain.getLong("current")*1000 - currentSong.startTime.value!!
            Log.d(playerStoreTag, "latency compensator set to ${(latencyCompensator).toFloat()/1000} s")
        }
        currentSong.stopTime.value = resMain.getLong("end_time")*1000
        currentTime.value = (resMain.getLong("current"))*1000 - (latencyCompensator)

        val newStreamer = resMain.getJSONObject("dj").getString("djname")
        if (newStreamer != PlayerStore.instance.streamerName.value)
        {
            val streamerPictureUrl =
                "${urlToScrape}/dj-image/${resMain.getJSONObject("dj").getString("djimage")}"
            fetchImage(streamerPictureUrl)
        }
        streamerName.value = newStreamer
        Log.d(playerStoreTag, "store updated")
    }

    // this is the very first API call
    fun initApi(resMain: JSONObject)
    {
        updateApi(resMain)
        currentSongBackup.copy(currentSong)
        if (resMain.has("queue"))
        {
            val queueJSON =
                resMain.getJSONArray("queue")
            // if my queue is empty, I fill it entirely (startup)
            if (queue.isEmpty())
            {
                for (i in 0 until queueJSON.length())
                {
                    val s = extractSong(queueJSON[i] as JSONObject)
                    if (s.startTime.value != currentSong.startTime.value) // if the API is too slow and didn't remove the first song from queue...
                        queue.addLast(s)
                }
            }
        }
        Log.d(playerStoreTag, queue.toString())

        if (resMain.has("lp"))
        {
            val queueJSON =
                resMain.getJSONArray("lp")
            // if my stack is empty, I fill it entirely (startup)
            if (lp.isEmpty())
            {
                for (i in 0 until queueJSON.length())
                    lp.addLast(extractSong(queueJSON[i] as JSONObject))
            }
        }
        Log.d(playerStoreTag, lp.toString())
        isQueueUpdated.value = true
    }

    private fun fetchLastRequest()
    {
        doAsync {
            Thread.sleep(12000) // we wait a bit (12s) for the API to get updated on R/a/dio side!
            val s = URL(urlToScrape).readText()
            uiThread {
                val result = JSONObject(s)
                if (result.has("main")) {
                    val resMain = result.getJSONObject("main")
                    if (resMain.has("queue"))
                    {
                        val queueJSON =
                            resMain.getJSONArray("queue")
                        val t = extractSong(queueJSON[4] as JSONObject)
                        queue.addLast(t)
                        Log.d(playerStoreTag, "added last queue song: $t")
                    }
                }
            }
        }
    }

    fun updateQueueLp() {
        if (!queue.isEmpty()){
            queue.removeFirst()
            fetchLastRequest()
            Log.d(playerStoreTag, queue.toString())
        }

        if (!lp.isEmpty())
        {
            val n = Song()
            n.copy(currentSongBackup)
            lp.addFirst(n)
            currentSongBackup.copy(currentSong)
            Log.d(playerStoreTag, lp.toString())
        }
        isQueueUpdated.value = true
    }

    private fun extractSong(songJSON: JSONObject) : Song {
        val song = Song()
        song.setTitleArtist(songJSON.getString("meta"))
        song.startTime.value = songJSON.getLong("timestamp")
        song.stopTime.value = song.startTime.value
        song.type.value = songJSON.getInt("type")
        return song
    }

    fun fetchApi(isCompensatingLatency: Boolean = false) {
        doAsync {
            val s = URL(urlToScrape).readText()
            uiThread {
                val result = JSONObject(s)
                if (!result.isNull("main"))
                {
                    val res = result.getJSONObject("main")
                    updateApi(res, isCompensatingLatency)
                }
            }
        }
    }

    private fun fetchImage(fileUrl: String)
    {
        doAsync {
            var k: InputStream? = null
            var pic: Bitmap? = null
            try {
                k = URL(fileUrl).content as InputStream
                val options = BitmapFactory.Options()
                options.inSampleSize = 4
                // this makes 1/4 of origin image size from width and height.
                // it alleviates the memory for API16-API19 especially
                pic = BitmapFactory.decodeStream(k, null, options)
                k.close()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                k?.close()
            }
            uiThread {
                streamerPicture.postValue(pic)
            }
        }
    }

    private val playerStoreTag = "====PlayerStore===="
    companion object {
        val instance by lazy {
            PlayerStore()
        }
    }
}