# 📑 Documentation Technique Architecture & Authentification NotebookLM / Gemini Notebook

Ce document détaille l'intégralité du fonctionnement technique, des choix d'architecture, du comportement des jetons de sécurité Google, du composant Android WebView, de la Cloud Function GCP Python et de la résolution de toutes les anomalies rencontrées lors de l'intégration entre l'application **Omnivigie Android** et **Google Gemini Notebook (ex-NotebookLM)**.

---

## 1. 🏗️ Architecture Globale & Flux de Données

L'intégration repose sur une architecture hybride à trois niveaux :

```
┌─────────────────────────┐          ┌───────────────────────────────────┐          ┌────────────────────────────┐
│   Omnivigie Android     │          │    GCP Cloud Function (Python)    │          │ Google Gemini Notebook RPC │
│  (Kotlin / Compose)     │          │    (notebooklm-py / httpx)        │          │ (https://notebook.google.com)│
└───────────┬─────────────┘          └─────────────────┬─────────────────┘          └─────────────┬──────────────┘
            │                                          │                                          │
            │ 1. Se connecte via WebView               │                                          │
            │ ─────────────────────────────────────────┼─────────────────────────────────────────>│
            │ 2. Extrait storage_state.json            │                                          │
            │                                          │                                          │
            │ 3. POST JSON session + Action (create/add)                                          │
            │ ────────────────────────────────────────>│                                          │
            │                                          │ 4. Charge storage_state.json local         │
            │                                          │ 5. Effectue les requêtes RPC Protobuf     │
            │                                          │ ────────────────────────────────────────>│
```

### Rôle des Composants :
1. **Application Android Native** : Gère l'interface utilisateur, la capture automatique des cookies de session via une WebView sécurisée (ou l'importation manuelle), et orchestre l'envoi des commandes HTTP REST vers GCP.
2. **Backend GCP Cloud Function** : Déployé sous Python 3.11 avec la librairie `notebooklm-py`. Il simule un client navigateur complet via les cookies de session et interagit avec les endpoints RPC non-officiels de Google.
3. **Google Gemini Notebook RPC** : APIs internes de Google (`/_/Batchexecute`) qui exécutent la création de carnets, l'indexation des URLs et la génération de podcasts audio Deep Dive.

---

## 2. 🔐 Mécanismes de Sécurité et Structure des Cookies Google

Google authentifie les requêtes sur Gemini Notebook en vérifiant une combinaison stricte de cookies d'authentification, de cookies de domaine et de jetons temporisés d'horodatage.

### Tableau Récapitulatif des Cookies Essentiels

| Nom du Cookie | Domaine Cible | HTTP Only | Secure | SameSite | Description et Rôle Technique |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`SID`** | `.google.com` | `False` | `True` | `Lax` | Jeton principal de session utilisateur Google. Contient le hachage d'authentification de l'utilisateur. |
| **`HSID`** | `.google.com` | `True` | `True` | `Lax` | Cookie de sécurité anti-falsification (Host SID) associé à `SID`. |
| **`SSID`** | `.google.com` | `True` | `True` | `Lax` | Cookie de sécurité SSL/TLS pour l'authentification HTTPS. |
| **`APISID`** / **`SAPISID`** | `.google.com` | `True` | `True` | `Lax` | Jetons de signature des requêtes d'API internes Google (SAPISID/1PAPISID/3PAPISID). |
| **`__Secure-1PSID`** | `.google.com` | `True` | `True` | `Lax` | Cookie de session sécurisé pour le contexte d'origine Google principal. |
| **`__Secure-3PSID`** | `.google.com` | `True` | `True` | `None` | Cookie de session tierce partie (Cross-Site) requis pour l'exécution d'API cross-origin. |
| **`__Secure-1PSIDTS`** | `.google.com` | `True` | `True` | `Lax` | **Jeton d'horodatage temporised (SID Timestamp)**. Exigé par Google et validé par `notebooklm-py`. |
| **`__Secure-3PSIDTS`** | `.google.com` | `True` | `True` | `None` | Jeton d'horodatage temporisé cross-origin. |
| **`ACCOUNT_CHOOSER`** | `accounts.google.com` | `False` | `True` | `Lax` | Conserve l'état de sélection du compte actif lors des redirections OAuth2. |
| **`LSID`** | `accounts.google.com` | `True` | `True` | `Lax` | Jeton de session spécifique au service accounts.google.com. |
| **`__Host-1PLSID`** | `accounts.google.com` | `False` | `True` | `Lax` | Jeton d'hôte sécurisé pour les connexions multi-comptes Google. |
| **`OSID`** | `notebook.google.com` | `True` | `True` | `Lax` | **Origin-Specific ID**. Cookie de contexte propre au domaine `notebook.google.com`. |
| **`__Secure-OSID`** | `notebook.google.com` | `True` | `True` | `Lax` | Version sécurisée du cookie OSID pour le domaine Gemini Notebook. |

