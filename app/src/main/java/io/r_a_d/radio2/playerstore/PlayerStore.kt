package io.r_a_d.radio2.playerstore

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
        thread.value = resMain.getString("thread")

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
        isAfkStream = resMain.getBoolean("isafkstream")
        listenersCount.value = listeners
        val tagsJsonArr = resMain.getJSONArray("tags")
        tags.clear()
        for (tagIndex in 0 until tagsJsonArr.length())
        {
            tags += tagsJsonArr.getString(tagIndex)
        }

        /* Update Queue. We'll rewrite the whole queue list.
        strategy:
        - If the queue fetched is newer:
            + overwrite queue
            + use fetched Last Played to merge with stored Lp
        - else if the queue fetched isn't newer, and the current song has not been updated:
            + pass (nothing to do)
        - else if the queue fetched isn't newer, and the current song has been updated:
            + re-launch the fetch in 3 seconds.
        */
        if (isIcyChanged)
        {
            // ICY metadata changed from when playing the stream. We know we MUST get:
            // - always, a new list of Last Played. If not, query again the API later.
            // - if we got a new Last Played, and if no streamer is up, a new Queue

        } else {
            // The API update must have been called from a timer. We may, or may not, get:
            // - maybe, a new list of Last Played,
            // - maybe, a new Queue
            if (resMain.has("lp"))
            {
                val queueJSON = resMain.getJSONArray("lp")
                // get the new Last Played ArrayList
                val newLp = ArrayList<Song>()

                for (i in 0 until queueJSON.length()) {
                    val song = extractSong(queueJSON[i] as JSONObject)
                    if (!lp.contains(song))
                        newLp.add(newLp.size, song)
                }
                // Merge the Last Played from the API into the Last Played we have.
                // union() guarantees that the order is preserved
                lp = lp.reversed().union(newLp.reversed()).reversed() as ArrayList<Song>
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
                val t = extractSong(queueJSON[4] as JSONObject)
                if (t == queue.last())
                {
                    Log.d(playerStoreTag, "Song already in there: $t")
                    // Let's end it here. The API doesn't have anything new.
                } else {
                    queue.add(queue.size, t)
                    Log.d(playerStoreTag, "added last queue song: $t")
                    isQueueUpdated.value = true
                }
            }

        }
        updateQueue()





        lastUpdated.value = System.currentTimeMillis()
        Log.d(tag, playerStoreTag + "tags: " + tags);
        Log.d(tag, playerStoreTag + "store updated")
    }

    private val scrape : (Any?) -> String =
    {
        URL(urlToScrape).readText()
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

    private fun updateLp() {
        // note : lp must never be empty. There should always be some songs "last played".
        // if not, then the function has been called before initialization. No need to do anything.
        if (lp.isNotEmpty()) {
            if (lp[0] != currentSongBackup)
            {
                val n = Song()
                n.copy(currentSongBackup)
                lp.add(0, n)
                isLpUpdated.value = true
                Log.d(playerStoreTag, "added last played ${lp[0]}")
                Log.d(playerStoreTag, lp.toString())
            }
            else {
                Log.d(playerStoreTag, "trying to add $currentSongBackup while it already exists. Skipping")
            }
        } else {
            Log.d(playerStoreTag, "last played array is empty (this isn't normal unless it's prior to initialization)")
        }
    }

    private fun updateQueue() {
        if (queue.isNotEmpty()) {
            queue.remove(queue.first())
            Log.d(playerStoreTag, playerStoreTag + queue.toString())
            fetchLastRequest()
            isQueueUpdated.value = true
        } else if (isInitialized) {
            fetchLastRequest()
        } else {
            Log.d(playerStoreTag, playerStoreTag +  "queue is empty!")
        }
    }

    private fun fetchLastRequest()
    {
        val sleepScrape: (Any?) -> String = {
            val sleepTime: Long = 3000
            Thread.sleep(sleepTime)
            URL(urlToScrape).readText()
        }

        lateinit var post: (parameter: Any?) -> Unit

        fun postFun(result: JSONObject)
        {
            if (result.has("main")) {
                val resMain = result.getJSONObject("main")
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
                    val t = extractSong(queueJSON[4] as JSONObject)
                    if (t == queue.last())
                    {
                        Log.d(playerStoreTag, "Song already in there: $t")
                        Async(sleepScrape, post)
                    } else {
                        queue.add(queue.size, t)
                        Log.d(playerStoreTag, "added last queue song: $t")
                        isQueueUpdated.value = true
                    }
                }
            }
        }

        post = {
            val result = JSONObject(it as String)
            /*  The goal is to pass the result to a function that will process it (postFun).
                The magic trick is, under circumstances, the last queue song might not have been updated yet when we fetch it.
                So if this is detected ==> if (t == queue.last() )
                Then the function re-schedule an Async(sleepScrape, post).
                To do that, the "post" must be defined BEFORE the function, but the function must be defined BEFORE the "post" value.
                So I declare "post" as lateinit var, define the function, then define the "post" that calls the function. IT SHOULD WORK.
             */
            postFun(result)
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

