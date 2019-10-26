package io.r_a_d.radio2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.net.URL
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream

class ApiDataFetcher(url: String) {

    private val urlToScrape: String = url

    fun fetch() {
        doAsync {
            val s = URL(urlToScrape).readText()
            uiThread {
                val result = JSONObject(s)
                if (!result.isNull("main"))
                {
                    val res = result.getJSONObject("main")
                    if (res.getJSONObject("dj").getString("djimage") != PlayerStore.instance.streamerName.value)
                    {
                        val streamerPictureUrl =
                            "${urlToScrape}/dj-image/${res.getJSONObject("dj").getString("djimage")}"
                        fetchImage(streamerPictureUrl)
                    }
                    PlayerStore.instance.updateApi(res)
                }
            }
        }
    }

    private fun fetchImage(fileUrl: String)
    {
        doAsync {
            var k: InputStream? = null
            var pic: Bitmap? = null
            try {
                k = URL(fileUrl).content as InputStream
                pic = BitmapFactory.decodeStream(k)
                k.close()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                k?.close()
            }
            uiThread {
                PlayerStore.instance.streamerPicture.value = pic
            }
        }
    }

}