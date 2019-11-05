package io.r_a_d.radio2.ui.songs.request

import android.util.Log
import androidx.lifecycle.MutableLiveData
import io.r_a_d.radio2.ActionOnError
import io.r_a_d.radio2.Async
import io.r_a_d.radio2.tag
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.CookieHandler
import java.net.CookieManager
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import java.util.regex.Pattern

import javax.net.ssl.HttpsURLConnection

/**
 * Requests a song via the website's API
 *
 * We scrape the website for a CSRF token and POST it to /request/ endpoint with
 * the song id
 *
 * Created by Kethsar on 1/2/2017.
 * Converted to Kotlin and adapted by Yattoz on 05 Nov. 2019
 */

class Requestor {
    private val cookieManager: CookieManager = CookieManager()
    private val requestUrl = "https://r-a-d.io/request/%1\$d"
    private val searchUrl = "https://r-a-d.io/api/search/%1s?page=%2\$d"

    var token: String? = null
    val snackBarText : MutableLiveData<String?> = MutableLiveData()
    var responseArray : ArrayList<RequestResponse> = ArrayList()

    init {
        snackBarText.value = ""
    }

    /**
     * Scrape the website for the CSRF token required for requesting
     * scrapeToken and postToken are the two lambas run by the Async() class.
     */

    private val scrapeToken : (Any?) -> Any? = {
        val radioSearchUrl = "https://r-a-d.io/search"
        var searchURL: URL? = null
        var retVal: String? = null
        var reader: BufferedReader? = null

        CookieHandler.setDefault(cookieManager) // it[0] ??

        try {
            searchURL = URL(radioSearchUrl)
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }

        try {
            reader = BufferedReader(InputStreamReader(searchURL!!.openStream(), "UTF-8"))
            var line: String?
            line = reader.readLine()
            while (line != null)
            {
                line = line.trim { it <= ' ' }
                val p = Pattern.compile("value=\"(\\w+)\"")
                val m = p.matcher(line)

                if (line.startsWith("<form")) {
                    if (m.find()) {
                        retVal = m.group(1)
                        break
                    }
                }
                line = reader.readLine()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (reader != null) try {
                reader.close()
            } catch (ignored: IOException) {
            }

        }
        retVal
    }

    private val postToken : (Any?) -> (Unit) = {
        token = it as String?
    }

    /**
     * Request the song with the CSRF token that was scraped
     */
    private val requestSong: (Any?) -> Any? = {
            val reqString = it as String
            var response = ""

            try {
                val reqURL = URL(reqString)
                val conn = reqURL.openConnection() as HttpsURLConnection
                val tokenObject = JSONObject()

                tokenObject.put("_token", token)
                val requestBytes = tokenObject.toString().toByteArray()

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.setChunkedStreamingMode(0)
                conn.setRequestProperty("Content-Type", "application/json")

                val os = conn.outputStream
                os.write(requestBytes)

                val responseCode = conn.responseCode

                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    var line: String?
                    val br = BufferedReader(InputStreamReader(
                        conn.inputStream))
                    line = br.readLine()
                    while (line != null) {
                        response += line
                        line = br.readLine()
                    }
                } else {
                    response = ""
                }
            } catch (ex: IOException) {
                ex.printStackTrace()
            } catch (ex: JSONException) {
                ex.printStackTrace()
            }

            response
        }

    private val postSong  : (Any?) -> (Unit) = {
            val response = JSONObject(it as String)
            val key = response.names()!!.get(0) as String
            val value = response.getString(key)

            snackBarText.postValue(value)
        }

    fun search(query: String)
    {
        responseArray.clear()
        searchPage(query, 1) // the searchPage function is recursive to get all pages.
    }

    private fun searchPage(query: String, pageNumber : Int)
    {
        val searchURL = String.format(Locale.getDefault(), searchUrl, query, pageNumber)
        val scrape : (Any?) -> JSONObject = {
            val res = URL(searchURL).readText()
            val json = JSONObject(res)
            json
        }
        val post : (Any?) -> Unit = {
            val response = RequestResponse(it as JSONObject)

            Log.d(tag, (response as RequestResponse).toString())
            responseArray.add(response)
            if (response.currentPage < response.lastPage)
                searchPage(query, pageNumber + 1) // recursive call to get the next page
            else
                finishSearch()
        }
        Async(scrape, post, ActionOnError.NOTIFY)
    }

    private fun finishSearch()
    {
        //request(responseArray.first().songs.first().id)
    }

    fun request(songID: Int?) {
        val requestSongUrl = String.format(requestUrl, songID!!)
        if (token == null) {
            Async(scrapeToken, postToken, ActionOnError.NOTIFY)
        }
        Async(requestSong, postSong, ActionOnError.NOTIFY, requestSongUrl)
    }

    companion object {
        val instance by lazy {
            Requestor()
        }
    }

}
