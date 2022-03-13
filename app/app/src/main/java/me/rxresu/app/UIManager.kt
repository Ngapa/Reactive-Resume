package me.rxresu.app

import android.app.Activity
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.LinearLayout
import android.os.Build
import android.view.animation.AccelerateInterpolator
import android.graphics.BitmapFactory
import android.util.TypedValue
import android.app.ActivityManager.TaskDescription
import android.view.View
import android.view.animation.DecelerateInterpolator

class UIManager(private val activity: Activity) {

    private val webView: WebView
    private val progressSpinner: ProgressBar
    private val progressBar: ProgressBar
    private val offlineContainer: LinearLayout

    private var pageLoaded = false

    // Set Loading Progress for ProgressBar
    private fun setLoadingProgress(progress: Int) {
        // set progress in UI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            progressBar.setProgress(progress, true)
        } else {
            progressBar.progress = progress
        }

        // hide ProgressBar if not applicable
        if (progress in 0..99) {
            progressBar.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.INVISIBLE
        }

        // get app screen back if loading is almost complete
        if (progress >= Constants.PROGRESS_THRESHOLD && !pageLoaded) {
            setLoading(false)
        }
    }

    // Show loading animation screen while app is loading/caching the first time
    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            progressSpinner.visibility = View.VISIBLE
            webView.animate().translationX(Constants.SLIDE_EFFECT).alpha(0.5f)
                .setInterpolator(AccelerateInterpolator()).start()
        } else {
            webView.translationX = Constants.SLIDE_EFFECT * -1
            webView.animate().translationX(0f).alpha(1f).setInterpolator(DecelerateInterpolator())
                .start()
            progressSpinner.visibility = View.INVISIBLE
        }
        pageLoaded = !isLoading
    }

    // handle visibility of offline screen
    private fun setOffline(offline: Boolean) {
        if (offline) {
            setLoadingProgress(100)
            webView.visibility = View.INVISIBLE
            offlineContainer.visibility = View.VISIBLE
        } else {
            webView.visibility = View.VISIBLE
            offlineContainer.visibility = View.INVISIBLE
        }
    }

    init {
        progressBar = activity.findViewById<View>(R.id.progressBarBottom) as ProgressBar
        progressSpinner = activity.findViewById<View>(R.id.progressSpinner) as ProgressBar
        offlineContainer = activity.findViewById<View>(R.id.offlineContainer) as LinearLayout
        webView = activity.findViewById<View>(R.id.webView) as WebView

        // set click listener for offline-screen
        offlineContainer.setOnClickListener {
            webView.loadUrl(Constants.WEBAPP_URL)
            setOffline(false)
        }
    }
}
