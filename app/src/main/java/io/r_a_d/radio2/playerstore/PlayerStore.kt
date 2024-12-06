package io.r_a_d.radio2.playerstore

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.MutableLiveData
import io.r_a_d.radio2.*
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.net.URL


class PlayerStore {

    val isPlaying: MutableLiveData<Boolean> = MutableLiveData()
    val isServiceStarted: MutableLiveData<Boolean> = MutableLiveData()
    val volume: MutableLiveData<Int> = MutableLiveData()
    val playbackState: MutableLiveData<Int> = MutableLiveData()
    val currentTime: MutableLiveData<Long> = MutableLiveData()
    val streamerPicture: MutableLiveData<Bitmap> = MutableLiveData()
    val streamerName: MutableLiveData<String> = MutableLiveData()
    val lastUpdated: MutableLiveData<Long> = MutableLiveData()

    val currentSong : Song = Song()
    val currentSongBackup: Song = Song()
    var lp : ArrayList<Song> = ArrayList()
    val queue : ArrayList<Song> = ArrayList()

    val isQueueUpdated: MutableLiveData<Boolean> = MutableLiveData()
    val isLpUpdated: MutableLiveData<Boolean> = MutableLiveData()
    val isMuted : MutableLiveData<Boolean> = MutableLiveData()
    val listenersCount: MutableLiveData<Int> = MutableLiveData()

    var tags: ArrayList<String> = ArrayList()
    private val urlToScrape = "https://r-a-d.io/api"
    var isInitialized: Boolean = false
    var isStreamDown: Boolean = false
    var thread: MutableLiveData<String> = MutableLiveData()
    var isAfkStream: Boolean = true

    private val maxApiFetchRetry = 5
    private var curApiFetchRetry = 0
    private val sleepTime: Long = 4000

    init {
        playbackState.value = PlaybackStateCompat.STATE_STOPPED
        isPlaying.value = false
        isServiceStarted.value = false
        streamerName.value = ""
        volume.value = preferenceStore.getInt("volume", 100)
        currentTime.value = System.currentTimeMillis()
        isQueueUpdated.value = false
        isLpUpdated.value = false
        isMuted.value = false
        currentSong.title.value = noConnectionValue
        listenersCount.value = 0
        thread.value = "none"
    }

    // ##################################################
    // ################# API FUNCTIONS ##################
    // ##################################################

    private fun updateApi(resMain: JSONObject, isIcyChanged: Boolean = false) {
        // If we're not in PLAYING state, update title / artist metadata. If we're playing, the ICY will take care of that.
        if (playbackState.value != PlaybackStateCompat.STATE_PLAYING || currentSong.title.value.isNullOrEmpty()
            || currentSong.title.value == noConnectionValue)
            currentSong.setTitleArtist(resMain.getString("np"))

        // only update the value if the song has changed. This avoids to trigger observers when they shouldn't be triggered

        // if (currentSong.startTime.value != resMain.getLong("start_time")*1000)
        //    currentSong.startTime.value = resMain.getLong("start_time")*1000
        currentSong.startTime.value = resMain.getLong("start_time")*1000
        currentSong.stopTime.value = resMain.getLong("end_time")*1000
        currentTime.value = (resMain.getLong("current"))*1000
        currentSongBackup.copy(currentSong)
        val newStreamer = resMain.getJSONObject("dj").getString("djname")
        if (newStreamer != streamerName.value)
        {
            val streamerPictureUrl =
                "${urlToScrape}/dj-image/${resMain.getJSONObject("dj").getString("djimage")}"
            fetchPicture(streamerPictureUrl)

            streamerName.value = newStreamer
        }
        val listeners = resMain.getInt("listeners")
        // THE ORDER MATTERS. We need to set isAfkStream BEFORE thread, because we're checking
        // whether to display the thread when it changes based on the value of isAfkStream.
        isAfkStream = resMain.getBoolean("isafkstream")
        thread.value = resMain.getString("thread")
        listenersCount.value = listeners
        val tagsJsonArr = resMain.getJSONArray("tags")
        tags.clear()
        for (tagIndex in 0 until tagsJsonArr.length())
        {
            tags += tagsJsonArr.getString(tagIndex)
        }

        val isApiUpToDate = checkApiUpToDate(resMain)

        if (isApiUpToDate)
        {
            Log.d(playerStoreTag, "First call to fetchApi, and the API has something new. Updating Queue/LP...")
            updateQueueAndLp(resMain)
        }
        else if (isIcyChanged)
        {
            // ICY metadata changed from when playing the stream. We know we MUST get:
            // - always, a new list of Last Played. If not, query again the API later.
            // - if we got a new Last Played, and if no streamer is up, a new Queue
            Log.d(playerStoreTag, "First call to fetchApi, and the API isn't updated yet. Scheduling call to fetch again...")
            fetchApiUntilUpToDateAsync()
        }


        lastUpdated.value = System.currentTimeMillis()
        Log.d(playerStoreTag, "PlayerStore updated")
    }

