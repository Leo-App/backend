package de.slg.leoapp.module.news

import de.slg.leoapp.*
import de.slg.leoapp.annotation.EditIndicator
import de.slg.leoapp.annotation.Editable
import de.slg.leoapp.module.news.data.Entry
import de.slg.leoapp.module.news.data.PostEntry
import de.slg.leoapp.module.news.data.TargetOption
import org.jetbrains.exposed.sql.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

object NewsTask {

    fun getEntryById(id: Int): Entry? {
        TODO()
    }

    fun getEntriesForUser(id: String): List<Entry> {
        TODO()
    }

    fun addOrUpdateEntry(entry: PostEntry): Boolean {
        val editIndicator = getEditIndicator(entry)

        return runOnDatabase {
            if (editIndicator == null) {

                for (cur in entry.javaClass.fields) {
                    if (cur.getAnnotation(EditIndicator::class.java) == null && cur.get(entry) == null) return@runOnDatabase false
                }

                val id = Entries.insertAndGetId {
                    it[Entries.title] = entry.title!!
                    it[Entries.content] = entry.content!!
                    it[Entries.deadline] = DateTime(entry.deadline, DateTimeZone.getDefault())
                    it[Entries.author] = entry.author
                }

                val recipients: List<Int> = if (entry.recipient != null) {
                    val targetedGrades = getTargetedGrades(entry.recipient)
                    Users.slice(Users.id).select {
                        Users.grade inList targetedGrades
                    }.map { it[Users.id] }
                } else {
                    entry.recipients ?: emptyList()
                }

                EntryRecipients.batchInsert(recipients) { input ->
                    this[EntryRecipients.user] = input
                    this[EntryRecipients.entry] = id.value
                }

            } else {
                Entries.update(where = { editIndicator.first eq editIndicator.second }, body = {
                    for (cur in entry.javaClass.fields) {
                        val annotation = cur.getAnnotation(Editable::class.java) ?: continue

                        if (annotation.databaseName != "") {
                            it[Entries.getColumnForFieldName(annotation.databaseName)!!] = cur.get(entry)
                        } else {
                            it[Entries.getColumnForFieldName(cur.name)!!] = cur.get(entry)
                        }
                    }
                })
            }

            true
        }

    }

    fun deleteEntryWithId(id: Int) {
        runOnDatabase {
            Entries.deleteWhere {
                Entries.id eq id
            }
        }
    }

    private fun getTargetedGrades(recipient: String): List<String> {
        try {
            val target = TargetOption.valueOf("T$recipient")

            return when (target) {
                TargetOption.T5,
                TargetOption.T6,
                TargetOption.T7,
                TargetOption.T8,
                TargetOption.T9,
                TargetOption.T10,
                TargetOption.TEF,
                TargetOption.TQ1,
                TargetOption.TQ2 -> listOf(recipient)
                TargetOption.TSEK1 -> listOf("5", "6", "7", "8", "9", "10") //10 For G9
                TargetOption.TSEK2 -> listOf("EF", "Q1", "Q2")
                TargetOption.TALL -> listOf("5", "6", "7", "8", "9", "10", "EF", "Q1", "Q2")
            }
        } catch (e: IllegalArgumentException) {
            return emptyList()
        }
    }

    private fun getEditIndicator(entry: PostEntry): Pair<Column<Any>, Any>? {
        for (cur in entry.javaClass.fields) {
            val annotation = cur.getAnnotation(EditIndicator::class.java)

            if (annotation != null) {
                return if (annotation.databaseName != "")
                    Pair(Entries.getColumnForFieldName(annotation.databaseName)!!, cur.get(entry))
                else
                    Pair(Entries.getColumnForFieldName(cur.name)!!, cur.get(entry))
            }
        }
        return null
    }

}