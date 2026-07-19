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
     */  
    suspend fun createNotebook(title: String): String? {  
        val session = sessionManager.getNotebookSession() ?: run {  
            Log.e(TAG, "Aucune session NotebookLM valide stockée.")  
            return null  
        }  
          
        try {  
            val innerRequest = JSONArray().apply {  
                put(title)  
                put(JSONObject.NULL)  
                put(JSONArray())  
            }

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
                cookie = session.cookies,
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
     */  
    suspend fun addUrlSource(notebookId: String, title: String, url: String): Boolean {  
        val session = sessionManager.getNotebookSession() ?: run {  
            Log.e(TAG, "Session NotebookLM manquante.")  
            return false  
        }

        try {  
            val urlDetails = JSONArray().apply {  
                put(url)  
                put(title)  
                put(JSONObject.NULL)  
                put(1)   
            }

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
                cookie = session.cookies,
                csrfToken = session.snlm0e,  
                req = fReq  
            )

            val success = responseText.contains(notebookId)  
            Log.d(TAG, "Ajout de la source '$title' : $success")  
            return success  
        } catch (e: Exception) {  
            Log.e(TAG, "Erreur lors de l'ajout de la source URL $url : ${e.message}", e)  
            return false  
        }  
    }

    /**
     * Fonction de debug pour lister les notebooks les plus récents.
     */
    suspend fun listRecentNotebooks(limit: Int = 2): List<Pair<String, String>> {
        val TAG_DEBUG = "NotebookLM_ListDebug"

        val session = sessionManager.getNotebookSession()
        if (session == null) {
            Log.e(TAG_DEBUG, "Échec: Aucune session active.")
            return emptyList()
        }

        return try {
            val rpcId = "v8L2hc" 

            val innerRequest = JSONArray()

            val fReq = JSONArray().apply {
                val outerArray = JSONArray().apply {
                    put(rpcId)
                    put(innerRequest.toString()) 
                    put(JSONObject.NULL)
                    put("generic")
                }
                val batch = JSONArray().apply {
                    put(outerArray)
                }
                put(batch)
            }.toString()

            Log.d(TAG_DEBUG, "Payload Jspb corrigé : $fReq")

            val responseText = apiService.batchExecute(
                rpcId = rpcId,
                fSid = session.fdrfje,
                cookie = session.cookies,
                csrfToken = session.snlm0e, 
                req = fReq
            )

            Log.d(TAG_DEBUG, "✅ Succès ! Réponse (Taille: ${responseText.length})")
            
            val allNotebooks = extractNotebooks(responseText)
            val recentNotebooks = allNotebooks.take(limit)

            recentNotebooks.forEachIndexed { index, notebook ->
                Log.i(TAG_DEBUG, "-> [${index + 1}] Titre : '${notebook.second}' | ID : ${notebook.first}")
            }
            recentNotebooks

        } catch (e: Exception) {
            Log.e(TAG_DEBUG, "❌ Erreur : ${e.message}", e)
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
