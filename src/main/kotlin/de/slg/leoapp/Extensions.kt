package de.slg.leoapp

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager
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

fun <T : Table> T.insertOrUpdate(vararg onDuplicateUpdateKeys: Column<*>, body: T.(InsertStatement<Number>) -> Unit) =
        InsertOrUpdate<Number>(onDuplicateUpdateKeys,this).apply {
            body(this)
            execute(TransactionManager.current())
        }

class InsertOrUpdate<Key : Any>(
        private val onDuplicateUpdateKeys: Array< out Column<*>>,
        table: Table,
        isIgnore: Boolean = false
) : InsertStatement<Key>(table, isIgnore) {
    override fun prepareSQL(transaction: Transaction): String {
        val onUpdateSQL = if(onDuplicateUpdateKeys.isNotEmpty()) {
            " ON DUPLICATE KEY UPDATE " + onDuplicateUpdateKeys.joinToString { "${transaction.identity(it)}=VALUES(${transaction.identity(it)})" }
        } else ""
        return super.prepareSQL(transaction) + onUpdateSQL
    }
}


