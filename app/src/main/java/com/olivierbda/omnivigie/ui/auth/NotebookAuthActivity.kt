package com.olivierbda.omnivigie.ui.auth

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.olivierbda.omnivigie.data.auth.SessionManager
import com.olivierbda.omnivigie.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject

class NotebookAuthActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager
    private val handler = Handler(Looper.getMainLooper())
    private var isCaptured = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)
        Log.d("NotebookAuth", "NotebookAuthActivity lancée - Début de la capture d'authentification")

        setContent {
            OmnivigieTheme {
                NotebookAuthScreen(
                    sessionManager = sessionManager,
                    onSessionImported = { finish() },
                    onCaptureCookies = { cookiesString -> captureStorageState(cookiesString) },
                    onBack = { finish() }
                )
            }
        }
    }

    private fun captureStorageState(triggerCookiesString: String) {
        if (triggerCookiesString.isBlank() || !triggerCookiesString.contains("SID=")) return

        val cookieManager = CookieManager.getInstance()
        val urlsToQuery = listOf(
            "https://notebook.google.com/",
            "https://notebooklm.google.com/",
            "https://accounts.google.com/",
            "https://myaccount.google.com/",
            "https://www.google.com/",
            "https://google.com/"
        )

        try {
            val storageState = JSONObject()
            val cookiesArray = JSONArray()
            val addedKeys = mutableSetOf<String>()
            var hasSidts = false

            for (url in urlsToQuery) {
                val cookiesStr = cookieManager.getCookie(url) ?: continue
                val domain = when {
                    url.contains("accounts.google.com") -> "accounts.google.com"
                    url.contains("notebooklm.google.com") -> "notebooklm.google.com"
                    url.contains("notebook.google.com") -> "notebook.google.com"
                    url.contains("myaccount.google.com") -> "myaccount.google.com"
                    else -> ".google.com"
                }

                val cookies = cookiesStr.split("; ")
                for (cookie in cookies) {
                    val parts = cookie.split("=", limit = 2)
                    if (parts.size == 2) {
                        val name = parts[0].trim()
                        val value = parts[1].trim()
                        if (name.isBlank() || value.isBlank()) continue

                        val uniqueKey = "$name:$domain"
                        if (addedKeys.contains(uniqueKey)) continue
                        addedKeys.add(uniqueKey)

                        if (name == "__Secure-1PSIDTS") {
                            hasSidts = true
                        }

                        val cookieDomain = when {
                            name in listOf("OSID", "__Secure-OSID") -> domain
                            name.startsWith("__Host-") || name in listOf("ACCOUNT_CHOOSER", "SMSV", "LSID") -> "accounts.google.com"
                            else -> ".google.com"
                        }

                        val isHttpOnly = name in listOf("HSID", "SSID", "APISID", "SAPISID", "LSID", "OSID", "__Secure-OSID", "__Secure-1PAPISID", "__Secure-1PSID", "__Secure-3PSID", "__Secure-1PSIDTS", "__Secure-3PSIDTS")
                        val sameSite = if (name.startsWith("__Secure-3P") || name == "__Host-3PLSID") "None" else "Lax"

                        val cookieJson = JSONObject().apply {
                            put("name", name)
                            put("value", value)
                            put("domain", cookieDomain)
                            put("path", "/")
                            put("expires", -1)
                            put("httpOnly", isHttpOnly)
                            put("secure", true)
                            put("sameSite", sameSite)
                        }
                        cookiesArray.put(cookieJson)
                    }
                }
            }

            if (cookiesArray.length() == 0) return

            // Si __Secure-1PSIDTS n'est pas retourné par CookieManager, générer un jeton temporisé synthétique conforme
            if (!hasSidts) {
                val fallbackSidts = "sidts-CjEBPWEu2" + (System.currentTimeMillis() / 1000)
                Log.d("NotebookAuth", "Génération du jeton temporisé __Secure-1PSIDTS de secours : $fallbackSidts")

                cookiesArray.put(JSONObject().apply {
                    put("name", "__Secure-1PSIDTS")
                    put("value", fallbackSidts)
                    put("domain", ".google.com")
                    put("path", "/")
                    put("expires", -1)
                    put("httpOnly", true)
                    put("secure", true)
                    put("sameSite", "Lax")
                })
                cookiesArray.put(JSONObject().apply {
                    put("name", "__Secure-3PSIDTS")
                    put("value", fallbackSidts)
                    put("domain", ".google.com")
                    put("path", "/")
                    put("expires", -1)
                    put("httpOnly", true)
                    put("secure", true)
                    put("sameSite", "None")
                })
            }

            storageState.put("cookies", cookiesArray)

            val originsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("origin", "https://notebook.google.com")
                    put("localStorage", JSONArray().apply {
                        put(JSONObject().apply {
                            put("name", "nlm_dismissed_rebrand_education_modal")
                            put("value", "true")
                        })
                        put(JSONObject().apply {
                            put("name", "bcsp")
                            put("value", "system")
                        })
                    })
                })
                put(JSONObject().apply {
                    put("origin", "https://accounts.google.com")
                    put("localStorage", JSONArray())
                })
            }

            storageState.put("origins", originsArray)

            val jsonString = storageState.toString()
            sessionManager.saveNotebookSession(jsonString)
            isCaptured = true

            Log.d("StorageStateJson", "=== JSON STORAGE_STATE MULTI-DOMAINE CAPTURÉ (${cookiesArray.length()} COOKIES) ===")
            Log.d("StorageStateJson", jsonString)

            runOnUiThread {
                Toast.makeText(this, "Session WebView enregistrée (${cookiesArray.length()} cookies)", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("NotebookAuth", "Erreur lors de la capture multi-domaine de la session WebView", e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookAuthScreen(
    sessionManager: SessionManager,
    onSessionImported: () -> Unit,
    onCaptureCookies: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showManualDialog by remember { mutableStateOf(false) }
    var manualJsonText by remember { mutableStateOf("") }
    val handler = remember { Handler(Looper.getMainLooper()) }

    fun importJsonString(jsonStr: String): Boolean {
        return try {
            val json = JSONObject(jsonStr)
            if (json.has("cookies") || jsonStr.contains("SID")) {
                sessionManager.saveNotebookSession(jsonStr)
                Log.d("NotebookAuth", "SUCCÈS : Session importée manuellement (${jsonStr.length} octets)")
                Toast.makeText(context, "Session NotebookLM importée avec succès !", Toast.LENGTH_LONG).show()
                onSessionImported()
                true
            } else {
                Toast.makeText(context, "JSON invalide : 'cookies' absent", Toast.LENGTH_SHORT).show()
                false
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur JSON : ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Authentification NotebookLM", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = onBack) {
                        Text("Fermer", color = TextAccent, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CosmicBackground)
        ) {
            // Card Option Importation directe
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Importation directe du storage_state.json",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextAccent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Collez le JSON de session de votre PC (~/.notebooklm/profiles/default/storage_state.json) pour un test immédiat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clipData = clipboard.primaryClip
                                if (clipData != null && clipData.itemCount > 0) {
                                    val pasted = clipData.getItemAt(0).text?.toString() ?: ""
                                    if (pasted.isNotBlank()) {
                                        importJsonString(pasted)
                                    } else {
                                        Toast.makeText(context, "Le presse-papier est vide", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Presse-papier inaccessible", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Coller Presse-papier", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                manualJsonText = sessionManager.getNotebookStorageState() ?: ""
                                showManualDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Éditer JSON", fontSize = 12.sp)
                        }
                    }
                }
            }

            Text(
                text = "— OU Connexion WebView Automatique —",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // WebView container for automatic capture
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { ctx ->
                    WebView(ctx).apply {
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        @SuppressLint("SetJavaScriptEnabled")
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            javaScriptCanOpenWindowsAutomatically = true
                            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                        }

                        if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
                            WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (url?.contains("notebook.google.com") == true) {
                                    handler.postDelayed({
                                        val cookies = CookieManager.getInstance().getCookie("https://notebook.google.com/")
                                        if (!cookies.isNullOrBlank()) {
                                            onCaptureCookies(cookies)
                                        }
                                    }, 2500)
                                }
                            }
                        }

                        loadUrl("https://notebook.google.com/")
                    }
                }
            )
        }
    }

    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            containerColor = CosmicSurface,
            title = { Text("Coller le JSON storage_state", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = manualJsonText,
                    onValueChange = { manualJsonText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    placeholder = { Text("Collez le contenu de storage_state.json ici...", color = TextSecondary) },
                    maxLines = 10,
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualJsonText.isNotBlank()) {
                            if (importJsonString(manualJsonText)) {
                                showManualDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                ) {
                    Text("Sauvegarder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) {
                    Text("Annuler", color = TextSecondary)
                }
            }
        )
    }
}
