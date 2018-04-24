package soko.ekibun.bangumi.view

import android.content.Context
import android.os.Handler
import android.util.Log
import android.webkit.*

class BackgroundWebView(context: Context): WebView(context) {
    var onPageFinished = {_:String?->}
    var onCatchVideo={_: WebResourceRequest ->}

    var uiHandler: Handler = Handler{true}

    init{
        settings.javaScriptEnabled = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                onPageFinished(url)
                super.onPageFinished(view, url)
            }
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (request.requestHeaders["Range"] != null) {
                    onCatchVideo(request)
                    uiHandler.post{
                        view.onPause()
                        view.clearCache(true)
                        view.clearHistory()
                        view.loadUrl("about:blank")
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }
        }
    }
}
