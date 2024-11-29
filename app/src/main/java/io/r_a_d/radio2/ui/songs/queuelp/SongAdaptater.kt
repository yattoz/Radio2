package io.r_a_d.radio2.ui.songs.queuelp

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.r_a_d.radio2.colorBlue
import io.r_a_d.radio2.colorWhited
import io.r_a_d.radio2.databinding.SongViewBinding
import io.r_a_d.radio2.playerstore.Song
import io.r_a_d.radio2.tag
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.ArrayList

class SongAdaptater(private val dataSet: ArrayList<Song>
                    /*,
                    context: Context,
                    resource: Int,
                    objects: Array<out Song>*/
) : RecyclerView.Adapter<SongAdaptater.MyViewHolder>() /*ArrayAdapter<Song>(context, resource, objects)*/ {


    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    class MyViewHolder(private val binding: SongViewBinding) : RecyclerView.ViewHolder(binding.root)
    {
        fun bind(dataSet: ArrayList<Song>, position: Int) {
            val hourPlayedTimestamp: Long = dataSet[position].startTime.value!!
            Log.d(tag,"$hourPlayedTimestamp")
            val date = Date(hourPlayedTimestamp)
            Log.d(tag,"$date")
            val dateFormatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
            val hourString: String = dateFormatter.format(date)
            binding.item.text = "${dataSet[position].artist.value} - ${dataSet[position].title.value}"
            binding.itemTime.text = hourString
            if (dataSet[position].type.value == 1) {
                binding.item.setTextColor(colorBlue)
                binding.itemTime.setTextColor(colorBlue)
            }
            else {
                binding.item.setTextColor(colorWhited)
                binding.itemTime.setTextColor(colorWhited)
            }
            // if dataSet.size = 1, it means we display "No Queue".
            binding.itemTime.visibility = if (dataSet.size == 1) View.GONE else View.VISIBLE
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MyViewHolder {

        // create a new view
        val binding = SongViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // set the view's size, margins, paddings and layout parameters
        //...
        return MyViewHolder(binding)
    }

    // Replace the contents of a view (invoked by the layout manager)
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(dataSet, position)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size
}

