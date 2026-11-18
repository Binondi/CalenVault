package com.devsorg.calenvault.vault.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.devsorg.calenvault.vault.utils.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class AppDatabase private constructor(context: Context) {
    private val dbHelper = DatabaseHelper(context.applicationContext)

    fun hiddenFileDao(): HiddenFileDao = object : HiddenFileDao {
        override fun getAllHiddenFiles(): Flow<List<HiddenFileEntity>> = flow {
            emit(withContext(Dispatchers.IO) {
                val list = mutableListOf<HiddenFileEntity>()
                val db = dbHelper.readableDatabase
                db.rawQuery("SELECT * FROM hidden_files", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        list.add(cursorToEntity(cursor))
                    }
                }
                list
            })
        }

        override suspend fun insertHiddenFile(hiddenFile: HiddenFileEntity) = withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("filePath", hiddenFile.filePath)
                put("fileName", hiddenFile.fileName)
                put("encryptedFileName", hiddenFile.encryptedFileName)
                put("fileType", hiddenFile.fileType.name)
                put("originalExtension", hiddenFile.originalExtension)
                put("isEncrypted", if (hiddenFile.isEncrypted) 1 else 0)
                put("dateAdded", hiddenFile.dateAdded)
            }
            db.insertWithOnConflict("hidden_files", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            Unit
        }

        override suspend fun deleteHiddenFile(hiddenFile: HiddenFileEntity) = withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            db.delete("hidden_files", "filePath = ?", arrayOf(hiddenFile.filePath))
            Unit
        }

        override suspend fun getHiddenFileByPath(filePath: String): HiddenFileEntity? = withContext(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            db.rawQuery("SELECT * FROM hidden_files WHERE filePath = ? LIMIT 1", arrayOf(filePath)).use { cursor ->
                if (cursor.moveToFirst()) cursorToEntity(cursor) else null
            }
        }

        override suspend fun getHiddenFileByOriginalName(fileName: String): HiddenFileEntity? = withContext(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            db.rawQuery("SELECT * FROM hidden_files WHERE fileName = ? LIMIT 1", arrayOf(fileName)).use { cursor ->
                if (cursor.moveToFirst()) cursorToEntity(cursor) else null
            }
        }

        override suspend fun updateEncryptionStatus(
            filePath: String,
            newFilePath: String,
            encryptedFileName: String?,
            isEncrypted: Boolean
        ) = withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("isEncrypted", if (isEncrypted) 1 else 0)
                put("filePath", newFilePath)
                if (encryptedFileName != null) {
                    put("encryptedFileName", encryptedFileName)
                }
            }
            db.update("hidden_files", values, "filePath = ?", arrayOf(filePath))
            Unit
        }

        override suspend fun updateHiddenFile(hiddenFile: HiddenFileEntity) = withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("filePath", hiddenFile.filePath)
                put("fileName", hiddenFile.fileName)
                put("encryptedFileName", hiddenFile.encryptedFileName)
                put("fileType", hiddenFile.fileType.name)
                put("originalExtension", hiddenFile.originalExtension)
                put("isEncrypted", if (hiddenFile.isEncrypted) 1 else 0)
                put("dateAdded", hiddenFile.dateAdded)
            }
            db.update("hidden_files", values, "filePath = ?", arrayOf(hiddenFile.filePath))
            Unit
        }

        private fun cursorToEntity(cursor: android.database.Cursor): HiddenFileEntity {
            val typeStr = cursor.getString(cursor.getColumnIndexOrThrow("fileType"))
            val fileType = try {
                FileManager.FileType.valueOf(typeStr)
            } catch (_: Exception) {
                FileManager.FileType.DOCUMENT
            }
            return HiddenFileEntity(
                filePath = cursor.getString(cursor.getColumnIndexOrThrow("filePath")),
                fileName = cursor.getString(cursor.getColumnIndexOrThrow("fileName")),
                encryptedFileName = cursor.getString(cursor.getColumnIndexOrThrow("encryptedFileName")) ?: "",
                fileType = fileType,
                originalExtension = cursor.getString(cursor.getColumnIndexOrThrow("originalExtension")) ?: "",
                isEncrypted = cursor.getInt(cursor.getColumnIndexOrThrow("isEncrypted")) == 1,
                dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow("dateAdded"))
            )
        }
    }

    private class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "vault_database", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS hidden_files (
                    filePath TEXT PRIMARY KEY NOT NULL,
                    fileName TEXT NOT NULL,
                    encryptedFileName TEXT,
                    fileType TEXT NOT NULL,
                    originalExtension TEXT,
                    isEncrypted INTEGER NOT NULL,
                    dateAdded INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}


