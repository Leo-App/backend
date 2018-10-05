package de.slg.leoapp.db

import tel.egram.kuery.Subject
import tel.egram.kuery.Table
import tel.egram.kuery.dml.Assignment

class BatchInsertStatement<T : Table>(
        val assignments: List<BatchValue>,
        val subject: Subject<T>) {

    override fun toString(): String {
        return MySQLDialect.build(this)
    }
}