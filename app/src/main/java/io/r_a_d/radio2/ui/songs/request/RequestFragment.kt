package io.r_a_d.radio2.ui.songs.request

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.r_a_d.radio2.databinding.FragmentRequestBinding

class RequestFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var searchView: SearchView
    private lateinit var binding: FragmentRequestBinding

    private val listener : SearchView.OnQueryTextListener = object : SearchView.OnQueryTextListener{
        override fun onQueryTextSubmit(query: String?): Boolean {
            Log.d(tag, "query submitted")
            if (query == null || query.isEmpty()) {
                Requestor.instance.snackBarText.value = "Field is empty, no search possible."
            }
            else {
                Requestor.instance.search(query)
            }
            searchView.clearFocus();
            return true
        }
        override fun onQueryTextChange(newText: String?): Boolean {
            if (newText == "")
            {
                Requestor.instance.reset()
                viewAdapter.notifyDataSetChanged() // this is to remove the "Load more" button
            }
            return true
        }
    }

    private val requestSongObserver = Observer<Boolean> {
        Log.d(tag, "request song list changed")
        viewAdapter.notifyDataSetChanged()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentRequestBinding.inflate(inflater, container, false)

        val recyclerSwipe = binding.recyclerSwipe
        recyclerSwipe.isEnabled = false // don't need to pull-to-refresh for Request

        searchView = binding.searchBox
        searchView.setOnQueryTextListener(listener)

        viewManager = LinearLayoutManager(context)
        viewAdapter = RequestSongAdapter(Requestor.instance.requestSongArray)

        recyclerView = binding.requestRecycler.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter
        }
        Requestor.instance.isRequestResultUpdated.observe(viewLifecycleOwner, requestSongObserver)
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        searchView.clearFocus() // hides soft keyboard too.
    }

    companion object {
        @JvmStatic
        fun newInstance() = RequestFragment()
    }
}