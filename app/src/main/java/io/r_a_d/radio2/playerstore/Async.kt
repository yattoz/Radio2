package io.r_a_d.radio2.playerstore

import android.os.AsyncTask

class Async(val handler: () -> Any?, val post: (Any?) -> Unit = {}) : AsyncTask<Void, Void, Any>() {
    init {
        execute()
    }

    override fun doInBackground(vararg params: Void?): Any? {
        return handler()
    }

    override fun onPostExecute(result: Any?) {
        post(result)
    }
}