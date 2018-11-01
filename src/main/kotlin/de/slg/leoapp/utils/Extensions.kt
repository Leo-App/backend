package de.slg.leoapp.utils

import com.squareup.moshi.Moshi
import tel.egram.kuery.Table
import java.io.File
import java.io.InputStream

fun InputStream.toFile(path: String) {
    val file = File(path)
    if (!file.exists()) file.createNewFile()

    file.outputStream().use {
        copyTo(it)
    }
}

inline operator fun <reified D, reified R> List<D>.invoke(mapping: D.() -> R): List<R> {
    val list = mutableListOf<R>()
    this.forEach { list.add(it.mapping()) }
    return list
}

fun Table.getColumnForFieldName(name: String): Table.Column? {
    for (cur in javaClass.declaredFields) {
        val value = cur.get(this)
        if (value is Table.Column) {
            if (value.name == name) return value
        }
    }
    return null
}

inline fun <reified R> String.parseJSON(): R? {
    val moshi = Moshi.Builder().build()
    return moshi.adapter(R::class.java).fromJson(this)
}
