# **Instructions de Codage : Implémentation de l'Étape 7 (NotebookLM API Client)**

Ce document contient les spécifications techniques et les directives de développement pour implémenter la création de carnets (notebooks) et l'ajout de sources via l'API RPC de NotebookLM dans l'application Android **Omnivigie**.

L'agent de codage doit suivre strictement ces étapes, respecter l'architecture du projet et appliquer les structures de payloads Google RPC (Jspb) décrites ci-dessous.

## **📌 Contexte Technique & Hypothèses**

1. **Authentification validée** : Les cookies SID, le jeton CSRF SNlM0e et le Session ID FdrFJe ont déjà été récupérés et sont gérés par le SessionManager du projet (dans com.olivierbda.omnivigie.data.auth.SessionManager).  
2. **Architecture** : Clean Architecture (data, domain, di, ui).  
3. **Persistance locale** : Room database (OmnivigieDatabase) avec ArticleDao gérant la table articles (ArticleEntity).  
4. **Réseau** : Utilisation de Retrofit et d'OkHttpClient pour les requêtes HTTP/2 asynchrones via les Coroutines Kotlin.

## **🛠️ Étape 1 : Interface de Service Retrofit (NotebookLmApiService.kt)**

**Fichier à créer :** app/src/main/java/com/olivierbda/omnivigie/data/remote/NotebookLmApiService.kt

L'agent de codage doit implémenter l'appel unique d'API RPC sur l'endpoint batchexecute. Cet endpoint utilise des requêtes POST de type application/x-www-form-urlencoded.

package com.olivierbda.omnivigie.data.remote

import retrofit2.http.Field  
import retrofit2.http.FormUrlEncoded  
import retrofit2.http.Header  
import retrofit2.http.POST  
import retrofit2.http.Query

interface NotebookLmApiService {

    /\*\*  
     \* Exécute une requête RPC groupée (batch) vers l'infrastructure de NotebookLM.  
     \*  
     \* @param rpcId L'identifiant de la fonction RPC (ex: "CCqFvf" pour la création, "izAoDd" pour l'ajout de source).  
     \* @param fSid Le Session ID (FdrFJe) extrait lors de la phase de connexion.  
     \* @param cookie Chaîne de cookies contenant le SID valide de l'utilisateur.  
     \* @param csrfToken Le jeton CSRF (SNlM0e) indispensable pour sécuriser l'appel RPC.  
     \* @param req Le payload sérialisé au format attendu par batchexecute.  
     \*/  
    @FormUrlEncoded  
    @POST("\_/NotebookLmUi/data/batchexecute")  
    suspend fun batchExecute(  
        @Query("rpcids") rpcId: String,  
        @Query("f.sid") fSid: String,  
        @Header("Cookie") cookie: String,  
        @Header("X-Goog-Ext-277745143-Jspb") csrfToken: String,  
        @Field("f.req") req: String  
    ): String  
}

## **🛠️ Étape 2 : Le Repository (NotebookLmRepository.kt)**

**Fichier à créer :** app/src/main/java/com/olivierbda/omnivigie/data/repository/NotebookLmRepository.kt

Le Repository doit :

1. Récupérer la session active via le SessionManager.  
2. Formater dynamiquement les requêtes f.req en respectant l'encapsulation de chaînes JSON imbriquées (Jspb).  
3. Parser la réponse brute textuelle pour y extraire les identifiants d'objets.

**⚠️ RÈGLE DE CRATION DU JSON (CRITIQUE) :**

La structure RPC attend que certains paramètres internes (les arguments de la fonction RPC) soient passés sous la forme d'une *chaîne de caractères JSON sérialisée* à l'intérieur d'un tableau JSON principal, et non sous forme d'objets JSON bruts structurés.

### **Algorithmes de Payload à implémenter :**

* **Création de Notebook (rpcid: CCqFvf)** :  
  Format logique : \[\[\["CCqFvf","\[\\"\[TITRE\]\\",null,\[\]\]\\",null,\\"generic\\"\]\]\]  