    private val scrape : (Any?) -> String =
    {
        URL(urlToScrape).readText()
    }

    private fun checkApiUpToDate(resMain: JSONObject): Boolean
    {
        val newLp = ArrayList<Song>()
        if (resMain.has("lp"))
        {
            val queueJSON = resMain.getJSONArray("lp")
            // get the new Last Played ArrayList
            for (i in 0 until queueJSON.length()) {
                val song = extractSong(queueJSON[i] as JSONObject)
                if (!lp.contains(song))
                    newLp.add(newLp.size, song)
            }
        }

        // If we added all the songs from the getJSONArray into the NewLp ArrayList, it means that
        // there's a song reported by the API that's not in the last played. The API is up-to-date,
        // and we can use its contents to update our store.
        val isApiUpToDate = newLp.size > 0
        return isApiUpToDate
    }

    /* initApi is called :
        - at startup
        - when a streamer changes.
        the idea is to fetch the queue when a streamer changes (potentially Hanyuu), and at startup.
        The Last Played is only fetched if it's empty (so, only at startup), not when a streamer changes.
     */
    fun initApi()
    {
        val post : (parameter: Any?) -> Unit = {
            val result = JSONObject(it as String)
            if (result.has("main"))
            {
                val resMain = result.getJSONObject("main")
                updateApi(resMain)
                currentSongBackup.copy(currentSong)
                queue.clear()
                if (resMain.has("queue") && resMain.getBoolean("isafkstream"))
                {
                    val queueJSON =
                        resMain.getJSONArray("queue")
                    for (i in 0 until queueJSON.length())
                    {
                        val t = extractSong(queueJSON[i] as JSONObject)
                        if (t != currentSong) // if the API is too slow and didn't remove the first song from queue...
                            queue.add(queue.size, t)
                    }
                }
                isQueueUpdated.value = true
                Log.d(playerStoreTag, queue.toString())

                if (resMain.has("lp"))
                {
                    val queueJSON =
                        resMain.getJSONArray("lp")
                    // if my stack is empty, I fill it entirely (startup)
                    if (lp.isEmpty())
                    {
                        for (i in 0 until queueJSON.length())
                            lp.add(lp.size, extractSong(queueJSON[i] as JSONObject))
                    }
                }
                Log.d(playerStoreTag, lp.toString())
                isLpUpdated.value = true
            }
            isInitialized = true
        }
        Async(scrape, post)
    }

    fun fetchApi(isIcyChanged: Boolean = false) {
        val post: (parameter: Any?) -> Unit = {
            val result = JSONObject(it as String)
            if (!result.isNull("main"))
            {
                val res = result.getJSONObject("main")
                updateApi(res, isIcyChanged)
            }
        }
        Async(scrape, post)
    }

    // ##################################################
    // ############## QUEUE / LP FUNCTIONS ##############
    // ##################################################


