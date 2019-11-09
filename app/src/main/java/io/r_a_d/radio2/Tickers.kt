package io.r_a_d.radio2

import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import io.r_a_d.radio2.playerstore.PlayerStore
import io.r_a_d.radio2.streamerNotificationService.StreamerMonitorService
import org.json.JSONObject
import java.net.URL
import java.util.*

class StreamerMonitorTick : TimerTask() {
    override fun run() {
        // we implement a lightweight version of the API fetch to minimize CPU activity.
        val urlToScrape = "https://r-a-d.io/api"
        val scrape : (Any?) -> String =
        {
            URL(urlToScrape).readText()
        }
        val post: (parameter: Any?) -> Unit = {
            val result = JSONObject(it as String)
            if (!result.isNull("main"))
            {
                val name = result.getJSONObject("main").getJSONObject("dj").getString("djname")
                StreamerMonitorService.instance.streamerName.postValue(name)
            }
        }
        Async(scrape, post)
    }
}

class Tick  : TimerTask() {
    override fun run() {
        PlayerStore.instance.currentTime.postValue(PlayerStore.instance.currentTime.value!! + 500)
    }
}

class ApiFetchTick  : TimerTask() {
    override fun run() {
        if (PlayerStore.instance.playbackState.value == PlaybackStateCompat.STATE_STOPPED)
        {
            PlayerStore.instance.fetchApi()
            //Log.d(apiFetchTickTag, "mainApiData fetch from object #${this.hashCode()}")
        }
    }
}
