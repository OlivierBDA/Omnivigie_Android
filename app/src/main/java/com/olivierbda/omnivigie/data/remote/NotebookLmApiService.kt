package com.olivierbda.omnivigie.data.remote

import retrofit2.http.Field  
import retrofit2.http.FormUrlEncoded  
import retrofit2.http.Header  
import retrofit2.http.POST  
import retrofit2.http.Query

interface NotebookLmApiService {

    /**  
     * Exécute une requête RPC groupée (batch) vers l'infrastructure de NotebookLM.  
     *  
     * @param rpcId L'identifiant de la fonction RPC (ex: "CCqFvf" pour la création, "izAoDd" pour l'ajout de source).  
     * @param fSid Le Session ID (FdrFJe) extrait lors de la phase de connexion.  
     * @param cookie Chaîne de cookies contenant le SID valide de l'utilisateur.  
     * @param csrfToken Le jeton CSRF (SNlM0e) indispensable pour sécuriser l'appel RPC.  
     * @param req Le payload sérialisé au format attendu par batchexecute.  
     */  
    @FormUrlEncoded
    @POST("_/NotebookLmUi/data/batchexecute")
    suspend fun batchExecute(  
        @Query("rpcids") rpcId: String,  
        @Query("f.sid") fSid: String,
        @Query("at") at: String,
        @Header("Cookie") cookie: String,
        @Header("User-Agent") userAgent: String,
        @Header("X-Goog-Ext-277745143-Jspb") csrfToken: String,  
        @Field("f.req") req: String  
    ): String  
}
