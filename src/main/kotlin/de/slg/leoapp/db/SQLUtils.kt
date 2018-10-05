package de.slg.leoapp.db

import de.slg.leoapp.annotation.*
import de.slg.leoapp.utils.Secure
import tel.egram.kuery.Predicate
import tel.egram.kuery.Subject
import tel.egram.kuery.Table
import tel.egram.kuery.dml.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement
import java.text.SimpleDateFormat
import java.util.*

/* fallback */
fun execute(queries: List<String>) {
    getConnection().use {
        val statement = it.createStatement()
        queries.forEach { cur ->
            statement.addBatch(cur)
        }
        statement.executeBatch()
    }
}

/* IN operator */
class InExpression(val left: Any?, val right: List<Any?>) : Predicate

infix fun Table.Column.inList(flag: List<Any?>): InExpression {
    return InExpression(this, flag)
}

/* Upsert */
inline fun <T : Table> Subject.Into<T>.upsert(insert: (T) -> UpsertData): UpsertStatement<T> {
    return UpsertStatement(insert(table), this)
}

class UpsertData(val assignments: Iterable<Assignment>, val updates: Iterable<Assignment>)

fun <T : Table> UpsertStatement<T>.execute() {
    getConnection().use { connection -> connection.createStatement().use { it.execute(this.toString()) } }
}

infix fun Iterable<Assignment>.to(second: Iterable<Assignment>): UpsertData = UpsertData(this, second)

/* Update */
fun <T : Table> UpdateStatement<T>.execute() {
    getConnection().use { connection -> connection.createStatement().use { it.execute(this.toString()) } }
}

/* Batch insert */
fun <T : Table> Subject.Into<T>.batchInsert(insert: (T) -> List<BatchValue>): BatchInsertStatement<T> {
    return BatchInsertStatement(insert(table), this)
}

operator fun Table.Column.invoke(vararg values: Any): BatchValue {
    return BatchValue(this, values)
}

operator fun Table.Column.invoke(values: List<Any>): BatchValue {
    return BatchValue(this, values)
}

operator fun List<BatchValue>.rangeTo(assignment: Assignment): List<BatchValue> {
    return this.plusElement(BatchValue(assignment.column, assignment.value))
}

operator fun Assignment.rangeTo(assignment: BatchValue): List<BatchValue> {
    return listOf(BatchValue(this.column, this.value), assignment)
}

operator fun BatchValue.rangeTo(assignment: BatchValue): List<BatchValue> {
    return listOf(this, assignment)
}

operator fun BatchValue.rangeTo(assignment: Assignment): List<BatchValue> {
    return listOf(this, BatchValue(assignment.column, assignment.value))
}

operator fun List<BatchValue>.rangeTo(assignment: BatchValue): List<BatchValue> {
    return this.plusElement(assignment)
}

class BatchValue(val column: Table.Column, vararg val values: Any?)

fun <T : Table> BatchInsertStatement<T>.execute() {
    getConnection().use { connection -> connection.createStatement().use { it.execute(this.toString()) } }
}

/* Delete */
fun <T : Table> DeleteStatement<T>.execute() {
    getConnection().use { connection -> connection.createStatement().use { it.execute(this.toString(MySQLDialect)) } }
}

/* Insert */
fun <T : Table> InsertStatement<T>.execute(): Long {
    return getConnection().use { connection ->
        val statement = connection.prepareStatement(this.toString(MySQLDialect), Statement.RETURN_GENERATED_KEYS)
        statement.executeUpdate()

        statement.generatedKeys.use {
            it.first()
            it.getLong(1)
        }
    }
}

/* Select */
val Table.Column.count: CountProjection
    get() = CountProjection(this)

class CountProjection(val column: Table.Column) : Projection

operator fun Projection.rangeTo(column: Table.Column): Iterable<Projection> {
    return listOf(this, column)
}

operator fun Iterable<Projection>.rangeTo(column: Table.Column): Iterable<Projection> {
    return this.plusElement(column)
}

operator fun Iterable<Table.Column>.rangeTo(projection: Projection): Iterable<Projection> {
    return this.plusElement(projection)
}


inline fun <T : Table, R> SelectStatement<T>.execute(mapping: T.(ResultRow) -> R): List<R> {
    val query = this.toString(MySQLDialect)
    val returnList = mutableListOf<R>()

    getConnection().use {
        val statement = it.createStatement()
        if (!statement.execute(query)) return emptyList()

        val results: ResultSet = statement.resultSet.apply { if (!first()) return emptyList() }

        while (!results.isAfterLast) {
            val map = mutableMapOf<String, Any?>()

            for (column in projection) {
                when (column) {
                    is Table.Column -> map[column.toString()] = getObjectAtColumn(results, column, this.subject.table)
                    is CountProjection -> map[column.toString()] = getObjectAtColumn(results, column)
                }
            }

            if (map.isNotEmpty()) {
                returnList.add(this.subject.table.mapping(ResultRow(map)))
            }

            results.next()
        }

        results.close()
    }

    return returnList
}

inline fun <T : Table, T2 : Table, R> Select2Statement<T, T2>.execute(mapping: (ResultRow) -> R): List<R> {
    val query = this.toString(MySQLDialect)
    return executeMultipleSelect(query, projection, mapping) { m, c, r ->
        m[c.toString()] = getObjectAtColumn(r, c, this.joinOn2Clause.subject.table, this.joinOn2Clause.table2)
    }
}

