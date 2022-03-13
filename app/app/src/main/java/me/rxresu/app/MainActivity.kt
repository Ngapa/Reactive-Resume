package me.rxresu.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private var uiManager: UIManager? = null
    private var webViewHelper: WebViewHelper? = null
    private var intentHandled = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.Theme_ReactiveResume_NoActionBar)
        setContentView(R.layout.activity_main)

        // Setup Helpers
        uiManager = UIManager(this)
        webViewHelper = WebViewHelper(this, uiManager)

        // Setup App
        webViewHelper.setupWebView()
        uiManager.changeRecentAppsIcon()

        // Check for Intents
        try {
            val i = intent
            val intentAction = i.action
            // Handle URLs opened in Browser
            if (!intentHandled && intentAction != null && intentAction == Intent.ACTION_VIEW) {
                val intentUri = i.data
                var intentText = ""
                if (intentUri != null) {
                    intentText = intentUri.toString()
                }
                // Load up the URL specified in the Intent
                if (intentText != "") {
                    intentHandled = true
                    webViewHelper.loadIntentUrl(intentText)
                }
            } else {
                // Load up the Web App
                webViewHelper.loadHome()
            }
        } catch (e: Exception) {
            // Load up the Web App
            webViewHelper.loadHome()
        }
    }

    override fun onPause() {
        webViewHelper.onPause()

        super.onPause()
    }

    override fun onResume() {
        webViewHelper.onResume()
        // retrieve content from cache primarily if not connected
        webViewHelper.forceCacheIfOffline()

        super.onResume()
    }

    // Handle back-press in browser
    override fun onBackPressed() {
        if (!webViewHelper.goBack()) {
            super.onBackPressed()
        }
    }
}
