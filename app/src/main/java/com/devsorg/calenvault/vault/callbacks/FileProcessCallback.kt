package com.devsorg.calenvault.vault.callbacks

import java.io.File

interface FileProcessCallback {
    fun onFilesProcessedSuccessfully(copiedFiles: List<File>)
    fun onFileProcessFailed()
}