* **Ajout de Source URL (rpcid: izAoDd)** :  
  Format logique : \[\[\["izAoDd","\[\\"\[NOTEBOOK\_ID\]\\",null,null,\[\\"\[URL\]\\",\\"\[TITRE\]\\",null,1\]\]\\",null,\\"generic\\"\]\]\]

### **Code de référence à implémenter par l'Agent :**

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

    private val TAG \= "NotebookLmRepository"

    /\*\*  
     \* Crée un nouveau carnet de notes thématique dans NotebookLM.  
     \* @param title Le titre du carnet (ex: "\[AI\] 2026-05-11 TLDR-Machine Learning")  
     \* @return L'ID du notebook créé (chaîne alphanumérique d'au moins 16 caractères), ou null en cas d'échec.  
     \*/  
    suspend fun createNotebook(title: String): String? {  
        val session \= sessionManager.getNotebookSession() ?: run {  
            Log.e(TAG, "Aucune session NotebookLM valide stockée.")  
            return null  
        }  
          
        try {  
            // Création du tableau interne d'arguments sérialisé en String  
            val innerRequest \= JSONArray().apply {  
                put(title)  
                put(JSONObject.NULL)  
                put(JSONArray())  
            }

            // Encapsulation dans le format RPC global  
            val fReq \= JSONArray().apply {  
                val outerArray \= JSONArray().apply {  
                    put("CCqFvf")  
                    put(innerRequest.toString())  
                    put(JSONObject.NULL)  
                    put("generic")  
                }  
                put(JSONArray().put(outerArray))  
            }.toString()

            val responseText \= apiService.batchExecute(  
                rpcId \= "CCqFvf",  
                fSid \= session.fdrfje,  
                cookie \= session.cookies,  
                csrfToken \= session.snlm0e,  
                req \= fReq  
            )

            val notebookId \= extractIdFromResponse(responseText)  
            Log.d(TAG, "Notebook créé avec succès. ID extrait : $notebookId")  
            return notebookId  
        } catch (e: Exception) {  
            Log.e(TAG, "Erreur lors de la création du Notebook : ${e.message}", e)  
            return null  
        }  
    }

    /\*\*  
     \* Associe un article de veille en ajoutant son URL en tant que source au carnet spécifié.  
     \* @param notebookId L'identifiant unique du Notebook de destination.  
     \* @param title Le titre de l'article pour affichage dans les sources.  
     \* @param url L'adresse Web (propre et sans tracking) de l'article de veille.  
     \* @return true si l'ajout de la source est validé, false sinon.  
     \*/  
    suspend fun addUrlSource(notebookId: String, title: String, url: String): Boolean {  
        val session \= sessionManager.getNotebookSession() ?: run {  
            Log.e(TAG, "Session NotebookLM manquante.")  
            return false  
        }

        try {  
            // Représente le sous-tableau d'informations sur l'URL : \[url, titre, null, type (1 \= URL web)\]  
            val urlDetails \= JSONArray().apply {  
                put(url)  
                put(title)  
                put(JSONObject.NULL)  
                put(1)   
            }

            // Tableau d'arguments principaux : \[notebookId, null, null, urlDetails\]  
            val innerRequest \= JSONArray().apply {  
                put(notebookId)  
                put(JSONObject.NULL)  
                put(JSONObject.NULL)  
                put(urlDetails)  
            }

            val fReq \= JSONArray().apply {  
                val outerArray \= JSONArray().apply {  
                    put("izAoDd")  
                    put(innerRequest.toString())  
                    put(JSONObject.NULL)  
                    put("generic")  
                }  
                put(JSONArray().put(outerArray))  
            }.toString()

            val responseText \= apiService.batchExecute(  
                rpcId \= "izAoDd",  
                fSid \= session.fdrfje,  
                cookie \= session.cookies,  
                csrfToken \= session.snlm0e,  
                req \= fReq  
            )

            // Si la réponse brute renvoie au moins l'ID du notebook d'origine, l'opération a réussi  
            val success \= responseText.contains(notebookId)  
            Log.d(TAG, "Ajout de la source '$title' : $success")  
            return success  
        } catch (e: Exception) {  
            Log.e(TAG, "Erreur lors de l'ajout de la source URL $url : ${e.message}", e)  
            return false  
        }  
    }

    /\*\*  
     \* Utilise une expression régulière pour capturer les identifiants de ressource renvoyés par Google RPC.  
     \*/  
    private fun extractIdFromResponse(response: String): String? {  
        val pattern \= Pattern.compile("\\"(\[a-zA-Z0-9\_-\]{16,})\\"")  
        val matcher \= pattern.matcher(response)  
        return if (matcher.find()) matcher.group(1) else null  
    }  
}

