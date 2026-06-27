package com.olivierbda.omnivigie.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emails")
data class EmailEntity(
    @PrimaryKey
    val id: String, // Gmail Message ID
    val receivedDate: Long,
    val sender: String,
    val subject: String,
    val bodyHtml: String,
    val isProcessed: Boolean = false
)
