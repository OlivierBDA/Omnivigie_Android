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
                put(JSONArray().apply {
                    put(outerArray)
                })
            }.toString()

            val responseText = apiService.batchExecute(  
                rpcId = "CCqFvf",  
                fSid = session.fdrfje,  
                cookie = session.cookies,  
                csrfToken = session.snlm0e,  
                req = fReq  
            )

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
                put(JSONArray().apply {
                    put(outerArray)
                })
            }.toString()

            val responseText = apiService.batchExecute(  
                rpcId = "izAoDd",  
                fSid = session.fdrfje,  
                cookie = session.cookies,  
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
     * Utilise une expression régulière pour capturer les identifiants de ressource renvoyés par Google RPC.  
     */  
    private fun extractIdFromResponse(response: String): String? {  
        val pattern = Pattern.compile("\"([a-zA-Z0-9_-]{16,})\"")  
        val matcher = pattern.matcher(response)  
        return if (matcher.find()) matcher.group(1) else null  
    }  
}