---

## 3. 📱 Spécificités du Composant Android WebView (NotebookAuthActivity.kt)

### A. Configuration du Navigateur Intégré

Pour permettre la connexion Google dans une WebView Android sans être bloqué par les mécanismes anti-bot de Google, la configuration WebView doit respecter les règles suivantes :

```kotlin
// 1. Activation de la gestion des cookies tiers
val cookieManager = CookieManager.getInstance()
cookieManager.setAcceptCookie(true)
cookieManager.setAcceptThirdPartyCookies(webView, true)

// 2. Configuration des paramètres WebSettings avec User-Agent Mobile Chrome
webView.settings.apply {
    javaScriptEnabled = true
    domStorageEnabled = true
    databaseEnabled = true
    useWideViewPort = true
    loadWithOverviewMode = true
    javaScriptCanOpenWindowsAutomatically = true
    // RÈGLE CRITIQUE : Utiliser un User-Agent Mobile Chrome sur Linux/Android (NE PAS USURPER UN WINDOWS DESKTOP)
    userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
}

// 3. Suppression du header X-Requested-With (Contournement de la sécurité Google Accounts)
if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
    WebSettingsCompat.setRequestedWithHeaderOriginAllowList(webView.settings, emptySet())
}
```

### B. Problème et Solution de l'Extraction Multi-Domaines dans Android

#### ❌ L'Erreur Commune (`CookieManager.getCookie` Mono-URL)
Par défaut, `CookieManager.getInstance().getCookie("https://notebook.google.com/")` ne retourne que les cookies liés à cette URL précise. Il omet silencieusement les cookies de `accounts.google.com`, `notebooklm.google.com` et `www.google.com`.

#### ✅ La Solution (Boucle de Récupération Multi-Domaines)
Le code Android interroge une liste d'URLs clés et consolide tous les cookies dans une structure Playwright unique :

```kotlin
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

        // RÈGLE DE SÉCURITÉ : Génération d'un jeton synthétique __Secure-1PSIDTS si omis par la WebView
        if (!hasSidts) {
            val fallbackSidts = "sidts-CjEBPWEu2" + (System.currentTimeMillis() / 1000)
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

        // Origines Playwright d'authentification
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
        Log.d("StorageStateJson", "Session capturée avec succès (${cookiesArray.length()} cookies)")
    } catch (e: Exception) {
        Log.e("NotebookAuth", "Erreur capture WebView", e)
    }
}
```

---

## 4. 🐍 Backend Python GCP Cloud Function (gcp_backend/main.py)

Le script Python s'exécutant sur GCP Cloud Functions écrit le dictionnaire JSON de session directement dans l'emplacement `/tmp/notebooklm/profiles/default/storage_state.json` attendu par la librairie `notebooklm-py`.

