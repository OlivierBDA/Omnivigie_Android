package com.olivierbda.omnivigie.data.remote

data class GmailListResponse(
    val messages: List<GmailMessageSummary>? = null,
    val nextPageToken: String? = null,
    val resultSizeEstimate: Int? = null
)

data class GmailMessageSummary(
    val id: String,
    val threadId: String
)

data class GmailMessage(
    val id: String,
    val threadId: String,
    val labelIds: List<String>? = null,
    val snippet: String? = null,
    val payload: GmailPayload? = null,
    val internalDate: Long? = null
)

data class GmailPayload(
    val partId: String? = null,
    val mimeType: String? = null,
    val filename: String? = null,
    val headers: List<GmailHeader>? = null,
    val body: GmailBody? = null,
    val parts: List<GmailPayload>? = null
)

data class GmailHeader(
    val name: String,
    val value: String
)

data class GmailBody(
    val size: Int? = null,
    val data: String? = null // Base64 encoded
)
