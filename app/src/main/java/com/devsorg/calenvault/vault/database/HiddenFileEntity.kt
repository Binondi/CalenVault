package com.devsorg.calenvault.vault.database

import com.devsorg.calenvault.vault.utils.FileManager

data class HiddenFileEntity(
    val filePath: String,
    val fileName: String,
    val encryptedFileName: String,
    val fileType: FileManager.FileType,
    val originalExtension: String,
    val isEncrypted: Boolean,
    var dateAdded: Long = System.currentTimeMillis()
)


