package com.qdash.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    fun openPdfFile(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(context, "الملف غير موجود!", Toast.LENGTH_SHORT).show()
                return
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "لا يمكن فتح الملف: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Copies an image URI (e.g. from gallery picker) to app internal storage.
     * Returns the absolute path of the saved file, or null on failure.
     */
    suspend fun copyUriToInternalStorage(
        context: Context,
        uri: Uri,
        subDir: String = "account_icons"
    ): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, subDir).apply { mkdirs() }
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val destFile = File(dir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists()) destFile.absolutePath else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

