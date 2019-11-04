package io.r_a_d.radio2

import android.os.AsyncTask
import android.util.Log

class Async(val handler: () -> Any?, val post: (Any?) -> Unit = {}) : AsyncTask<Void, Void, Any>() {
    init {
        try {
            execute()
        } catch (e: Exception)
        {
            Log.d(tag,e.toString())
        }
    }

    override fun doInBackground(vararg params: Void?): Any? {
        try {
            return handler()
        } catch (e: Exception) {
            Log.d(tag,e.toString())
        }
        return null
    }

    override fun onPostExecute(result: Any?) {
        try {
            post(result)
        } catch (e: Exception) {
            Log.d(tag,e.toString())
        }
    }
}