    private fun updateQueueAndLp(resMain: JSONObject)
    {
        // The API update must have been called from a timer. We may, or may not, get:
        // - maybe, a new list of Last Played,
        // - maybe, a new Queue
        if (resMain.has("lp"))
        {
            if (resMain.has("lp"))
            {
                val queueJSON = resMain.getJSONArray("lp")
                val queueJSONLength = queueJSON.length()

                // get the new Last Played ArrayList
                for (i in 0 until queueJSONLength) {
                    val song = extractSong(queueJSON[(queueJSONLength - 1) - i] as JSONObject)
                    if (!lp.contains(song))
                        lp.add(0, song)
                }
            }
            Log.d(playerStoreTag, "$lp")
            isLpUpdated.value = true
        }

        if ((resMain.has("isafkstream") && !resMain.getBoolean("isafkstream")) &&
            queue.isNotEmpty())
        {
            queue.clear() //we're not requesting anything anymore.
            isQueueUpdated.value = true
        } else if (resMain.has("isafkstream") && resMain.getBoolean("isafkstream") &&
            queue.isEmpty())
        {
            initApi()
        } else if (resMain.has("queue") && queue.isNotEmpty()) {
            val queueJSON =
                resMain.getJSONArray("queue")
            for (i in 0 until queueJSON.length()) {
                val song = extractSong(queueJSON[i] as JSONObject)
                if (!queue.contains(song)) {
                    queue.removeAt(0)
                    queue.add(queue.size, song)
                    Log.d(playerStoreTag, "added last queue song: $song")
                    isQueueUpdated.value = true
                }
            }
        }
    }

    private fun fetchApiUntilUpToDateAsync()
    {
        // reaching here, we must update queue and Last played.
        /*strategy:
        - If the queue fetched is newer:
            + overwrite queue
            + use fetched Last Played to merge with stored Lp
        - else if the queue fetched isn't newer, and the current song has not been updated:
            + pass (nothing to do)
        - else if the queue fetched isn't newer, and the current song has been updated:
            + re-launch the fetch in 3 seconds.
        */


        val sleepScrape: (Any?) -> String = {
            Thread.sleep(sleepTime)
            URL(urlToScrape).readText()
        }

        lateinit var post: (parameter: Any?) -> Unit

        fun postFun(result: JSONObject)
        {
            if (result.has("main")) {
                val resMain = result.getJSONObject("main")
                val isUpToDate = checkApiUpToDate(resMain)
                if (isUpToDate)
                {
                    Log.d(playerStoreTag, "Re-fetching API successful. Update Queue and Lp...")
                    curApiFetchRetry = 0
                    updateQueueAndLp(resMain)
                } else
                {
                    Log.d(playerStoreTag, "Re-fetching API unsuccessful. Scheduling new call in 3 seconds...")
                    Async(sleepScrape, post)
                }
            }
            return
        }

        post = {
            curApiFetchRetry++
            if (curApiFetchRetry > maxApiFetchRetry)
            {
                Log.w(playerStoreTag, "Retried API fetch $maxApiFetchRetry times, still no news. Aborting")
                curApiFetchRetry = 0
                // assert(false)
            } else {
                val result = JSONObject(it as String)
                postFun(result)
            }
        }

        Async(sleepScrape, post)
    }

    private fun extractSong(songJSON: JSONObject) : Song {
        val song = Song()
        song.setTitleArtist(songJSON.getString("meta"))
        song.startTime.value = songJSON.getLong("timestamp") * 1000
        song.stopTime.value = song.startTime.value
        song.type.value = songJSON.getInt("type")
        song.id = songJSON.getString("meta").hashCode()
        return song
    }

    // ##################################################
    // ############## PICTURE FUNCTIONS #################
    // ##################################################

    private fun fetchPicture(fileUrl: String)
    {
        // TODO: replace with glide and support gif...
        val scrape: (Any?) -> Bitmap? = {
            var k: InputStream? = null
            var pic: Bitmap? = null
            try {
                k = URL(fileUrl).content as InputStream
                val options = BitmapFactory.Options()
                options.inSampleSize = 1
                // this makes 1/2 of origin image size from width and height.
                // it alleviates the memory for API16-API19 especially
                pic = BitmapFactory.decodeStream(k, null, options)
                k.close()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                k?.close()
            }
            pic
        }
        val post : (parameter: Any?) -> Unit = {
            streamerPicture.postValue(it as Bitmap?)
        }
        Async(scrape, post)
    }

    fun initPicture(c: Context) {
        streamerPicture.value = BitmapFactory.decodeResource(c.resources,
            R.drawable.actionbar_logo
        )
    }

    private val playerStoreTag = "PlayerStore"
    companion object {
        val instance by lazy {
            PlayerStore()
        }
    }
}

