package com.olivierbda.omnivigie.data.remote

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface NotebookLmApiService {

    /**  
     * Exécute une requête RPC groupée (batch) vers l'infrastructure de NotebookLM.  
     */  
    @FormUrlEncoded
    @POST("_/NotebookLmUi/data/batchexecute")
    suspend fun batchExecute(  
        @Query("rpcids") rpcId: String,  
        @Query("f.sid") fSid: String,
        @Header("Cookie") cookie: String,
        @Field("at") csrfToken: String, // Le jeton SNlM0e DOIT être dans le body
        @Field("f.req") req: String  
    ): String  
}
