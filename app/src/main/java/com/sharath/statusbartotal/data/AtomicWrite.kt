package com.sharath.statusbartotal.data

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal object AtomicWrite {
    /** Writes to "<name>.tmp", syncs it, then renames over the real file. */
    fun write(file: File, text: String): Boolean {
        return try {
            val tmp = File(file.parentFile, file.name + ".tmp")
            FileOutputStream(tmp, false).use { out ->
                out.write(text.toByteArray(Charsets.UTF_8))
                out.flush()
                out.fd.sync()
            }
            Files.move(
                tmp.toPath(), file.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
            )
            true
        } catch (e: IOException) {
            false
        }
    }

    fun readLines(file: File): List<String> = try {
        if (file.exists()) file.readText(Charsets.UTF_8).split('\n') else emptyList()
    } catch (e: IOException) {
        emptyList()
    }
}
