package io.r_a_d.radio2.ui.songs.request

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.r_a_d.radio2.R
import io.r_a_d.radio2.preferenceStore
import kotlinx.android.synthetic.main.fragment_request.*

class FavoritesFragment : Fragment()  {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var searchView: SearchView

    private val listener : SearchView.OnQueryTextListener = object : SearchView.OnQueryTextListener{
        override fun onQueryTextSubmit(query: String?): Boolean {
            // do nothing
            return true
        }
        override fun onQueryTextChange(newText: String?): Boolean {
            (viewAdapter as RequestSongAdapter).filter(newText ?: "")
            return true
        }
    }

    private val favoritesSongObserver : Observer<Boolean> = Observer {
        viewAdapter.notifyDataSetChanged()
    }

    private val snackBarTextObserver: Observer<String?> = Observer {
        if (Requestor.instance.snackBarText.value != "")
        {
            val s = Snackbar.make(recyclerView, Requestor.instance.snackBarText.value as CharSequence, Snackbar.LENGTH_LONG)
            s.show()
            Requestor.instance.snackBarText.value = "" // resetting afterwards to avoid re-triggering it when we enter again the fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val root = inflater.inflate(R.layout.fragment_request, container, false)

        searchView = root.findViewById(R.id.searchBox)
        searchView.queryHint = "Search filter..."
        searchView.setOnQueryTextListener(listener)
        viewManager = LinearLayoutManager(context)

        val userName = preferenceStore.getString("userName", null)
        val noUserNameText : TextView = root.findViewById(R.id.noUserNameText)
        if (userName == null || userName == "")
        {
            noUserNameText.visibility = View.VISIBLE
            return root
        } else {
            noUserNameText.visibility = View.GONE
        }

        viewAdapter = RequestSongAdapter(Requestor.instance.favoritesSongArray)


        recyclerView = root.findViewById<RecyclerView>(R.id.request_recycler).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter
        }
        Requestor.instance.isFavoritesUpdated.observeForever(favoritesSongObserver) // TODO add pull to refresh
        Requestor.instance.snackBarText.observeForever(snackBarTextObserver)

        return root
    }

    override fun onDestroyView() {
        Requestor.instance.isFavoritesUpdated.removeObserver(favoritesSongObserver)
        Requestor.instance.snackBarText.removeObserver(snackBarTextObserver)
        super.onDestroyView()
    }

    companion object {
        @JvmStatic
        fun newInstance() = FavoritesFragment()
    }
}