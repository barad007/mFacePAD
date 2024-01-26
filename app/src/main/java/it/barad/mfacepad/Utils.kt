package it.barad.mfacepad

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.content.Context
import java.util.UUID

object Utils {
    private val privateFileName: String
        get() = UUID.randomUUID().toString().take(32)
    fun createPrivateFile(context: Context, parentDir: String, fileExtension: String = ""): File {
        val internalDir = File(context.filesDir, parentDir).apply { createIfDoesNotExist() }
        return File(internalDir, "$privateFileName$fileExtension")
    }

    @Throws(IOException::class)
    fun getFileFromAssets(context: Context, fileName: String): File = File(context.cacheDir, fileName)
        .also {
            if (!it.exists()) {
                it.outputStream().use { cache ->
                    context.assets.open(fileName).use { inputStream ->
                        inputStream.copyTo(cache)
                    }
                }
            }
        }

    fun File.createIfDoesNotExist(): Boolean = if (!exists()) mkdirs() else true

}