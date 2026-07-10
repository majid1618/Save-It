package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedUrlDao {
    @Query("SELECT * FROM saved_urls ORDER BY createdAt DESC")
    fun getAllSavedUrls(): Flow<List<SavedUrl>>

    @Query("SELECT * FROM saved_urls WHERE folderId = :folderId ORDER BY createdAt DESC")
    fun getSavedUrlsByFolder(folderId: Long): Flow<List<SavedUrl>>

    @Query("SELECT * FROM saved_urls WHERE id = :id")
    suspend fun getSavedUrlById(id: Long): SavedUrl?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedUrl(savedUrl: SavedUrl): Long

    @Update
    suspend fun updateSavedUrl(savedUrl: SavedUrl)

    @Delete
    suspend fun deleteSavedUrl(savedUrl: SavedUrl)

    @Query("DELETE FROM saved_urls WHERE id = :id")
    suspend fun deleteSavedUrlById(id: Long)

    @Query("SELECT * FROM saved_urls WHERE expiryTime IS NOT NULL AND expiryTime <= :currentTime")
    suspend fun getExpiredUrls(currentTime: Long): List<SavedUrl>

    @Query("DELETE FROM saved_urls WHERE expiryTime IS NOT NULL AND expiryTime <= :currentTime")
    suspend fun deleteExpiredUrls(currentTime: Long)
}
