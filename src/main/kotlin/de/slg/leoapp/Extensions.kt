package de.slg.leoapp

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import java.io.File
import java.io.InputStream

fun InputStream.toFile(path: String) {
    val file = File(path)
    if (!file.exists()) file.createNewFile()

    file.outputStream().use {
        copyTo(it)
    }
}

@Suppress("unchecked_cast")
fun Table.getColumnForFieldName(name: String): Column<Any>? {
    if (javaClass.getField(name) == null) return null
    return javaClass.getField(name).get(this) as Column<Any>
}

