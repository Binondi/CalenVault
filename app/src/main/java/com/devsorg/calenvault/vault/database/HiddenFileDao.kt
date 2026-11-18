package com.devsorg.calenvault.vault.database

import kotlinx.coroutines.flow.Flow

interface HiddenFileDao {
    fun getAllHiddenFiles(): Flow<List<HiddenFileEntity>>

    suspend fun insertHiddenFile(hiddenFile: HiddenFileEntity)

    suspend fun deleteHiddenFile(hiddenFile: HiddenFileEntity)

    suspend fun getHiddenFileByPath(filePath: String): HiddenFileEntity?

    suspend fun getHiddenFileByOriginalName(fileName: String): HiddenFileEntity?

    suspend fun updateEncryptionStatus(
        filePath: String,
        newFilePath: String,
        encryptedFileName: String?,
        isEncrypted: Boolean
    )

    suspend fun updateHiddenFile(hiddenFile: HiddenFileEntity)
}


