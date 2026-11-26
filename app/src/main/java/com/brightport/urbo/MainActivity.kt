package com.brightport.urbo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader

/** URBO: the national portal of Brightport and Occasue, packed into one app. */
class MainActivity : Activity() {
    private lateinit var web: WebView
    private val home = "https://appassets.androidplatform.net/assets/www/index.html"
    private val repo = "BBGINN77/urbo"
    // A downloaded update waiting for the one-time "install unknown apps" permission
    private var pendingUpdate: File? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val assets = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
        web = WebView(this).apply {
            setBackgroundColor(Color.parseColor("#f6f2e9"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            isFocusable = true
            isFocusableInTouchMode = true
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                    assets.shouldInterceptRequest(request.url)

                // Links to other websites open in the browser; everything else stays in the app
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val u = request.url
                    if (u.host == "appassets.androidplatform.net") return false
                    return try { startActivity(Intent(Intent.ACTION_VIEW, u)); true } catch (e: Exception) { true }
                }
            }
        }
        web.addJavascriptInterface(Bridge(), "UrboNative")
        setContentView(web)
        web.requestFocus(View.FOCUS_DOWN)
        if (savedInstanceState != null) web.restoreState(savedInstanceState) else web.loadUrl(home)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    // BACK goes to the previous page; on the home page it leaves the app
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && web.canGoBack()) { web.goBack(); return true }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() { super.onPause(); web.onPause() }
    override fun onResume() {
        super.onResume(); web.onResume()
        // Back from the permission screen: install the update that was waiting
        pendingUpdate?.let { if (packageManager.canRequestPackageInstalls()) { pendingUpdate = null; openInstaller(it) } }
    }

    private fun js(code: String) = runOnUiThread { web.evaluateJavascript("window.urboUpdate&&urboUpdate.$code", null) }
    private fun q(s: String) = JSONObject.quote(s)

    private fun build(): Int = try {
        @Suppress("DEPRECATION") (packageManager.getPackageInfo(packageName, 0).versionCode - 1)
    } catch (e: Exception) { 0 }

    private fun openInstaller(apk: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.files", apk)
        val i = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try { startActivity(i); js("state('installing')") } catch (e: Exception) { js("state('error')") }
    }

    /** What the web pages can ask the app to do. */
    inner class Bridge {
        @JavascriptInterface fun appBuild(): Int = build()

        /** Finds the newest URBO build. It follows GitHub's "latest release" link, which has no request limit,
         *  and falls back to the GitHub API. */
        @JavascriptInterface fun check() {
            Thread {
                var latest = 0; var why = ""
                try {
                    val c = URL("https://github.com/$repo/releases/latest").openConnection() as HttpURLConnection
                    c.instanceFollowRedirects = false
                    c.setRequestProperty("User-Agent", "URBO-Updater")
                    c.connectTimeout = 15000; c.readTimeout = 15000
                    val loc = c.getHeaderField("Location") ?: ""
                    c.disconnect()
                    latest = Regex("build-(\\d+)").find(loc)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    if (latest == 0) why = "no release link (HTTP ${c.responseCode})"
                } catch (e: Exception) { why = e.javaClass.simpleName + ": " + (e.message ?: "") }
                if (latest == 0) try {
                    val c = URL("https://api.github.com/repos/$repo/releases/latest").openConnection() as HttpURLConnection
                    c.setRequestProperty("Accept", "application/vnd.github+json")
                    c.setRequestProperty("User-Agent", "URBO-Updater")
                    c.connectTimeout = 15000; c.readTimeout = 15000
                    val j = JSONObject(c.inputStream.bufferedReader().readText()); c.disconnect()
                    latest = j.optString("tag_name").removePrefix("build-").toIntOrNull() ?: 0
                } catch (e: Exception) { why += " / " + e.javaClass.simpleName + ": " + (e.message ?: "") }
                val url = "https://github.com/$repo/releases/latest/download/urbo.apk"
                if (latest == 0) js("fail(${q(why)})")
                else if (latest > build()) js("found($latest,${build()},${q(url)})") else js("state('current')")
            }.start()
        }

        /** Downloads the new APK and opens Android's installer for it. */
        @JavascriptInterface fun install(url: String) {
            Thread {
                try {
                    val out = File(cacheDir, "urbo-update.apk")
                    val c = URL(url).openConnection() as HttpURLConnection
                    c.instanceFollowRedirects = true; c.connectTimeout = 20000; c.readTimeout = 60000
                    c.setRequestProperty("User-Agent", "URBO-Updater")
                    val total = c.contentLengthLong; var done = 0L; var last = -1
                    c.inputStream.use { input -> out.outputStream().use { o ->
                        val buf = ByteArray(64 * 1024)
                        while (true) { val n = input.read(buf); if (n < 0) break; o.write(buf, 0, n); done += n
                            if (total > 0) { val pct = (done * 100 / total).toInt(); if (pct != last) { last = pct; js("progress($pct)") } } }
                    } }
                    c.disconnect()
                    runOnUiThread {
                        if (!packageManager.canRequestPackageInstalls()) {
                            pendingUpdate = out; js("state('permission')")
                            try { startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            catch (e: Exception) { js("state('error')") }
                        } else openInstaller(out)
                    }
                } catch (e: Exception) { js("state('error')") }
            }.start()
        }
    }
}
