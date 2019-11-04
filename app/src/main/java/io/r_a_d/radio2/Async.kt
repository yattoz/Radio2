package io.r_a_d.radio2

import android.os.AsyncTask
import android.util.Log
import io.r_a_d.radio2.playerstore.PlayerStore

class Async(val handler: () -> Any?, val post: (Any?) -> Unit = {}) : AsyncTask<Void, Void, Any>() {
    init {
        try {
            execute()
        } catch (e: Exception)
        {
            Log.d(tag,e.toString())
        }
    }

    private fun resetPlayerStateOnNetworkError() {
        var storeReset = false

        // checking isInitialized avoids setting streamerName multiple times, so it avoids a callback loop.
        if (PlayerStore.instance.isInitialized)
        {
            PlayerStore.instance.currentSong.artist.postValue("")
            PlayerStore.instance.isInitialized = false
            PlayerStore.instance.streamerName.postValue("")
            PlayerStore.instance.queue.clear()
            PlayerStore.instance.lp.clear()
            PlayerStore.instance.isQueueUpdated.postValue(true)
            PlayerStore.instance.isLpUpdated.postValue(true)
            // safe-update for the title avoids callback loop too.
            if (PlayerStore.instance.currentSong.title.value != "No connection")
                PlayerStore.instance.currentSong.title.postValue("No connection")
            storeReset = true
        }


        Log.d(tag, "fallback for no network. Store reset : $storeReset")
    }

    override fun doInBackground(vararg params: Void?): Any? {
        try {
            return handler()
        } catch (e: Exception) {
            Log.d(tag,e.toString())
            resetPlayerStateOnNetworkError()
        }
        return null
    }

    override fun onPostExecute(result: Any?) {
        try {
            post(result)
        } catch (e: Exception) {
            Log.d(tag,e.toString())
            resetPlayerStateOnNetworkError()
        }
    }
}