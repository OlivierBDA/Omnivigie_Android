package com.olivierbda.omnivigie.data.repository

import android.util.Log  
import com.olivierbda.omnivigie.data.auth.SessionManager  
import com.olivierbda.omnivigie.data.remote.NotebookLmApiService  
import org.json.JSONArray  
import org.json.JSONObject  
import java.util.regex.Pattern

class NotebookLmRepository(  
    private val apiService: NotebookLmApiService,  
    private val sessionManager: SessionManager  
) {

    private val TAG = "NotebookLmRepository"

    private val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    /**  
     * Crée un nouveau carnet de notes thématique dans NotebookLM.  
     * @param title Le titre du carnet (ex: "[AI] 2026-05-11 TLDR-Machine Learning")  
     * @return L'ID du notebook créé (chaîne alphanumérique d'au moins 16 caractères), ou null en cas d'échec.  
     */  
    suspend fun createNotebook(title: String): String? {  
        val session = sessionManager.getNotebookSession() ?: run {  
            Log.e(TAG, "Aucune session NotebookLM valide stockée.")  
            return null  
        }  
          
        try {  
            // Création du tableau interne d'arguments sérialisé en String  
            val innerRequest = JSONArray().apply {  
                put(title)  
                put(JSONObject.NULL)  
                put(JSONArray())  
            }

            // Encapsulation dans le format RPC global
            val outerArray = JSONArray().apply {
                put("CCqFvf")
                put(innerRequest.toString())
                put(JSONObject.NULL)
                put("generic")
            }
            val fReq = JSONArray().apply {
                val batch = JSONArray().apply {
                    put(outerArray)
                }
                put(batch)
            }.toString()

            Log.d(TAG, "Envoi RPC CCqFvf - f.sid: ${session.fdrfje}")
            Log.d(TAG, "Payload f.req: $fReq")

            val responseText = apiService.batchExecute(  
                rpcId = "CCqFvf",  
                fSid = session.fdrfje,
                at = session.snlm0e,
                cookie = session.cookies,
                userAgent = USER_AGENT,
                csrfToken = session.snlm0e,  
                req = fReq  
            )

            Log.d(TAG, "Réponse brute NotebookLM: $responseText")

            val notebookId = extractIdFromResponse(responseText)  
            Log.d(TAG, "Notebook créé avec succès. ID extrait : $notebookId")  
            return notebookId  
        } catch (e: Exception) {  
            Log.e(TAG, "Erreur lors de la création du Notebook : ${e.message}", e)  
            return null  
        }  
    }

    /**  
     * Associe un article de veille en ajoutant son URL en tant que source au carnet spécifié.  
     * @param notebookId L'identifiant unique du Notebook de destination.  
     * @param title Le titre de l'article pour affichage dans les sources.  
     * @param url L'adresse Web (propre et sans tracking) de l'article de veille.  
     * @return true si l'ajout de la source est validé, false sinon.  
     */  
    suspend fun addUrlSource(notebookId: String, title: String, url: String): Boolean {  
        val session = sessionManager.getNotebookSession() ?: run {  
            Log.e(TAG, "Session NotebookLM manquante.")  
            return false  
        }

        try {  
            // Représente le sous-tableau d'informations sur l'URL : [url, titre, null, type (1 = URL web)]  
            val urlDetails = JSONArray().apply {  
                put(url)  
                put(title)  
                put(JSONObject.NULL)  
                put(1)   
            }

            // Tableau d'arguments principaux : [notebookId, null, null, urlDetails]  
            val innerRequest = JSONArray().apply {  
                put(notebookId)  
                put(JSONObject.NULL)  
                put(JSONObject.NULL)  
                put(urlDetails)  
            }

            val outerArray = JSONArray().apply {
                put("izAoDd")
                put(innerRequest.toString())
                put(JSONObject.NULL)
                put("generic")
            }
            val fReq = JSONArray().apply {
                val batch = JSONArray().apply {
                    put(outerArray)
                }
                put(batch)
            }.toString()

            Log.d(TAG, "Envoi RPC izAoDd - Source: $title")
            Log.d(TAG, "Payload f.req: $fReq")

            val responseText = apiService.batchExecute(  
                rpcId = "izAoDd",  
                fSid = session.fdrfje,
                at = session.snlm0e,
                cookie = session.cookies,
                userAgent = USER_AGENT,
                csrfToken = session.snlm0e,  
                req = fReq  
            )

            // Si la réponse brute renvoie au moins l'ID du notebook d'origine, l'opération a réussi  
            val success = responseText.contains(notebookId)  
            Log.d(TAG, "Ajout de la source '$title' : $success")  
            return success  
        } catch (e: Exception) {  
            Log.e(TAG, "Erreur lors de l'ajout de la source URL $url : ${e.message}", e)  
            return false  
        }  
    }

    /**
     * Fonction de debug pour lister les [limit] notebooks les plus récents.
     * Idéal pour valider que le compte est bien lié et voir les IDs existants.
     *
     * @param limit Le nombre de notebooks à retourner (par défaut 2).
     * @return Une liste de Pair(NotebookId, Titre)
     */
    suspend fun listRecentNotebooks(limit: Int = 2): List<Pair<String, String>> {
        val TAG_DEBUG = "NotebookLM_ListDebug"

        // 1. Vérification de la connexion
        val session = sessionManager.getNotebookSession()
        if (session == null) {
            Log.e(TAG_DEBUG, "Échec: Aucune session active. Impossible de lister les notebooks.")
            return emptyList()
        }

        Log.i(TAG_DEBUG, "✅ Étape 1 : Connexion à NotebookLM valide. FdrFJe extrait: ${session.fdrfje.take(5)}...")

        return try {
            Log.d(TAG_DEBUG, "⏳ Étape 2 : Préparation de la requête liste...")

            // rpcid exact utilisé par notebooklm-py pour lister les notebooks
            val rpcId = "v8L2hc"

            // Le format standard pour récupérer une liste
            val innerRequest = JSONArray().apply {
                put(JSONObject.NULL)
            }

            val outerArray = JSONArray().apply {
                put(rpcId)
                put(innerRequest.toString()) // Toujours toString() pour l'inner request !
                put(JSONObject.NULL)
                put("generic")
            }
            val fReq = JSONArray().apply {
                val batch = JSONArray().apply {
                    put(outerArray)
                }
                put(batch)
            }.toString()

            Log.d(TAG_DEBUG, "   Payload Jspb généré : $fReq")

            // 2. Appel réseau
            val responseText = apiService.batchExecute(
                rpcId = rpcId,
                fSid = session.fdrfje,
                at = session.snlm0e,
                cookie = session.cookies,
                userAgent = USER_AGENT,
                csrfToken = session.snlm0e,
                req = fReq
            )

            Log.d(TAG_DEBUG, "✅ Étape 3 : Réponse brute reçue (Taille: ${responseText.length} caractères)")

            // 3. Extraction (Parsing)
            val allNotebooks = extractNotebooks(responseText)
            val recentNotebooks = allNotebooks.take(limit)

            Log.i(TAG_DEBUG, "✅ Étape 4 : Extraction terminée. Affichage des $limit derniers notebooks :")
            if (recentNotebooks.isEmpty()) {
                Log.w(TAG_DEBUG, "   Aucun notebook trouvé ou échec du parsing Regex.")
            } else {
                recentNotebooks.forEachIndexed { index, notebook ->
                    Log.i(TAG_DEBUG, "   -> [${index + 1}] Titre : '${notebook.second}' | ID : ${notebook.first}")
                }
            }

            recentNotebooks

        } catch (e: Exception) {
            Log.e(TAG_DEBUG, "❌ Erreur critique lors de la liste des notebooks : ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Parse la chaîne RPC brute pour y trouver des paires ID de Notebook / Titre.
     */
    private fun extractNotebooks(response: String): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        val pattern = Pattern.compile("\"([a-zA-Z0-9_-]{16,})\"[^\\[]*\\[\"([^\"]+)\"")
        val matcher = pattern.matcher(response)

        while (matcher.find()) {
            val id = matcher.group(1)
            val title = matcher.group(2)
            if (id != null && title != null) {
                if (!title.contains("google.com") && title.length > 2) {
                    results.add(Pair(id, title))
                }
            }
        }
        return results.distinctBy { it.first }
    }

    /**  
     * Utilise une expression régulière pour capturer les identifiants de ressource renvoyés par Google RPC.  
     */  
    private fun extractIdFromResponse(response: String): String? {  
        val pattern = Pattern.compile("\"([a-zA-Z0-9_-]{16,})\"")  
        val matcher = pattern.matcher(response)  
        return if (matcher.find()) matcher.group(1) else null  
    }  
}
