package com.sharath.statusbartotal.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.FileNotFoundException

/** Writes a CSV into Download/StatusBarTotal/ through MediaStore (no permission needed). */
object ReportWriter {
    const val FOLDER = "Download/StatusBarTotal/"
    const val SHOWN_FOLDER = "Downloads/StatusBarTotal"

    /** True when the file was written and published; every failure is caught and returns false. */
    fun write(context: Context, fileName: String, text: String): Boolean {
        val resolver = context.contentResolver
        var uri: Uri? = null
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, FOLDER)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val created = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return false
            uri = created
            val out = resolver.openOutputStream(created) ?: throw FileNotFoundException(fileName)
            out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            resolver.update(created, done, null, null)
            true
        } catch (e: Exception) {
            uri?.let {
                try {
                    resolver.delete(it, null, null)
                } catch (ignored: Exception) {
                    // nothing more to do
                }
            }
            false
        }
    }
}