## **🛠️ Étape 3 : Mise à jour de la Base Locale (ArticleDao.kt)**

L'agent de codage doit ajouter ou mettre à jour les requêtes d'accès aux données dans le DAO Room existant afin de récupérer les articles qualifiés intéressants et de mettre à jour leur état une fois injectés dans un carnet.

**Fichier à modifier :** app/src/main/java/com/olivierbda/omnivigie/data/local/dao/ArticleDao.kt

Ajouter les signatures suivantes :

// Dans ArticleDao.kt

/\*\*  
 \* Récupère les articles qualifiés d'intéressants (isInteresting \= true) d'un thème précis,  
 \* et qui n'ont pas encore été traités / exportés (isProcessed \= false).  
 \*/  
@Query("SELECT \* FROM articles WHERE isInteresting \= 1 AND isProcessed \= 0 AND tags LIKE :themePattern")  
suspend fun getInterestingPendingArticlesByTheme(themePattern: String): List\<ArticleEntity\>

/\*\*  
 \* Marque un lot d'articles comme traités, et mémorise le carnet de destination.  
 \*/  
@Query("UPDATE articles SET isProcessed \= 1, notebookId \= :notebookId, notebookName \= :notebookName WHERE id IN (:articleIds)")  
suspend fun markArticlesAsProcessedInNotebook(  
    articleIds: List\<Long\>,   
    notebookId: String,   
    notebookName: String  
)

