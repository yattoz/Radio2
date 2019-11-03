package io.r_a_d.radio2.ui.chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import io.r_a_d.radio2.R


class ChatFragment : Fragment() {

    private lateinit var chatViewModel: ChatViewModel


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        chatViewModel =
                ViewModelProviders.of(this).get(ChatViewModel::class.java)

        if (!chatViewModel.isChatLoaded)
        {
            chatViewModel.root = inflater.inflate(R.layout.fragment_chat, container, false)
            chatViewModel.webView = chatViewModel.root.findViewById<WebView>(R.id.chat_webview)
            chatViewModel.webViewChat = WebViewChat(chatViewModel.webView as WebView)
            chatViewModel.webViewChat!!.start()
            chatViewModel.isChatLoaded = true
            Log.d(tag, "webview created")
        } else {
            Log.d(tag, "webview already created!?")
        }

        return chatViewModel.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }


}