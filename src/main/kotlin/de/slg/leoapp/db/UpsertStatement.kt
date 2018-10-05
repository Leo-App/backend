package de.slg.leoapp.db

import tel.egram.kuery.Dialect
import tel.egram.kuery.Subject
import tel.egram.kuery.Table

class UpsertStatement<T: Table>(
        val upsert: UpsertData,
        val subject: Subject<T>) {

    fun toString(dialect: Dialect): String {
        return MySQLDialect.build(this)
    }
}