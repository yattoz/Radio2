package io.r_a_d.radio2.ui.nowplaying

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import io.r_a_d.radio2.*
import io.r_a_d.radio2.playerstore.PlayerStore
import io.r_a_d.radio2.playerstore.Song
import io.r_a_d.radio2.ui.queuelp.LastPlayedFragment





class NowPlayingFragment : Fragment() {

    /*
    companion object {
        fun newInstance() = NowPlayingFragment()
    }

     */

    private val nowPlayingFragmentTag = NowPlayingFragment::class.java.name


    private lateinit var nowPlayingViewModel: NowPlayingViewModel

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        nowPlayingViewModel = ViewModelProviders.of(this).get(NowPlayingViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_nowplaying, container, false)

        // View bindings to the ViewModel
        val songTitleText: TextView = root.findViewById(R.id.text_song_title)
        val songArtistText: TextView = root.findViewById(R.id.text_song_artist)
        val seekBarVolume: SeekBar = root.findViewById(R.id.seek_bar_volume)
        val volumeText: TextView = root.findViewById(R.id.volume_text)
        val progressBar: ProgressBar = root.findViewById(R.id.progressBar)
        val streamerPictureImageView: ImageView = root.findViewById(R.id.streamerPicture)
        val streamerNameText : TextView = root.findViewById(R.id.streamerName)
        val songTitleNextText: TextView = root.findViewById(R.id.text_song_title_next)
        val songArtistNextText: TextView = root.findViewById(R.id.text_song_artist_next)


        PlayerStore.instance.currentSong.title.observe(this, Observer {
                songTitleText.text = it
        })

        PlayerStore.instance.currentSong.artist.observe(this, Observer {
                songArtistText.text = it
        })

        PlayerStore.instance.playbackState.observe(this, Observer {
            syncPlayPauseButtonImage(root)
        })

        // trick : I can't observe the queue because it's an ArrayDeque that doesn't trigger any change...
        // so I observe a dedicated Mutable that gets set when the queue is updated.
        PlayerStore.instance.isQueueUpdated.observe(this, Observer {
            val t = if (PlayerStore.instance.queue.size > 0) PlayerStore.instance.queue[0] else Song() // (it.peekFirst != null ? it.peekFirst : Song() )
            songTitleNextText.text = t.title.value
            songArtistNextText.text = t.artist.value
        })

        PlayerStore.instance.volume.observe(this, Observer {
            volumeText.text = "$it%"
        })

        PlayerStore.instance.streamerPicture.observe(this, Observer { pic ->
            streamerPictureImageView.setImageBitmap(pic)
        })

        PlayerStore.instance.streamerName.observe(this, Observer {
            streamerNameText.text = it
        })

        // fuck it, do it on main thread
        PlayerStore.instance.currentTime.observe(this, Observer {
            val dd = (PlayerStore.instance.currentTime.value!! - PlayerStore.instance.currentSong.startTime.value!!).toInt()
            progressBar.progress = dd
        })

        PlayerStore.instance.currentSong.stopTime.observe(this, Observer {
            val dd = (PlayerStore.instance.currentSong.stopTime.value!! - PlayerStore.instance.currentSong.startTime.value!!).toInt()
            progressBar.max = dd
        })

        PlayerStore.instance.currentSong.stopTime.observe(this, Observer {
            val t : TextView= root.findViewById(R.id.endTime)
            val minutes: String = ((PlayerStore.instance.currentSong.stopTime.value!! - PlayerStore.instance.currentSong.startTime.value!!)/60/1000).toString()
            val seconds: String = ((PlayerStore.instance.currentSong.stopTime.value!! - PlayerStore.instance.currentSong.startTime.value!!)/1000%60).toString()
            t.text = "$minutes:${if (seconds.toInt() < 10) "0" else ""}$seconds"
        })

        PlayerStore.instance.currentTime.observe(this, Observer {
            val t : TextView= root.findViewById(R.id.currentTime)
            val minutes: String = ((PlayerStore.instance.currentTime.value!! - PlayerStore.instance.currentSong.startTime.value!!)/60/1000).toString()
            val seconds: String = ((PlayerStore.instance.currentTime.value!! - PlayerStore.instance.currentSong.startTime.value!!)/1000%60).toString()
            t.text = "$minutes:${if (seconds.toInt() < 10) "0" else ""}$seconds"
        })

        seekBarVolume.setOnSeekBarChangeListener(nowPlayingViewModel.seekBarChangeListener)
        seekBarVolume.progress = PlayerStore.instance.volume.value!!
        progressBar.max = 1000
        progressBar.progress = 0

        syncPlayPauseButtonImage(root)

        // initialize the value for isPlaying when displaying the fragment
        PlayerStore.instance.isPlaying.value = PlayerStore.instance.playbackState.value == PlaybackStateCompat.STATE_PLAYING

        val button: ImageButton = root.findViewById(R.id.play_pause)
        button.setOnClickListener{
            PlayerStore.instance.isPlaying.value = PlayerStore.instance.playbackState.value == PlaybackStateCompat.STATE_STOPPED
        }

        return root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    private fun syncPlayPauseButtonImage(v: View)
    {
        val img = v.findViewById<ImageButton>(R.id.play_pause)

        if (PlayerStore.instance.playbackState.value == PlaybackStateCompat.STATE_STOPPED) {
            img.setImageResource(R.drawable.exo_controls_play)
        } else {
            img.setImageResource(R.drawable.exo_controls_pause)
        }
    }



}
