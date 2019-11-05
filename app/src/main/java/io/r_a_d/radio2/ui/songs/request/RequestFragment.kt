package io.r_a_d.radio2.ui.songs.request

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.r_a_d.radio2.R
import io.r_a_d.radio2.playerstore.PlayerStore
import io.r_a_d.radio2.playerstore.Song
import io.r_a_d.radio2.ui.songs.queuelp.SongAdaptater

class RequestFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var searchView: SearchView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_request, container, false)
        searchView = root.findViewById(R.id.searchBox)
        Requestor.instance.search("day")

        /*
        viewManager = LinearLayoutManager(context)
        viewAdapter = SongAdaptater(
            if (PlayerStore.instance.queue.isEmpty())
                ArrayList<Song>(listOf((Song("No queue - "))))
            else
                PlayerStore.instance.queue
        )


        recyclerView = root.findViewById<RecyclerView>(R.id.queue_lp_recycler).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter
        }

         */
        Requestor.instance.snackBarText.observeForever(snackBarTextObserver)
        return root
    }

    override fun onDestroyView() {
        Requestor.instance.snackBarText.removeObserver(snackBarTextObserver)
        super.onDestroyView()
    }

    private val snackBarTextObserver: Observer<String?> = Observer {
        if (Requestor.instance.snackBarText.value != "")
        {
            val s = Snackbar.make(searchView, Requestor.instance.snackBarText.value as CharSequence, Snackbar.LENGTH_LONG)
            s.show()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = RequestFragment()
    }
}