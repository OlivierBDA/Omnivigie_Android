package com.olivierbda.omnivigie.ui.auth

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.olivierbda.omnivigie.data.auth.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

class NotebookAuthActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var sessionManager: SessionManager

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url?.contains("notebooklm.google.com") == true) {
                    captureStorageState()
                }
            }
        }

        webView.loadUrl("https://notebooklm.google.com/")
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