*(Note : Ajustez le nom de la table articles et des colonnes isInteresting, isProcessed, notebookId, notebookName selon le schéma exact défini à l'Étape 2 du projet).*

## **🛠️ Étape 4 : Le Use Case Métier (CreateThemedNotebookUseCase.kt)**

**Fichier à créer :** app/src/main/java/com/olivierbda/omnivigie/domain/usecase/CreateThemedNotebookUseCase.kt

Ce Use Case coordonne le flux complet de création de bout en bout :

package com.olivierbda.omnivigie.domain.usecase

import android.util.Log  
import com.olivierbda.omnivigie.data.local.dao.ArticleDao  
import com.olivierbda.omnivigie.data.repository.NotebookLmRepository  
import kotlinx.coroutines.Dispatchers  
import kotlinx.coroutines.withContext  
import java.text.SimpleDateFormat  
import java.util.Date  
import java.util.Locale

class CreateThemedNotebookUseCase(  
    private val notebookRepository: NotebookLmRepository,  
    private val articleDao: ArticleDao  
) {  
    private val TAG \= "CreateThemedNotebook"

    suspend fun execute(theme: String): Result\<String\> \= withContext(Dispatchers.IO) {  
        // 1\. Recherche des articles qualifiés en local  
        val pattern \= "%$theme%"  
        val pendingArticles \= articleDao.getInterestingPendingArticlesByTheme(pattern)

        if (pendingArticles.isEmpty()) {  
            return@withContext Result.failure(Exception("Aucun article pertinent et non traité pour le thème '$theme'"))  
        }

        // 2\. Formatage du nom officiel du Notebook  
        val dateString \= SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())  
        val notebookName \= "\[AI\] $dateString TLDR-$theme"

        Log.d(TAG, "Lancement de la création du carnet : $notebookName avec ${pendingArticles.size} articles.")

        // 3\. Appel à NotebookLM pour la création du carnet de notes  
        val notebookId \= notebookRepository.createNotebook(notebookName)  
            ?: return@withContext Result.failure(Exception("Échec de l'initialisation du carnet auprès de NotebookLM (problème réseau ou session expirée)."))

        // 4\. Ajout itératif des sources URL dans le carnet  
        val successProcessedIds \= mutableListOf\<Long\>()  
        for (article in pendingArticles) {  
            val isAdded \= notebookRepository.addUrlSource(  
                notebookId \= notebookId,  
                title \= article.title,  
                url \= article.url  
            )  
            if (isAdded) {  
                // Utilisation de l'id auto-généré de l'entité Room  
                successProcessedIds.add(article.id)  
            }  
        }

        // 5\. Mise à jour de l'état local en base Room  
        if (successProcessedIds.isNotEmpty()) {  
            articleDao.markArticlesAsProcessedInNotebook(  
                articleIds \= successProcessedIds,  
                notebookId \= notebookId,  
                notebookName \= notebookName  
            )  
            Log.d(TAG, "DB locale mise à jour. ${successProcessedIds.size} articles marqués traités.")  
        } else {  
            return@withContext Result.failure(Exception("Le carnet a été créé mais aucun article n'a pu y être injecté en tant que source."))  
        }

        Result.success(notebookName)  
    }  
}

## **🛠️ Étape 5 : Module de Dépendances Hilt / DI**

L'agent de codage doit s'assurer que les dépendances nécessaires sont correctement déclarées et injectées dans le graphe Hilt.

### **Configuration du Timeout Réseau (Important) :**

La création et le scraping des liens externes par les bots internes de Google NotebookLM prenant du temps, l'agent doit configurer un timeout personnalisé sur l'instance d'OkHttpClient dédiée à ce service (au moins **45 secondes**).

// Directives d'injection pour le Network Module Hilt existant :

@Provides  
@Singleton  
fun provideNotebookLmApiService(okHttpClient: OkHttpClient): NotebookLmApiService {  
    // Adapter le client existant en augmentant les timeouts pour éviter les SocketTimeoutException  
    val customizedClient \= okHttpClient.newBuilder()  
        .connectTimeout(45, TimeUnit.SECONDS)  
        .readTimeout(45, TimeUnit.SECONDS)  
        .writeTimeout(45, TimeUnit.SECONDS)  
        .build()

    return Retrofit.Builder()  
        .baseUrl("\[https://notebooklm.google.com/\](https://notebooklm.google.com/)")  
        .client(customizedClient)  
        .addConverterFactory(ScalarsConverterFactory.create()) // Obligatoire pour consommer les réponses RPC brutes  
        .build()  
        .create(NotebookLmApiService::class.java)  
}

## **🧪 Protocole de validation de l'implémentation (Tests à faire valider par l'Agent)**

L'agent de codage doit confirmer les points de contrôle suivants :

1. **Zéro Crash Réseau** : Vérifier que les requêtes vers l'URL Google RPC ne lèvent pas de MalformedURLException en encodant proprement l'URL de base et les paramètres.  
2. **Encodage de Payload** : Valider via des tests unitaires ou des logs précis que f.req est produit sous une forme de type \[\[\["CCqFvf","\[\\"Mon Titre\\",null,\[\]\]\\",null,\\"generic\\"\]\]\] (les guillemets internes de la sous-requête doivent impérativement être échappés).  
3. **Mise à jour SQLite locale** : Suite au retour Result.success, vérifier directement la base Room pour confirmer que les articles concernés ont bien changé leur statut de isProcessed de 0 à 1\.