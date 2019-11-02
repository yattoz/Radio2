package io.r_a_d.radio2.ui.chat

import android.os.Bundle
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatViewModel : ViewModel() {

    var webViewBundle: Bundle? = null
    var webView: WebView? = null
    var isChatLoaded = false
    var webViewChat: WebViewChat? = null
}