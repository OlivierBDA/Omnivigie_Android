package com.olivierbda.omnivigie.ui.auth

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.olivierbda.omnivigie.data.auth.SessionManager
import org.json.JSONArray
import org.json.JSONObject

class NotebookAuthActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var sessionManager: SessionManager

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        webView = WebView(this)

        // 1. Enable Cookie Manager for Google account sign-in
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        // 2. Configure WebView settings with a clean Mobile Chrome User Agent
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            javaScriptCanOpenWindowsAutomatically = true
            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        }

        // 3. Remove X-Requested-With header to bypass Google accounts disallowed WebView security block
        if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
            WebSettingsCompat.setRequestedWithHeaderOriginAllowList(webView.settings, emptySet())
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url?.contains("notebooklm.google.com") == true) {
                    captureStorageState()
                }
            }
        }

        val notebookUrl = intent.getStringExtra("NOTEBOOK_URL")
            ?: intent.getStringExtra("NOTEBOOK_ID")?.let { "https://notebooklm.google.com/notebook/$it" }
            ?: "https://notebooklm.google.com/"

        webView.loadUrl(notebookUrl)
        setContentView(webView)
    }

    private fun captureStorageState() {
        val cookieManager = CookieManager.getInstance()
        val url = "https://notebooklm.google.com/"
        val cookiesString = cookieManager.getCookie(url) ?: return

        if (!cookiesString.contains("SID=")) return

        try {
            val storageState = JSONObject()
            val cookiesArray = JSONArray()

            val cookies = cookiesString.split("; ")
            for (cookie in cookies) {
                val parts = cookie.split("=", limit = 2)
                if (parts.size == 2) {
                    val cookieJson = JSONObject()
                    cookieJson.put("name", parts[0])
                    cookieJson.put("value", parts[1])
                    cookieJson.put("domain", ".google.com")
                    cookieJson.put("path", "/")
                    cookieJson.put("expires", -1)
                    cookieJson.put("httpOnly", false)
                    cookieJson.put("secure", true)
                    cookieJson.put("sameSite", "Lax")
                    cookiesArray.put(cookieJson)
                }
            }

            storageState.put("cookies", cookiesArray)
            storageState.put("origins", JSONArray())

            sessionManager.saveNotebookSession(storageState.toString())

            runOnUiThread {
                Toast.makeText(this, "Session NotebookLM capturée (format Playwright)", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
