package io.r_a_d.radio2.ui.songs

import android.os.Bundle
import android.util.AttributeSet
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
import io.r_a_d.radio2.playerstore.PlayerStore
import io.r_a_d.radio2.playerstore.Song
import io.r_a_d.radio2.ui.queuelp.LastPlayedFragment
import io.r_a_d.radio2.ui.queuelp.QueueFragment

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
        val root = inflater.inflate(R.layout.fragment_songs, container, false)
        val viewPager: ViewPager = root.findViewById(R.id.tabPager)


        // Add Fragments to adapter one by one
        adapter = SongsPagerAdapter(childFragmentManager, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT)
        /*
        adapter.addFragment(LastPlayedFragment.newInstance(PlayerStore.instance.lp, PlayerStore.instance.isLpUpdated), "Last played")
        adapter.addFragment(LastPlayedFragment.newInstance(
            if (PlayerStore.instance.queue.isEmpty())
                ArrayList<Song>(listOf((Song("No queue - "))))
            else
                PlayerStore.instance.queue
            , PlayerStore.instance.isQueueUpdated), "Queue")

         */
        adapter.addFragment(LastPlayedFragment.newInstance(), "last played")
        adapter.addFragment(QueueFragment.newInstance(), "queue")


        viewPager.adapter = adapter

        val tabLayout : TabLayout = root.findViewById(R.id.tabLayout)
        tabLayout.setupWithViewPager(viewPager)
        Log.d(tag, "SongFragment view created")

        return root
    }

}