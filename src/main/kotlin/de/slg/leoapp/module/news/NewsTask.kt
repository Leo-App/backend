package de.slg.leoapp.module.news

import de.slg.leoapp.annotation.EditIndicator
import de.slg.leoapp.annotation.Editable
import de.slg.leoapp.annotation.Optional
import de.slg.leoapp.db.*
import de.slg.leoapp.module.news.data.Entry
import de.slg.leoapp.module.news.data.PostEntry
import de.slg.leoapp.module.news.data.TargetOption
import de.slg.leoapp.utils.getColumnForFieldName
import tel.egram.kuery.*
import tel.egram.kuery.dml.Assignment
import java.util.Date

object NewsTask {

    fun getEntryById(id: Int): Entry? {
        val entryList = from(Entries)
                .where { it.id eq id }
                .select { it.author..it.title..it.content..it.views..it.deadline..it.attachment }
                .execute { Entry(id, it[author], it[title], it[content], it[views], it.get<Date>(deadline).time, it[attachment]) }

        return if (entryList.isNotEmpty()) entryList[0] else null
    }

    fun getEntriesForUser(id: String): List<Entry> {
        return if (id.toIntOrNull() == null) {
            from(Entries)
                    .join(EntryRecipients)
                    .on { entries, entryRecipients -> entries.id eq entryRecipients.entry }
                    .join(Users).on { _, entryRecipients, users -> entryRecipients.user eq users.id }
                    .where { _, _, users -> users.defaultname eq id }
                    .select { entries, _, _ -> entries.id..entries.author..entries.title..entries.content..entries.views..entries.deadline..entries.attachment }
                    .execute {
                        Entry(
                                it[Entries.id],
                                it[Entries.author],
                                it[Entries.title],
                                it[Entries.content],
                                it[Entries.views],
                                it.get<Date>(Entries.deadline).time,
                                it[Entries.attachment]
                        )
                    }
        } else {
            from(Entries)
                    .join(EntryRecipients)
                    .on { entries, entryRecipients -> entries.id eq entryRecipients.entry }
                    .join(Users).on { _, entryRecipients, users -> entryRecipients.user eq users.id }
                    .where { _, _, users -> users.id eq id.toInt() }
                    .select { entries, _, _ -> entries.id..entries.author..entries.title..entries.content..entries.views..entries.deadline..entries.attachment }
                    .execute {
                        Entry(
                                it[Entries.id],
                                it[Entries.author],
                                it[Entries.title],
                                it[Entries.content],
                                it[Entries.views],
                                it.get<Date>(Entries.deadline).time,
                                it[Entries.attachment]
                        )
                    }
        }
    }

    fun addOrUpdateEntry(entry: PostEntry?): Boolean {

        if (entry == null) return false

        val editIndicator = getEditIndicator(entry)

        if (editIndicator == null) { //editIndicator is null, so we want to add a new entry

            //Checking if all necessary fields are filled
            for (cur in entry.javaClass.fields) {
                if (cur.getAnnotation(EditIndicator::class.java) == null && cur.get(entry) == null) {
                    val annotation = cur.getAnnotation(Optional::class.java) ?: return false
                    if (annotation.replacement != "") {
                        val replacement = entry.javaClass.getField(annotation.replacement)
                        if (replacement?.get(entry) == null) {
                            return false
                        }
                    }
                }
            }

            val id = into(Entries).insert { it.title(entry.title)..it.content(entry.content)..it.deadline(Date(entry.deadline!!).toSQLFormat())..it.author(entry.author) }.execute()

            val isCustomRecipient: Boolean

            val recipients: List<Int> = if (entry.recipient != null) {
                val targetedGrades = getTargetedGrades(entry.recipient)
                isCustomRecipient = false

                from(Users)
                        .where { Users.grade inList targetedGrades }
                        .select { it.id }
                        .execute { it.get<Long>(Entries.id).toInt() }
            } else {
                isCustomRecipient = true
                entry.recipients ?: emptyList()
            }

            into(EntryRecipients)
                    .batchInsert { it.user(recipients)..it.entry(id)..it.custom(isCustomRecipient) }
                    .execute()
        } else {

            val updateList = mutableListOf<Assignment>()

            for (cur in entry.javaClass.fields) {
                val annotation = cur.getAnnotation(Editable::class.java) ?: continue
                val field = cur.get(entry) ?: continue

                if (annotation.databaseName != "") {
                    updateList.add(Assignment.Value(Entries.getColumnForFieldName(annotation.databaseName)!!, field))
                } else {
                    updateList.add(Assignment.Value(Entries.getColumnForFieldName(cur.name)!!, field))
                }
            }

            from(Users)
                    .where { editIndicator.first eq editIndicator.second }
                    .update{ updateList }
                    .execute()
        }

        return true
    }

    fun deleteEntryWithId(id: Int) {
        from(Entries).where { it.id eq id }.delete().execute()
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

    private fun getEditIndicator(entry: PostEntry): Pair<Table.Column, Any>? {
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

    private infix fun Table.Column.eq(flag: Any): EqExpression {
        return EqExpression(this, flag)
    }


}