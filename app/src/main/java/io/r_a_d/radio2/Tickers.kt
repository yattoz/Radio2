package io.r_a_d.radio2

import android.support.v4.media.session.PlaybackStateCompat
import io.r_a_d.radio2.alarm.RadioSleeper
import io.r_a_d.radio2.playerstore.PlayerStore
import java.util.*

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
        }
        // We decrease the counter every 10 seconds.
        // I use this ApiFetchTick instead of using the simple Tick (above) because Tick is created by the Activity.
        // When the activity is dismissed, the ticker is destroyed. Whereas this ApiFetchTick is created by the Service.
        // So it won't go away even if the user destroys the activity.
        if (RadioSleeper.instance.durationMillis.value != null)
        {
            RadioSleeper.instance.durationMillis.postValue( RadioSleeper.instance.durationMillis.value!! - (10*1000).toLong())
        }
    }
}
