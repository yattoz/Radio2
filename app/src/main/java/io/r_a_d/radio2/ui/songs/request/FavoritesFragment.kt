package io.r_a_d.radio2.ui.songs.request

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.r_a_d.radio2.*


class FavoritesFragment : Fragment()  {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var searchView: SearchView
    private lateinit var root: View
    private lateinit var recyclerSwipe: SwipeRefreshLayout

    private val favoritesSongObserver : Observer<Boolean> = Observer {
        viewAdapter.notifyDataSetChanged()
        createView() // force-re-create the view!
        recyclerSwipe.isRefreshing = false // disable refreshing animation. Needs to be done manually...
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        root = inflater.inflate(R.layout.fragment_request, container, false)

        return createView()
    }

    private fun createView() : View?
    {

        viewAdapter = RequestSongAdapter(Requestor.instance.favoritesSongArray)

        val listener : SearchView.OnQueryTextListener = object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                // do nothing
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                (viewAdapter as RequestSongAdapter).filter(newText ?: "")
                return true
            }
        }

        searchView = root.findViewById(R.id.searchBox)
        searchView.queryHint = "Search filter..."
        searchView.setOnQueryTextListener(listener)
        viewManager = LinearLayoutManager(context)
        recyclerView = root.findViewById<RecyclerView>(R.id.request_recycler).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter
        }

        val userName = preferenceStore.getString("userName", null)
        val noUserNameText : TextView = root.findViewById(R.id.noUserNameText)

        recyclerSwipe = root.findViewById(R.id.recyclerSwipe) as SwipeRefreshLayout
        recyclerSwipe.setOnRefreshListener {
            val userName1 = preferenceStore.getString("userName", null)
            Log.d(tag,"userName = $userName1")
            if (userName1 != null && !userName1.isBlank())
            {
                noUserNameText.visibility = View.GONE
                Requestor.instance.initFavorites()
            } else {
                noUserNameText.visibility = View.VISIBLE
                recyclerSwipe.isRefreshing = false
            }
        }

        if (userName != null && !userName.isBlank())
        {
            noUserNameText.visibility = View.GONE
        } else {
            noUserNameText.visibility = View.VISIBLE
            return root
        }
        val raFButton : AppCompatButton = root.findViewById(R.id.ra_f_button)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) // for API21+ Material Design makes ripples on the button.
            raFButton.supportBackgroundTintList = colorGreenList
        else // But on API20- no Material Design support, so we add some more color when clicked
            raFButton.supportBackgroundTintList = colorGreenListCompat
        raFButton.isEnabled = true
        raFButton.isClickable = true
        raFButton.setOnClickListener {
            val s  = Requestor.instance.raF()
            Requestor.instance.snackBarText.value = ""
            Requestor.instance.addRequestMeta = "Request: ${s.artist.value} - ${s.title.value}\n"
            Requestor.instance.request(s.id)
        }
        raFButton.visibility = View.VISIBLE

        Requestor.instance.isFavoritesUpdated.observeForever(favoritesSongObserver)


        return root
    }

    override fun onDestroyView() {
        Requestor.instance.isFavoritesUpdated.removeObserver(favoritesSongObserver)
        super.onDestroyView()
    }

    companion object {
        @JvmStatic
        fun newInstance() = FavoritesFragment()
    }
}