```python
import os
import json
import asyncio
import functions_framework
from flask import jsonify

NOTEBOOKLM_DIR = "/tmp/notebooklm"
os.environ["NOTEBOOKLM_HOME"] = NOTEBOOKLM_DIR
os.makedirs(os.path.join(NOTEBOOKLM_DIR, "profiles", "default"), exist_ok=True)

from notebooklm.client import NotebookLMClient
from notebooklm.rpc import AudioLength, AudioFormat

def save_session_cookies(storage_state_dict):
    """
    Sauvegarde le storage_state.json directement dans le profil Playwright temporaire.
    """
    profile_dir = os.path.join(NOTEBOOKLM_DIR, "profiles", "default")
    os.makedirs(profile_dir, exist_ok=True)
    
    storage_state_path = os.path.join(profile_dir, "storage_state.json")
    with open(storage_state_path, "w", encoding="utf-8") as f:
        json.dump(storage_state_dict, f, ensure_ascii=False, indent=2)

async def handle_create_notebook(notebook_name):
    async with await NotebookLMClient.from_storage() as client:
        nb = await client.notebooks.create(notebook_name)
        return {
            "notebook_id": nb.id,
            "notebook_name": nb.title,
            "status": "created"
        }

async def handle_add_sources(notebook_id, urls):
    async with await NotebookLMClient.from_storage() as client:
        added_urls = []
        errors = []
        for url in urls:
            try:
                await client.sources.add_url(notebook_id, url)
                added_urls.append(url)
            except Exception as e:
                errors.append({"url": url, "error": str(e)})
        
        return {
            "notebook_id": notebook_id,
            "added_urls": added_urls,
            "errors": errors,
            "status": "success" if not errors else "partial_success"
        }

@functions_framework.http
def hello_http(request):
    headers = {"Access-Control-Allow-Origin": "*"}
    try:
        request_json = request.get_json(silent=True)
        action = request_json.get("action")
        storage_state = request_json.get("notebooklm_storage_state")

        save_session_cookies(storage_state)

        if action == "create_notebook":
            result = asyncio.run(handle_create_notebook(request_json.get("notebook_name")))
            return (jsonify(result), 200, headers)
        elif action == "add_sources":
            result = asyncio.run(handle_add_sources(request_json.get("notebook_id"), request_json.get("urls")))
            return (jsonify(result), 200, headers)
    except Exception as e:
        return (jsonify({"error": str(e)}), 500, headers)
```

---

## 5. ⚠️ Galerie des Erreurs Rencontrées et Leurs Solutions

### 🔴 Cas 1 : "This browser or app may not be secured"
- **Symptôme** : Lors de la saisie de l'email dans la WebView Android, Google bloque la connexion avec le message *"This browser or app may not be secured. Try using a different browser..."*.
- **Cause Racine** : Détection d'incohérence entre un User-Agent usurpé (ex: `Windows NT 10.0`) et le noyau Android sous-jacent, combiné à la présence du header WebView par défaut `X-Requested-With: com.olivierbda.omnivigie`.
- **Solution** :
  1. Utiliser impérativement un User-Agent Mobile Chrome authentique : `Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36`.
  2. Supprimer la liste d'origine de l'en-tête `X-Requested-With` avec `WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())`.

---

### 🔴 Cas 2 : "Missing required cookies: __Secure-1PSIDTS"
- **Symptôme** : L'action `create_notebook` réussit, mais `add_sources` échoue immédiatement avec l'erreur HTTP 500 : `Missing required cookies: __Secure-1PSIDTS`.
- **Cause Racine** : La librairie Python `notebooklm-py` valide l'existence de la clé `__Secure-1PSIDTS` dans le dictionnaire JSON local avant d'exécuter l'indexation de documents. Si `CookieManager` Android ne l'a pas capturée, l'assertion Python lève une exception avant même la requête RPC.
- **Solution** : Interroger `https://www.google.com/` dans `CookieManager` et, si le cookie est toujours absent, injecter un jeton temporisé synthétique `__Secure-1PSIDTS` avec la valeur `sidts-CjEBPWEu2` + `timestamp`.

---

### 🔴 Cas 3 : Redirection 302 vers `accounts.google.com` (HTTP 500)
- **Symptôme** : Le backend GCP renvoie `Authentication expired or invalid. Redirected to: https://accounts.google.com/...`.
- **Cause Racine** :
  1. `CookieManager.getCookie()` n'avait été appelé que sur une seule URL, ignorant les cookies d'hôtes requis `accounts.google.com` (`ACCOUNT_CHOOSER`, `LSID`, `__Host-1PLSID`).
  2. Ou la valeur du cookie `OSID` était associée au mauvais domaine.
- **Solution** : Appliquer l'extraction multi-domaine (section 3B) qui reconstruit la carte complète des cookies pour tous les domaines Google.

---

### 🔴 Cas 4 : Fermeture intempestive de la WebView pendant l'édition
- **Symptôme** : L'écran `NotebookAuthActivity` se refermait automatiquement au bout de 2.5 secondes alors que l'utilisateur tentait d'éditer ou de coller un JSON.
- **Cause Racine** : Le callback `captureStorageState` appelait systématiquement `finish()`.
- **Solution** : Supprimer l'appel automatique à `finish()`. L'écran reste ouvert et propose des boutons explicites `Coller Presse-papier`, `Éditer JSON` et `Fermer`.

---

## 6. 📄 Exemple Concret de Payload JSON (`storage_state.json`) Validé

