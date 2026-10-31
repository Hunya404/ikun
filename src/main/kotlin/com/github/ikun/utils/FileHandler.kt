package com.github.ikun.utils

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class FileHandler(val file: File) {
    private val mutex = Mutex()
    private val tmp = File(file.parentFile, "${file.name}.tmp")

    init {
        file.parentFile?.mkdirs()
        if (tmp.exists()) tmp.delete()
        if (! file.exists()) file.createNewFile()
    }

    fun write(value: String) = runBlocking { mutex.withLock { atomicWrite(value) } }

    private fun atomicWrite(value: String) = try {
        FileOutputStream(tmp).use { fos ->
            fos.write(value.toByteArray())
            fos.flush()
            fos.fd.sync()
        }

        // The temporary file was already synced above. A second sync through a
        // read-only handle can fail on Windows, even after the config was saved.
        try {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
    finally {
        tmp.delete()
    }
}
