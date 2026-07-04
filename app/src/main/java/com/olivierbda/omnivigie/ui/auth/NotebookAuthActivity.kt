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
        webView.settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        webView.addJavascriptInterface(MyJavaScriptInterface(), "HTMLViewer")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url?.contains("notebooklm.google.com") == true) {
                    // Inject JS to get HTML content
                    webView.loadUrl("javascript:window.HTMLViewer.showHTML" +
                            "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');")
                }
            }
        }

        webView.loadUrl("https://notebooklm.google.com/")
        setContentView(webView)
    }

    inner class MyJavaScriptInterface {
        @JavascriptInterface
        fun showHTML(html: String) {
            val snlm0e = extractToken(html, "SNlM0e")
            val fdrfje = extractToken(html, "FdrFJe")

            if (snlm0e != null && fdrfje != null) {
                val cookies = CookieManager.getInstance().getCookie("https://notebooklm.google.com/")
                if (cookies != null && cookies.contains("SID=")) {
                    sessionManager.saveNotebookSession(cookies, snlm0e, fdrfje)
                    runOnUiThread {
                        Toast.makeText(this@NotebookAuthActivity, "Connexion NotebookLM réussie !", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }

        private fun extractToken(html: String, tokenName: String): String? {
            val pattern = Pattern.compile("\"$tokenName\":\"([^\"]+)\"")
            val matcher = pattern.matcher(html)
            return if (matcher.find()) matcher.group(1) else null
        }
    }
}
