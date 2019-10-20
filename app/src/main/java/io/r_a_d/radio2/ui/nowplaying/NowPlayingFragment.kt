package io.r_a_d.radio2.ui.nowplaying

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.SavedStateViewModelFactory
import io.r_a_d.radio2.*


class NowPlayingFragment : Fragment() {

    /*
    companion object {
        fun newInstance() = NowPlayingFragment()
    }

     */

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

        PlayerStore.instance.songTitle.observe(this, Observer {
            songTitleText.text = it
        })

        PlayerStore.instance.songArtist.observe(this, Observer {
            songArtistText.text = it
        })

        PlayerStore.instance.isPlaying.observe(this, Observer {
            syncPlayPauseButtonImage(root)
        })

        PlayerStore.instance.volume.observe(this, Observer {
            volumeText.text = "$it%"
        })

        seekBarVolume.setOnSeekBarChangeListener(nowPlayingViewModel.seekBarChangeListener)

        syncPlayPauseButtonImage(root)

        val button: ImageButton = root.findViewById(R.id.play_pause)
        button.setOnClickListener{
            PlayerStore.instance.isMeantToPlay.value = !PlayerStore.instance.isPlaying.value!!
        }

        return root
    }

    private fun syncPlayPauseButtonImage(v: View)
    {
        val img = v.findViewById<ImageButton>(R.id.play_pause)

        if (PlayerStore.instance.isPlaying.value!!) {
            img.setImageResource(R.drawable.pause_small)
        } else {
            img.setImageResource(R.drawable.arrow_small)
        }
    }



}