inline fun <T : Table, T2 : Table, T3 : Table, R> Select3Statement<T, T2, T3>.execute(mapping: (ResultRow) -> R): List<R> {
    val query = this.toString(MySQLDialect)
    return executeMultipleSelect(query, projection, mapping) { m, c, r ->
        m[c.toString()] = getObjectAtColumn(
                r,
                c,
                this.joinOn3Clause.joinOn2Clause.subject.table,
                this.joinOn3Clause.joinOn2Clause.table2,
                this.joinOn3Clause.table3
        )
    }
}

inline fun <T : Table, T2 : Table, T3 : Table, T4 : Table, R> Select4Statement<T, T2, T3, T4>.execute(mapping: (ResultRow) -> R): List<R> {
    val query = this.toString(MySQLDialect)
    return executeMultipleSelect(query, projection, mapping) { m, c, r ->
        m[c.toString()] = getObjectAtColumn(
                r,
                c,
                this.joinOn4Clause.joinOn3Clause.joinOn2Clause.subject.table,
                this.joinOn4Clause.joinOn3Clause.joinOn2Clause.table2,
                this.joinOn4Clause.joinOn3Clause.table3,
                this.joinOn4Clause.table4
        )
    }
}

@PublishedApi
internal inline fun <R> executeMultipleSelect(query: String,
                                              projection: Iterable<Projection>,
                                              mapping: (ResultRow) -> R,
                                              columnMapping: (MutableMap<String, Any?>, Table.Column, ResultSet) -> Unit): List<R> {
    val returnList = mutableListOf<R>()

    getConnection().use {
        val statement = it.createStatement()
        if (!statement.execute(query)) return emptyList()

        val results: ResultSet = statement.resultSet.apply { if (!first()) return emptyList() }

        while (!results.isAfterLast) {
            val map = mutableMapOf<String, Any?>()

            for (column in projection) {
                when (column) {
                    is Table.Column -> columnMapping(map, column, results)
                    is CountProjection -> map[column.toString()] = getObjectAtColumn(results, column)
                }
            }

            if (map.isNotEmpty()) {
                returnList.add(mapping(ResultRow(map)))
            }

            results.next()
        }
        results.close()
    }
    return returnList
}

/* General JDBC Utils */
@PublishedApi
internal fun getConnection(): Connection {
    val credentials = Secure.getDatabaseCredentials()

    val properties = Properties().apply {
        put("user", credentials.first)
        put("password", credentials.second)
    }

    //jdbc:mysql://ucloud.sql.regioit.intern:3306/leoapp
    Class.forName("com.mysql.jdbc.Driver")
    return DriverManager.getConnection("jdbc:mysql://localhost:3306/leoapp", properties)
}

@PublishedApi
internal fun getObjectAtColumn(results: ResultSet, column: Table.Column, vararg tables: Table): Any? {
    for (table in tables) {
        when (getTypeForColumnName(column.name, table)) {
            SQLType.STRING -> return results.getString(results.findColumn(column.name))
            SQLType.INTEGER -> return results.getInt(results.findColumn(column.name))
            SQLType.DATE -> return results.getDate(results.findColumn(column.name))
            SQLType.FLOAT -> return results.getFloat(results.findColumn(column.name))
            SQLType.DATETIME -> return results.getDate(results.findColumn(column.name))
            SQLType.LONG -> return results.getLong(results.findColumn(column.name))
            SQLType.UNKNOWN -> { /* just continue the loop */
            }
        }
    }
    return null
}

@PublishedApi
internal fun getObjectAtColumn(results: ResultSet, column: CountProjection): Any? {
    return results.getInt(results.findColumn(column.column.toCountString()))
}

@PublishedApi
internal fun <T : Table> getTypeForColumnName(s: String, t: T): SQLType {
    val tableClass = t::class.java

    tableClass.declaredFields.forEach {
        it.isAccessible = true

        with(it.get(t)) {
            if (this is Table.Column) {
                if (this.name == s) {
                    val annotations = tableClass.getDeclaredMethod("$s\$annotations").annotations

                    annotations.forEach { annotation ->
                        when (annotation.annotationClass.toString().split(" ")[1]) {
                            SQLText::class.java.toString().split(" ")[1],
                            SQLVarchar::class.java.toString().split(" ")[1] -> {
                                println("string")
                                return SQLType.STRING
                            }
                            SQLInteger::class.java.toString().split(" ")[1] -> {
                                println("int")
                                return SQLType.INTEGER
                            }
                            SQLDate::class.java.toString().split(" ")[1] -> {
                                println("date")
                                return SQLType.DATE
                            }
                            SQLDatetime::class.java.toString().split(" ")[1] -> {
                                println("datetime")
                                return SQLType.DATETIME
                            }
                            SQLFloat::class.java.toString().split(" ")[1] -> {
                                println("float")
                                return SQLType.FLOAT
                            }
                            SQLLong::class.java.toString().split(" ")[1] -> {
                                println("long")
                                return SQLType.LONG
                            }
                        }
                    }
                }
            }
        }

        it.isAccessible = false
    }
    return SQLType.UNKNOWN
}

@Suppress("unchecked_cast")
class ResultRow(private val data: Map<String, Any?>) {
    operator fun <T> get(i: Table.Column): T {
        return data[i.toString()] as T
    }

    operator fun <T> get(i: CountProjection): T {
        return data[i.column.toCountString()] as T
    }
}

fun Date.toSQLFormat(): String {
    val df = SimpleDateFormat("yyyy-MM-dd")
    return df.format(this)
}

fun Table.Column.toCountString() = "c_$this"

@PublishedApi
internal enum class SQLType { STRING, INTEGER, DATE, DATETIME, LONG, FLOAT, UNKNOWN }