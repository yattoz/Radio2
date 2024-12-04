package io.r_a_d.radio2.ui.nowplaying

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.LinkMovementMethod
import android.util.Log
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.text.HtmlCompat
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import io.r_a_d.radio2.*
import io.r_a_d.radio2.alarm.RadioSleeper
import io.r_a_d.radio2.databinding.FragmentNowplayingBinding
import io.r_a_d.radio2.playerstore.PlayerStore
import io.r_a_d.radio2.playerstore.Song


class NowPlayingFragment : Fragment() {

    private lateinit var nowPlayingViewModel: NowPlayingViewModel
    private lateinit var binding: FragmentNowplayingBinding
    var tagsVisibility: Int = View.GONE

    fun tagsVisibilityToggle() {
        tagsVisibility = if (tagsVisibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        nowPlayingViewModel = ViewModelProvider(this)[NowPlayingViewModel::class.java]
        binding = FragmentNowplayingBinding.inflate(inflater, container, false)

        // View bindings to the ViewModel
        val songTitleText: TextView = binding.textSongTitle
        val songArtistText: TextView = binding.textSongArtist
        val seekBarVolume: SeekBar = binding.seekBarVolume
        val volumeText: TextView = binding.volumeText
        val progressBar: ProgressBar = binding.progressBar
        val streamerPictureImageView: ImageView = binding.streamerPicture
        val streamerNameText : TextView = binding.streamerName
        val songTitleNextText: TextView = binding.textSongTitleNext
        val songArtistNextText: TextView = binding.textSongArtistNext
        val volumeIconImage : ImageView = binding.volumeIcon
        val listenersText : TextView = binding.listenersCount
        val threadText : TextView = binding.thread
        val songTagsText : TextView = binding.textSongTags


        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            streamerNameText,8, 20, 2, TypedValue.COMPLEX_UNIT_SP)
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            listenersText,8, 16, 2, TypedValue.COMPLEX_UNIT_SP)
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            threadText,8, 32, 2, TypedValue.COMPLEX_UNIT_SP)


        PlayerStore.instance.currentSong.title.observe(viewLifecycleOwner, Observer {
            songTitleText.text = it
        })

        PlayerStore.instance.currentSong.artist.observe(viewLifecycleOwner, Observer {
            songArtistText.text = it
        })

        PlayerStore.instance.lastUpdated.observe(viewLifecycleOwner, Observer {
            if (!PlayerStore.instance.isAfkStream)
            {
                songTagsText.text = "tags can't be displayed while a dj is on!"
            } else {
                val s = PlayerStore.instance.tags.fold("", { first, element -> "$first $element" })
                songTagsText.text = "tags: ${if (s.isNotEmpty()) s.substring(1, s.length) else s}"
            }
        })

        PlayerStore.instance.playbackState.observe(viewLifecycleOwner, Observer {
            syncPlayPauseButtonImage()
        })

        // trick : I can't observe the queue because it's an ArrayDeque that doesn't trigger any change...
        // so I observe a dedicated Mutable that gets set when the queue is updated.
        PlayerStore.instance.isQueueUpdated.observe(viewLifecycleOwner, Observer {
            val t = if (PlayerStore.instance.queue.size > 0) PlayerStore.instance.queue[0] else Song("No queue - ") // (it.peekFirst != null ? it.peekFirst : Song() )
            songTitleNextText.text = t.title.value
            songArtistNextText.text = t.artist.value
            val clipboardListenerNext = CreateClipboardListener(requireContext(), t)
            songTitleNextText.setOnLongClickListener(clipboardListenerNext)
            songArtistNextText.setOnLongClickListener(clipboardListenerNext)
        })

        fun volumeIcon(it: Int)
        {
            volumeText.text = "$it%"
            when {
                it > 66 -> volumeIconImage.setImageResource(R.drawable.ic_volume_high)
                it in 33..66 -> volumeIconImage.setImageResource(R.drawable.ic_volume_medium)
                it in 0..33 -> volumeIconImage.setImageResource(R.drawable.ic_volume_low)
                else -> volumeIconImage.setImageResource(R.drawable.ic_volume_off)
            }
        }

        PlayerStore.instance.volume.observe(viewLifecycleOwner, Observer {
            volumeIcon(it)
            seekBarVolume.progress = it // this updates the seekbar if it's set by something else when going to sleep.
        })

        PlayerStore.instance.isMuted.observe(viewLifecycleOwner, Observer {
            if (it)
                volumeIconImage.setImageResource(R.drawable.ic_volume_off)
            else
                volumeIcon(PlayerStore.instance.volume.value!!)
        })


        PlayerStore.instance.streamerPicture.observe(viewLifecycleOwner, Observer { pic ->
            streamerPictureImageView.setImageBitmap(pic)
        })

        PlayerStore.instance.streamerName.observe(viewLifecycleOwner, Observer {
            streamerNameText.text = it
        })

        PlayerStore.instance.listenersCount.observe(viewLifecycleOwner, Observer {
            listenersText.text = "${getString(R.string.listeners)}: $it"
        })

        // fuck it, do it on main thread
        PlayerStore.instance.currentTime.observe(viewLifecycleOwner, Observer {
            val dd = (PlayerStore.instance.currentTime.value!! - PlayerStore.instance.currentSong.startTime.value!!).toInt()
            progressBar.progress = dd
        })

        PlayerStore.instance.currentSong.stopTime.observe(viewLifecycleOwner, Observer {
            val dd = (PlayerStore.instance.currentSong.stopTime.value!! - PlayerStore.instance.currentSong.startTime.value!!).toInt()
            progressBar.max = dd
        })

        PlayerStore.instance.currentSong.stopTime.observe(viewLifecycleOwner, Observer {
            val t : TextView= binding.endTime
            val minutes: String = ((PlayerStore.instance.currentSong.stopTime.value!! - PlayerStore.instance.currentSong.startTime.value!!)/60/1000).toString()
            val seconds: String = ((PlayerStore.instance.currentSong.stopTime.value!! - PlayerStore.instance.currentSong.startTime.value!!)/1000%60).toString()
            t.text = "$minutes:${if (seconds.toInt() < 10) "0" else ""}$seconds"
        })

        PlayerStore.instance.currentTime.observe(viewLifecycleOwner, Observer {
            val t : TextView= binding.currentTime
            val minutes: String = ((PlayerStore.instance.currentTime.value!! - PlayerStore.instance.currentSong.startTime.value!!)/60/1000).toString()
            val seconds: String = ((PlayerStore.instance.currentTime.value!! - PlayerStore.instance.currentSong.startTime.value!!)/1000%60).toString()
            t.text = "$minutes:${if (seconds.toInt() < 10) "0" else ""}$seconds"

            val sleepInfoText = binding.sleepInfo
            val sleepAtMillis = RadioSleeper.instance.sleepAtMillis.value
            if (sleepAtMillis != null)
            {
                val duration = ((sleepAtMillis - System.currentTimeMillis()).toFloat() / (60f * 1000f) + 1).toInt() // I put 1 + it because the division rounds to the lower integer. I'd like to display the round up, like it's usually done.
                sleepInfoText.text = "Will close in $duration minute${if (duration > 1) "s" else ""}"
                sleepInfoText.visibility = View.VISIBLE
            } else {
                sleepInfoText.visibility = View.GONE
            }
        })

        PlayerStore.instance.thread.observe(viewLifecycleOwner, Observer {
            val link = PlayerStore.instance.thread.value!!

            val textLink = if (link.contains("https://ocv.me/up/") ) {
                threadText.visibility = View.VISIBLE
                binding.upNext.visibility = View.GONE
                binding.textSongTitleNext.visibility = View.GONE
                binding.textSongArtistNext.visibility = View.GONE
                "<a href=\"https://ocv.me/up/\">Upload your request!</a>"
            } else if (link != "none" && !PlayerStore.instance.isAfkStream) {
                threadText.visibility = View.VISIBLE
                binding.upNext.visibility = View.GONE
                binding.textSongTitleNext.visibility = View.GONE
                binding.textSongArtistNext.visibility = View.GONE
                var reslink = ""
                if (link.contains("<img src"))
                {

                    val reg = Regex("https:\\S*")
                    val res = reg.find(link)?.value
                    val res2 = res?.substring(0, res.length - 1)
                    val threadImage = binding.threadImage
                    threadImage.visibility = View.VISIBLE
                    threadText.visibility = View.GONE
                    Log.d(tag, "Loading $res2 into threadImage via Glide...")
                    Glide.with(this).load(res2).into(threadImage)
                    reslink = "<a href=\"$res2\">$res2</a>" // unused
                } else if (link.startsWith("https://")) {
                    val reg = Regex("https:\\S*")
                    val res = reg.find(link)?.value
                    reslink = "<a href=\"$res\">Thread up!</a>"
                }
                reslink
            } else {
                threadText.visibility = View.GONE
                binding.upNext.visibility = View.VISIBLE
                binding.textSongTitleNext.visibility = View.VISIBLE
                binding.textSongArtistNext.visibility = View.VISIBLE
                ""
            }
            threadText.text = HtmlCompat.fromHtml(textLink, HtmlCompat.FROM_HTML_MODE_LEGACY)
            threadText.movementMethod = LinkMovementMethod.getInstance()

        })


        seekBarVolume.progress = PlayerStore.instance.volume.value!!
        seekBarVolume.setOnSeekBarChangeListener(nowPlayingViewModel.seekBarChangeListener)

        progressBar.max = 100
        progressBar.progress = 0



        syncPlayPauseButtonImage()

        // initialize the value for isPlaying when displaying the fragment
        PlayerStore.instance.isPlaying.value = PlayerStore.instance.playbackState.value == PlaybackStateCompat.STATE_PLAYING

        val button: ImageButton = binding.playPause
        button.setOnClickListener{
            PlayerStore.instance.isPlaying.value = PlayerStore.instance.playbackState.value == PlaybackStateCompat.STATE_STOPPED
        }

        /*
            /* TODO : disabled volumeIconImage click listener, it creates weird behaviors when switching fragments.
                in particular, the mute state isn't retained when switching fragments, and it creates visual error
                (displaying the mute icon when it's not muted).
                So for the moment it's safer to disable it altogether.
             */
        volumeIconImage.setOnClickListener{
            PlayerStore.instance.isMuted.value = !PlayerStore.instance.isMuted.value!!
        }

         */

        val clipboardListener = CreateClipboardListener(requireContext(), PlayerStore.instance.currentSong)
        songTitleText.setOnLongClickListener(clipboardListener)
        songArtistText.setOnLongClickListener(clipboardListener)

        val showTagsListener: View.OnClickListener = View.OnClickListener {
            tagsVisibilityToggle()
            songTagsText.visibility = tagsVisibility
        }

        // We initialize the visibility to GONE. Every time the Fragment gets active again,
        // the visibility is reset. Not ideal, but good enough.
        tagsVisibility = View.GONE

        songArtistText.setOnClickListener(showTagsListener)
        songTitleText.setOnClickListener(showTagsListener)
        songTagsText.setOnClickListener(showTagsListener)

        if (preferenceStore.getBoolean("splitLayout", true))
            binding.root.addOnLayoutChangeListener(splitLayoutListener)

        return binding.root
    }

    private val splitLayoutListener : View.OnLayoutChangeListener = View.OnLayoutChangeListener { _: View, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int ->

        val isSplitLayout = preferenceStore.getBoolean("splitLayout", true)

        val viewHeight = (binding.root.rootView?.height ?: 1)
        val viewWidth = (binding.root.rootView?.width ?: 1)

        val newRatio = if (viewWidth > 0)
            (viewHeight*100)/viewWidth
        else
            100

        if (isSplitLayout && nowPlayingViewModel.screenRatio != newRatio) {
            onOrientation()
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        onOrientation()
        super.onViewStateRestored(savedInstanceState)
    }

    private fun onOrientation() {
        val viewHeight = (binding.root.rootView?.height ?: 1)
        val viewWidth = (binding.root.rootView?.width ?: 1)

        val isSplitLayout = preferenceStore.getBoolean("splitLayout", true)

        // modify layout to adapt for portrait/landscape
        val isLandscape = viewHeight.toDouble()/viewWidth.toDouble() < 1
        val parentLayout = binding.parentNowPlaying
        val constraintSet = ConstraintSet()
        constraintSet.clone(parentLayout)

        if (isLandscape && isSplitLayout)
        {
            constraintSet.connect(R.id.layoutBlock1, ConstraintSet.BOTTOM, R.id.parentNowPlaying, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.layoutBlock1, ConstraintSet.END, R.id.splitHorizontalLayout, ConstraintSet.END)
            constraintSet.connect(R.id.layoutBlock2, ConstraintSet.TOP, R.id.parentNowPlaying, ConstraintSet.TOP)
            constraintSet.connect(R.id.layoutBlock2, ConstraintSet.START, R.id.splitHorizontalLayout, ConstraintSet.END)
            constraintSet.setMargin(R.id.layoutBlock1, ConstraintSet.END, 16)
            constraintSet.setMargin(R.id.layoutBlock2, ConstraintSet.START, 16)
        } else {
            constraintSet.connect(R.id.layoutBlock1, ConstraintSet.BOTTOM, R.id.splitVerticalLayout, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.layoutBlock1, ConstraintSet.END, R.id.parentNowPlaying, ConstraintSet.END)
            constraintSet.connect(R.id.layoutBlock2, ConstraintSet.TOP, R.id.splitVerticalLayout, ConstraintSet.BOTTOM)
            constraintSet.connect(R.id.layoutBlock2, ConstraintSet.START, R.id.parentNowPlaying, ConstraintSet.START)
            constraintSet.setMargin(R.id.layoutBlock1, ConstraintSet.END, 0)
            constraintSet.setMargin(R.id.layoutBlock2, ConstraintSet.START, 0)
        }
        constraintSet.applyTo(parentLayout)

        // note : we have to COMPARE numbers that are FRACTIONS. And everyone knows that we should NEVER compare DOUBLES because of the imprecision at the end.
        // So instead, I multiply the result by 100 (to give 2 significant numbers), and do an INTEGER DIVISION. This is the right way to compare ratios.
        nowPlayingViewModel.screenRatio = if (viewWidth > 0)
                (viewHeight*100)/viewWidth
        else
            100
        Log.d(tag, "orientation set")
    }

    override fun onResume() {
        super.onResume()
        onOrientation()
    }

    private fun syncPlayPauseButtonImage()
    {
        val img = binding.playPause

        if (PlayerStore.instance.playbackState.value == PlaybackStateCompat.STATE_STOPPED) {
            img.setImageResource(R.drawable.baseline_play_arrow_24)
        } else {
            img.setImageResource(R.drawable.baseline_pause_24)
        }
    }
}
