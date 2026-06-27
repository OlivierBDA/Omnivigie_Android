package com.olivierbda.omnivigie.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface GmailService {
    @GET("gmail/v1/users/{userId}/messages")
    suspend fun listMessages(
        @Header("Authorization") token: String,
        @Path("userId") userId: String = "me",
        @Query("q") query: String? = null,
        @Query("maxResults") maxResults: Int = 10
    ): GmailListResponse

    @GET("gmail/v1/users/{userId}/messages/{id}")
    suspend fun getMessage(
        @Header("Authorization") token: String,
        @Path("userId") userId: String = "me",
        @Path("id") id: String
    ): GmailMessage
}
