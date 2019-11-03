package io.r_a_d.radio2.ui.news

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.r_a_d.radio2.Async
import io.r_a_d.radio2.tag
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class NewsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }
    val text: LiveData<String> = _text
    val newsArray : ArrayList<News> = ArrayList()

    private val urlToScrape = "https://r-a-d.io/api/news"

    private val scrape : () -> Unit =
    {
        val t = URL(urlToScrape).readText()
        val result = JSONArray(t as String)
        for (n in 0 until result.length())
        {
            val news = News()
            news.title = (result[n] as JSONObject).getString("title")
            news.author = (result[n] as JSONObject).getJSONObject("author").getString("user")
            news.text = (result[n] as JSONObject).getString("text")
            news.header = (result[n] as JSONObject).getString("header")

            val formatter6 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            news.date = formatter6.parse((result[n] as JSONObject).getString("updated_at")) ?: Date()

            Log.d(tag, "$news")
            newsArray.add(news)
        }
    }

    fun fetch()
    {
        val post : (parameter: Any?) -> Unit = {

        }
        Async(scrape, post)
    }
}