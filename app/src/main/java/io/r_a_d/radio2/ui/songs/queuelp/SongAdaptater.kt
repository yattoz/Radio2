package io.r_a_d.radio2.ui.songs.queuelp

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import io.r_a_d.radio2.R
import io.r_a_d.radio2.colorBlue
import io.r_a_d.radio2.colorWhited
import io.r_a_d.radio2.playerstore.Song
import io.r_a_d.radio2.tag
import kotlinx.android.synthetic.main.song_view.view.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalTime
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
    class MyViewHolder(view: ConstraintLayout) : RecyclerView.ViewHolder(view)


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MyViewHolder {
        // create a new view
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.song_view, parent, false) as ConstraintLayout
        // set the view's size, margins, paddings and layout parameters
        //...
        return MyViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val hourPlayedTimestamp: Long = dataSet[position].startTime.value!!
        Log.d(tag,"$hourPlayedTimestamp")
        val date = Date(hourPlayedTimestamp)
        Log.d(tag,"$date")
        val dateFormatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())

        val hourString: String = dateFormatter.format(date)

        holder.itemView.item.text = "${dataSet[position].artist.value} - ${dataSet[position].title.value}"
        holder.itemView.itemTime.text = hourString
        if (dataSet[position].type.value == 1) {
            holder.itemView.item.setTextColor(colorBlue)
            holder.itemView.itemTime.setTextColor(colorBlue)
        }
        else {
            holder.itemView.item.setTextColor(colorWhited)
            holder.itemView.itemTime.setTextColor(colorWhited)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size


    /*
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // create a new view
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.song_view, parent, false) as ConstraintLayout
    }
    */

}

