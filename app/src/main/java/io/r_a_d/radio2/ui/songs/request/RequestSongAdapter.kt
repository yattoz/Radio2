package io.r_a_d.radio2.ui.songs.request

import android.annotation.SuppressLint
import android.os.Build
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import io.r_a_d.radio2.*
import io.r_a_d.radio2.playerstore.Song
import android.view.View
import android.util.Log
import io.r_a_d.radio2.databinding.RequestSongViewBinding
import java.util.*
import kotlin.collections.ArrayList

const val requestSongAdapterTag = "requestSongAdapterTag"

class RequestSongAdapter(private val dataSet: ArrayList<Song>
) : RecyclerView.Adapter<RequestSongAdapter.RequestSongHolder>() {

    private val viewTypeCell = 1 // normal cell with song and request button
    private val viewTypeFooter = 2 // the bottom cell should be the "load more" button whenever needed

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    class RequestSongHolder(private val binding: RequestSongViewBinding) : RecyclerView.ViewHolder(binding.root)
    {
        fun bind(position: Int, itemCount: Int, dataSet: ArrayList<Song>)
        {
            Log.d(requestSongAdapterTag, "Request Adapter Bind: itemCount=$itemCount, position:$position")

            if (itemCount < 1)
            {
                // if there's nothing to display in the request list, just hide the button and
                // return.
                binding.loadMoreButton.visibility = View.GONE
                return
            }

            if (position == itemCount - 1)
            {
                binding.loadMoreButton.text = "Load more results"
                binding.loadMoreButton.setOnClickListener{
                    Requestor.instance.loadMore()
                }
                binding.loadMoreButton.visibility = View.VISIBLE
            } else {
                binding.loadMoreButton.visibility = View.GONE
            }



            val artist = binding.requestSongArtist
            val title = binding.requestSongTitle
            val button = binding.requestButton

            if (dataSet[position].isRequestable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) // for API21+ Material Design makes ripples on the button.
                    button.supportBackgroundTintList = colorGreenList
                else // But on API20- no Material Design support, so we add some more color when clicked
                    button.supportBackgroundTintList = colorGreenListCompat
                button.isEnabled = true
                button.isClickable = true
                button.setOnClickListener {
                    Requestor.instance.request(dataSet[position].id)
                }
            } else {
                button.supportBackgroundTintList = colorRedList
                button.isEnabled = false
                button.isClickable = false
            }

            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                button,2, 24, 2, TypedValue.COMPLEX_UNIT_SP)
            artist.text = dataSet[position].artist.value
            title.text = dataSet[position].title.value
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): RequestSongHolder {
        // create a new view
        val binding = RequestSongViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RequestSongHolder(binding)
    }

    // Replace the contents of a view (invoked by the layout manager)
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RequestSongHolder, position: Int) {
        holder.bind(position, itemCount, dataSet)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    override fun getItemViewType(position: Int): Int {
        return if (position == dataSet.size) viewTypeFooter else viewTypeCell
    }


    // a filtering function. As naive as it could be, but it should work.
    private val dataSetOrig = ArrayList<Song>()
    init {
        dataSetOrig.addAll(dataSet)
    }

    fun filter(entry: String) {
        var text = entry
        dataSet.clear()
        Log.d(tag, "entering filter")
        if (text.isEmpty()) {
            dataSet.addAll(dataSetOrig)
        } else {
            text = text.lowercase(locale = Locale.ROOT)
            for (item in dataSetOrig) {
                Log.d(tag, "$text, ${item.artist.value!!.lowercase(locale = Locale.ROOT)}, ${item.title.value!!.lowercase(
                    locale = Locale.ROOT
                )}")
                if (item.artist.value!!.lowercase(locale = Locale.ROOT).contains(text) ||
                    item.title.value!!.lowercase(locale = Locale.ROOT).contains(text)) {
                    dataSet.add(item)
                }
            }
        }
        notifyDataSetChanged()
    }

}

