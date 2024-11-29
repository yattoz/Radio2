package io.r_a_d.radio2.ui.news

import android.annotation.SuppressLint
import android.text.method.LinkMovementMethod
import android.widget.TextView
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.HtmlCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import io.r_a_d.radio2.R
import io.r_a_d.radio2.databinding.NewsViewBinding
import io.r_a_d.radio2.playerstore.Song
import kotlin.collections.ArrayList

class NewsAdapter(private val dataSet: ArrayList<News>
    /*,
    context: Context,
    resource: Int,
    objects: Array<out Song>*/
) : RecyclerView.Adapter<NewsAdapter.MyViewHolder>() /*ArrayAdapter<Song>(context, resource, objects)*/ {


    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    class MyViewHolder(private val binding: NewsViewBinding) : RecyclerView.ViewHolder(binding.root)
    {
        fun bind(dataSet: ArrayList<News>, position: Int) {
            val title = binding.newsTitle
            val text = binding.newsText
            val author = binding.newsAuthor
            val header = binding.newsHeader
            title.text = dataSet[position].title
            text.text = HtmlCompat.fromHtml(dataSet[position].text, HtmlCompat.FROM_HTML_MODE_LEGACY)
            text.movementMethod = LinkMovementMethod.getInstance()
            header.text = HtmlCompat.fromHtml(dataSet[position].header, HtmlCompat.FROM_HTML_MODE_LEGACY).replace(Regex("\n"), " ")
            val authorText = "| ${dataSet[position].author}"
            author.text = authorText
            TextViewCompat.setAutoSizeTextTypeWithDefaults(author, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
        }
    }
    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MyViewHolder {
        // create a new view
        val binding = NewsViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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

