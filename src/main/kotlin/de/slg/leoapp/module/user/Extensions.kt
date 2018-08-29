package de.slg.leoapp.module.user

import java.io.File
import java.io.InputStream

fun InputStream.toFile(path: String) {
    val file = File(path)
    if (!file.exists()) file.createNewFile()

    file.outputStream().use {
        copyTo(it)
    }
}

