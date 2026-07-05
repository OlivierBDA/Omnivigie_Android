package com.olivierbda.omnivigie.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "articles",
    foreignKeys = [
        ForeignKey(
            entity = EmailEntity::class,
            parentColumns = ["id"],
            childColumns = ["emailId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["emailId"])]
)
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val emailId: String,
    val title: String,
    val url: String,
    val source: String,
    val readingTime: String,
    val summary: String,
    val isSponsor: Boolean,
    
    // AI Qualification fields
    val aiInterest: Boolean? = null,
    val aiThemes: List<String> = emptyList(),
    val aiExplanation: String? = null,
    
    val isQualified: Boolean = false,
    val isSentToNotebook: Boolean = false,
    val notebookId: String? = null,
    val notebookName: String? = null
)
