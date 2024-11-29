package io.r_a_d.radio2.ui.news

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.r_a_d.radio2.R
import io.r_a_d.radio2.databinding.FragmentNewsBinding

class NewsFragment : Fragment() {

    private lateinit var newsViewModel: NewsViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        newsViewModel = ViewModelProvider(this)[NewsViewModel::class.java]

        val binding = FragmentNewsBinding.inflate(inflater, container, false)

        viewManager = LinearLayoutManager(context)
        viewAdapter = NewsAdapter(newsViewModel.newsArray)
        recyclerView = binding.newsRecycler.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter
        }

        binding.root.setOnRefreshListener {
            newsViewModel.fetch(binding.root, viewAdapter)
        }
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        newsViewModel = ViewModelProvider(this)[NewsViewModel::class.java]

        newsViewModel.fetch()
        Log.d(tag, "news fetched onCreate")
        super.onCreate(savedInstanceState)
    }
}