Voici un exemple exact et complet de la structure JSON du `storage_state.json` fonctionnel prêt à être transmis au backend GCP :

```json
{
  "cookies": [
    {
      "name": "SID",
      "value": "g.a000EXAMPLE_SID_COOKIE_VALUE_REDACTED_FOR_SECURITY_PURPOSES",
      "domain": ".google.com",
      "path": "/",
      "expires": -1,
      "httpOnly": false,
      "secure": true,
      "sameSite": "Lax"
    },
    {
      "name": "HSID",
      "value": "EXAMPLE_HSID_VALUE",
      "domain": ".google.com",
      "path": "/",
      "expires": -1,
      "httpOnly": true,
      "secure": true,
      "sameSite": "Lax"
    },
    {
      "name": "SSID",
      "value": "EXAMPLE_SSID_VALUE",
      "domain": ".google.com",
      "path": "/",
      "expires": -1,
      "httpOnly": true,
      "secure": true,
      "sameSite": "Lax"
    },
    {
      "name": "APISID",
      "value": "EXAMPLE_APISID_VALUE",
      "domain": ".google.com",
      "path": "/",
      "expires": -1,
      "httpOnly": true,
      "secure": true,
      "sameSite": "Lax"
    },
    {
      "name": "SAPISID",
      "value": "EXAMPLE_SAPISID_VALUE",
      "domain": ".google.com",
      "path": "/",
      "expires": -1,
      "httpOnly": true,
      "secure": true,
      "sameSite": "Lax"
    },
    {
      "name": "__Secure-1PSID",
      "value": "g.a000EXAMPLE_1PSID_COOKIE_VALUE_REDACTED",
      "domain": ".google.com",
      "path": "/",
      "expires": -1,
      "httpOnly": true,
      "secure": true,
      "sameSite": "Lax"
    },
    {
      "name": "__Secure-3PSID",
      "value": "g.a000EXAMPLE_3PSID_COOKIE_VALUE_REDACTED",
      "domain": ".google.com",
      "path": "/",
      "expires": -1,
      "httpOnly": true,
      "secure": true,
      "sameSite": "None"
    },
    {
      "name": "__Secure-1PSIDTS",
      "value": "sidts-CjEBPWEu2_EXAMPLE_TIMESTAMP_TOKEN_REDACTED",
      "domain": ".google.com",
      "path": "/",
      "expires": -1,
      "httpOnly": true,
      "secure": true,
      "sameSite": "Lax"
    },
    {
      "name": "__Secure-3PSIDTS",
      "value": "sidts-CjEBPWEu2_EXAMPLE_TIMESTAMP_TOKEN_REDACTED",
      "domain": ".google.com",
      "path": "/",
      "expires": -1,
      "httpOnly": true,
      "secure": true,
      "sameSite": "None"
    },
    {
      "name": "ACCOUNT_CHOOSER",
      "value": "EXAMPLE_ACCOUNT_CHOOSER_COOKIE_REDACTED",
      "domain": "accounts.google.com",
      "path": "/",
      "expires": -1,
      "httpOnly": false,
      "secure": true,
      "sameSite": "Lax"
    },
    {
      "name": "LSID",
      "value": "o.notebook.google.com|s.FR|s.youtube:g.a000EXAMPLE_LSID_COOKIE_REDACTED",
      "domain": "accounts.google.com",
      "path": "/",
      "expires": -1,
      "httpOnly": true,
      "secure": true,
      "sameSite": "Lax"
    },
    {
      "name": "OSID",
      "value": "g.a000EXAMPLE_OSID_COOKIE_VALUE_REDACTED",
      "domain": "notebook.google.com",
      "path": "/",
      "expires": -1,
      "httpOnly": true,
      "secure": true,
      "sameSite": "Lax"
    },
    {
      "name": "__Secure-OSID",
      "value": "g.a000EXAMPLE_SECURE_OSID_COOKIE_VALUE_REDACTED",
      "domain": "notebook.google.com",
      "path": "/",
      "expires": -1,
      "httpOnly": true,
      "secure": true,
      "sameSite": "Lax"
    }
  ],
  "origins": [
    {
      "origin": "https://notebook.google.com",
      "localStorage": [
        {
          "name": "nlm_dismissed_rebrand_education_modal",
          "value": "true"
        },
        {
          "name": "bcsp",
          "value": "system"
        }
      ]
    },
    {
      "origin": "https://accounts.google.com",
      "localStorage": []
    }
  ]
}
```
