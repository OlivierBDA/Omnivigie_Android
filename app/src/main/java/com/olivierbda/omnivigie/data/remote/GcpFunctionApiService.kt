package com.olivierbda.omnivigie.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class GcpFunctionRequest(
    @SerializedName("action") val action: String,
    @SerializedName("notebook_name") val notebookName: String? = null,
    @SerializedName("notebook_id") val notebookId: String? = null,
    @SerializedName("urls") val urls: List<String>? = null,
    @SerializedName("notebooklm_storage_state") val storageState: Any? = null
)

data class GcpFunctionResponse(
    @SerializedName("notebook_id") val notebookId: String? = null,
    @SerializedName("notebook_name") val notebookName: String? = null,
    @SerializedName("added_urls") val addedUrls: List<String>? = null,
    @SerializedName("errors") val errors: List<Map<String, String>>? = null,
    @SerializedName("task_id") val taskId: String? = null,
    @SerializedName("error") val error: String? = null
)

interface GcpFunctionApiService {
    @POST("/")
    suspend fun executeAction(
        @Header("Authorization") bearerToken: String,
        @Body request: GcpFunctionRequest
    ): GcpFunctionResponse
}
