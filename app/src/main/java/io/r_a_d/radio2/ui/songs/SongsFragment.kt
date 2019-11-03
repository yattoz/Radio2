package io.r_a_d.radio2.ui.songs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import io.r_a_d.radio2.R
import io.r_a_d.radio2.ui.songs.queuelp.LastPlayedFragment
import io.r_a_d.radio2.ui.songs.queuelp.QueueFragment

class SongsFragment : Fragment() {

    private lateinit var songsViewModel: SongsViewModel
    private lateinit var adapter : SongsPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        songsViewModel =
                ViewModelProviders.of(this).get(SongsViewModel::class.java)

        // We only initialize the whole thing if it has not been done earlier.
        // I feel like it makes the UI a bit snappier. The fragment is not destroyed but re-used.
        if (!songsViewModel.isInitialized)
        {
            // Add Fragments to adapter one by one
            songsViewModel.root = inflater.inflate(R.layout.fragment_songs, container, false)
            songsViewModel.viewPager = songsViewModel.root.findViewById(R.id.tabPager)
            adapter = SongsPagerAdapter(childFragmentManager, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT)
            adapter.addFragment(LastPlayedFragment.newInstance(), "last played")
            adapter.addFragment(QueueFragment.newInstance(), "queue")

            songsViewModel.viewPager.adapter = adapter

            val tabLayout : TabLayout = songsViewModel.root.findViewById(R.id.tabLayout)
            tabLayout.setupWithViewPager(songsViewModel.viewPager)
            songsViewModel.isInitialized = true
            Log.d(tag, "SongFragment view created")
        }

        return songsViewModel.root
    }

}