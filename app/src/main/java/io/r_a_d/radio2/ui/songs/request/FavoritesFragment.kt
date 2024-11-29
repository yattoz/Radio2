package io.r_a_d.radio2.ui.songs.request

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.r_a_d.radio2.*
import io.r_a_d.radio2.databinding.FragmentRequestBinding


class FavoritesFragment : Fragment()  {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var searchView: SearchView
    private lateinit var recyclerSwipe: SwipeRefreshLayout
    private lateinit var binding: FragmentRequestBinding

    private val favoritesSongObserver : Observer<Boolean> = Observer {
        viewAdapter.notifyDataSetChanged()
        createView(binding, isCallback = true) // force-re-create the view, but do not call again the initFavorites (avoid callback loop)
        recyclerSwipe.isRefreshing = false // disable refreshing animation. Needs to be done manually...
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentRequestBinding.inflate(inflater, container, false)

        return createView(binding)
    }

    private fun createView(binding: FragmentRequestBinding, isCallback: Boolean = false) : View?
    {

        viewAdapter = RequestSongAdapter(Requestor.instance.favoritesSongArray)

        val listener : SearchView.OnQueryTextListener = object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                (viewAdapter as RequestSongAdapter).filter(newText ?: "")
                viewAdapter.notifyDataSetChanged()
                return true
            }
        }

        searchView = binding.searchBox
        searchView.queryHint = "Search filter..."
        searchView.setOnQueryTextListener(listener)
        viewManager = LinearLayoutManager(context)
        recyclerView = binding.requestRecycler.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter
        }

        val noUserNameText : TextView = binding.noUserNameText

        recyclerSwipe = binding.recyclerSwipe
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


        val userName1 = preferenceStore.getString("userName", null)
        Log.d(tag,"userName = $userName1")
        if (userName1 != null && !userName1.isBlank())
        {
            noUserNameText.visibility = View.GONE
            if (!isCallback) // avoid callback loop if called from the Observer.
                Requestor.instance.initFavorites()
        } else {
            noUserNameText.visibility = View.VISIBLE
            recyclerSwipe.isRefreshing = false
        }

        val raFButton : AppCompatButton = binding.raFButton

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) // for API21+ Material Design makes ripples on the button.
            raFButton.backgroundTintList = colorGreenList
        else // But on API20- no Material Design support, so we add some more color when clicked
            androidx.core.view.ViewCompat.setBackgroundTintList(raFButton, colorGreenListCompat)
        raFButton.isEnabled = true
        raFButton.isClickable = true
        raFButton.setOnClickListener {
            searchView.clearFocus()
            val s  = Requestor.instance.raF()
            Requestor.instance.snackBarText.value = ""
            Requestor.instance.addRequestMeta = "Request: ${s.artist.value} - ${s.title.value}\n"
            Requestor.instance.request(s.id)
        }
        raFButton.visibility = View.VISIBLE

        Requestor.instance.isFavoritesUpdated.observe(viewLifecycleOwner, favoritesSongObserver)
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        searchView.clearFocus() // hides soft keyboard too.
    }

    companion object {
        @JvmStatic
        fun newInstance() = FavoritesFragment()
    }
}