package me.rxresu.app

import android.annotation.SuppressLint
import android.app.Activity
import android.net.ConnectivityManager
import android.os.Build
import android.graphics.Bitmap
import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Handler
import android.os.Message
import android.view.View
import android.webkit.*
import androidx.annotation.RequiresApi
import java.lang.Exception

class WebViewHelper(
    private val activity: Activity,
    private val uiManager: UIManager
) {
    private val webView: WebView
    private val webSettings: WebSettings

    @RequiresApi(Build.VERSION_CODES.M)
    fun isNetworkAvailable(context: Context) =
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).run {
            getNetworkCapabilities(activeNetwork)?.run {
                hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            } ?: false
        }

    // manipulate cache settings to make sure our PWA gets updated
    private fun useCache(use: Boolean) {
        if (use) {
            webSettings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        } else {
            webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        }
    }

    // public method changing cache settings according to network availability.
    // retrieve content from cache primarily if not connected,
    // allow fetching from web too otherwise to get updates.
    fun forceCacheIfOffline() {
        useCache(!isNetworkAvailable())
    }

    // handles initial setup of webview
    @SuppressLint("SetJavaScriptEnabled")
    fun setupWebView() {
        // accept cookies
        CookieManager.getInstance().setAcceptCookie(true)
        // enable JS
        webSettings.javaScriptEnabled = true
        // must be set for our js-popup-blocker:
        webSettings.setSupportMultipleWindows(true)

        // PWA settings
        webSettings.domStorageEnabled = true
        webSettings.setAppCachePath(activity.applicationContext.cacheDir.absolutePath)
        webSettings.setAppCacheEnabled(true)
        webSettings.databaseEnabled = true

        // enable mixed content mode conditionally
        if (Constants.ENABLE_MIXED_CONTENT
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        ) {
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        // retrieve content from cache primarily if not connected
        forceCacheIfOffline()

        // set User Agent
        if (Constants.OVERRIDE_USER_AGENT || Constants.POSTFIX_USER_AGENT) {
            var userAgent = webSettings.userAgentString
            if (Constants.OVERRIDE_USER_AGENT) {
                userAgent = Constants.USER_AGENT
            }
            if (Constants.POSTFIX_USER_AGENT) {
                userAgent = userAgent + " " + Constants.USER_AGENT_POSTFIX
            }
            webSettings.setUserAgentString(userAgent)
        }

        // enable HTML5-support
        webView.webChromeClient = object : WebChromeClient() {
            //simple yet effective redirect/popup blocker
            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {
                val href = view.handler.obtainMessage()
                view.requestFocusNodeHref(href)
                val popupUrl = href.data.getString("url")
                if (popupUrl != null) {
                    //it's null for most rouge browser hijack ads
                    webView.loadUrl(popupUrl)
                    return true
                }
                return false
            }

            // update ProgressBar
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                uiManager.setLoadingProgress(newProgress)
                super.onProgressChanged(view, newProgress)
            }
        }

        // Set up Webview client
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap) {
                super.onPageStarted(view, url, favicon)
                handleUrlLoad(view, url)
            }

            // handle loading error by showing the offline screen
            @Deprecated("")
            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String,
                failingUrl: String
            ) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    handleLoadError(errorCode)
                }
            }

            @TargetApi(Build.VERSION_CODES.M)
            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // new API method calls this on every error for each resource.
                    // we only want to interfere if the page itself got problems.
                    val url = request.url.toString()
                    if (view.url == url) {
                        handleLoadError(error.errorCode)
                    }
                }
            }
        }
    }

    // Lifecycle callbacks
    fun onPause() {
        webView.onPause()
    }

    fun onResume() {
        webView.onResume()
    }

    // show "no app found" dialog
    private fun showNoAppDialog(thisActivity: Activity) {
        AlertDialog.Builder(thisActivity)
            .setTitle(R.string.noapp_heading)
            .setMessage(R.string.noapp_description)
            .show()
    }

    // handle load errors
    private fun handleLoadError(errorCode: Int) {
        if (errorCode != WebViewClient.ERROR_UNSUPPORTED_SCHEME) {
            uiManager.setOffline(true)
        } else {
            // Unsupported Scheme, recover
            Handler().postDelayed({ goBack() }, 100)
        }
    }

    // handle external urls
    private fun handleUrlLoad(view: WebView, url: String): Boolean {
        // prevent loading content that isn't ours
        return if (!url.startsWith(Constants.WEBAPP_URL)) {
            // stop loading
            // stopping only would cause the PWA to freeze, need to reload the app as a workaround
            view.stopLoading()
            view.reload()

            // open external URL in Browser/3rd party apps instead
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                if (intent.resolveActivity(activity.packageManager) != null) {
                    activity.startActivity(intent)
                } else {
                    showNoAppDialog(activity)
                }
            } catch (e: Exception) {
                showNoAppDialog(activity)
            }
            // return value for shouldOverrideUrlLoading
            true
        } else {
            // let WebView load the page!
            // activate loading animation screen
            uiManager.setLoading(true)
            // return value for shouldOverrideUrlLoading
            false
        }
    }

    // handle back button press
    fun goBack(): Boolean {
        if (webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return false
    }

    // load app startpage
    fun loadHome() {
        webView.loadUrl(Constants.WEBAPP_URL)
    }

    // load URL from intent
    fun loadIntentUrl(url: String) {
        if (url != "" && url.contains(Constants.WEBAPP_HOST)) {
            webView.loadUrl(url)
        } else {
            // Fallback
            loadHome()
        }
    }

    init {
        webView = activity.findViewById<View>(R.id.webView) as WebView
        webSettings = webView.settings
    }
}
