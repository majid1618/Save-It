package com.example.data

import kotlinx.coroutines.flow.Flow

class UrlRepository(private val folderDao: FolderDao, private val savedUrlDao: SavedUrlDao) {
    val allFolders: Flow<List<Folder>> = folderDao.getAllFolders()
    val allSavedUrls: Flow<List<SavedUrl>> = savedUrlDao.getAllSavedUrls()

    fun getSavedUrlsByFolder(folderId: Long): Flow<List<SavedUrl>> =
        savedUrlDao.getSavedUrlsByFolder(folderId)

    suspend fun getFolderById(id: Long): Folder? = folderDao.getFolderById(id)
    suspend fun insertFolder(folder: Folder): Long = folderDao.insertFolder(folder)
    suspend fun deleteFolder(folder: Folder) = folderDao.deleteFolder(folder)
    suspend fun deleteFolderById(folderId: Long) = folderDao.deleteFolderById(folderId)

    suspend fun getSavedUrlById(id: Long): SavedUrl? = savedUrlDao.getSavedUrlById(id)
    suspend fun insertSavedUrl(savedUrl: SavedUrl): Long = savedUrlDao.insertSavedUrl(savedUrl)
    suspend fun updateSavedUrl(savedUrl: SavedUrl) = savedUrlDao.updateSavedUrl(savedUrl)
    suspend fun deleteSavedUrl(savedUrl: SavedUrl) = savedUrlDao.deleteSavedUrl(savedUrl)
    suspend fun deleteSavedUrlById(id: Long) = savedUrlDao.deleteSavedUrlById(id)
    
    suspend fun getExpiredUrls(currentTime: Long): List<SavedUrl> = savedUrlDao.getExpiredUrls(currentTime)
    suspend fun deleteExpiredUrls(currentTime: Long) = savedUrlDao.deleteExpiredUrls(currentTime)
}
