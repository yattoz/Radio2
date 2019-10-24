package io.r_a_d.radio2

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.MutableLiveData
import java.net.URL
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream

class ApiData(url: String) {

    private val urlToScrape: String = url
    var result: MutableLiveData<JSONObject> = MutableLiveData()

    init {
        result.value = JSONObject()
    }

    fun fetch() {
        doAsync {
            val s  = URL(urlToScrape).readText()
            uiThread  {
                val jsonResult = JSONObject(s)
                result.value = jsonResult
            }
        }
    }

    fun fetchImage(fileUrl: String)
    {
        doAsync {
            val k: InputStream?
            var pic: Bitmap? = null
            try {
                k = URL(fileUrl).content as InputStream
                pic = BitmapFactory.decodeStream(k)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            uiThread {
                PlayerStore.instance.streamerPicture.value = pic
            }
        }
    }

}