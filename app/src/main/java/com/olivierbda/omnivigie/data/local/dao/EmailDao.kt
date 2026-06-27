package com.olivierbda.omnivigie.data.local.dao

import androidx.room.*
import com.olivierbda.omnivigie.data.local.entities.EmailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmail(email: EmailEntity)

    @Query("SELECT * FROM emails ORDER BY receivedDate DESC")
    fun getAllEmails(): Flow<List<EmailEntity>>

    @Query("SELECT * FROM emails WHERE id = :id")
    suspend fun getEmailById(id: String): EmailEntity?

    @Delete
    suspend fun deleteEmail(email: EmailEntity)
}
