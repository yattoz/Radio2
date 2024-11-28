package io.r_a_d.radio2.ui.songs.queuelp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.r_a_d.radio2.R
import io.r_a_d.radio2.databinding.FragmentLastPlayedBinding
import io.r_a_d.radio2.playerstore.PlayerStore

class LastPlayedFragment : Fragment() {

    private val lastPlayedFragmentTag = this::class.java.name

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var binding: FragmentLastPlayedBinding


    private val queueObserver = Observer<Boolean> {
        Log.d(tag, lastPlayedFragmentTag + "lp changed")
        viewAdapter.notifyDataSetChanged()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentLastPlayedBinding.inflate(inflater, container, false)

        viewManager = LinearLayoutManager(context)
        viewAdapter = SongAdaptater(PlayerStore.instance.lp)

        recyclerView = binding.queueLpRecycler.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(false)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter
        }

        PlayerStore.instance.isLpUpdated.observeForever(queueObserver)

        return binding.root
    }

    override fun onDestroyView() {
        PlayerStore.instance.isLpUpdated.removeObserver(queueObserver)
        super.onDestroyView()
    }
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener

    companion object {
        @JvmStatic
        fun newInstance() = LastPlayedFragment()
    }
}
