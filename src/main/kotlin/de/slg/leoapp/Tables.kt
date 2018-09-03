package de.slg.leoapp

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object Users : Table("users") {
    val id: Column<Int> = integer("id").primaryKey().autoIncrement()
    val firstName: Column<String> = varchar("first_name", 255)
    val lastName: Column<String> = varchar("last_name", 255)
    val defaultname: Column<String> = varchar("name", 255)
    val grade: Column<String> = varchar("grade", 255)
    val permission: Column<Int> = integer("permission")
    val createdate = date("createdate")
}

object UserVotes : Table("user_votes") {
    val user: Column<Int> = integer("user").primaryKey()
    val answerId: Column<Int> = integer("answer_id").primaryKey()
}

object Devices : Table("devices") {
    val identifier: Column<String> = varchar("identifier", 255)
    val user: Column<Int> = integer("user")
    val checksum: Column<String> = varchar("checksum", 255)
    val timestamp = datetime("timestamp")
}

object Surveys : Table("surveys") {
    val author: Column<Int> = integer("author").primaryKey()
    val title: Column<String> = varchar("title", 255)
    val content: Column<String> = text("content")
    val multiple: Column<Boolean> = bool("multiple")
    val createdate = date("createdate")
}

object Answers : Table("answers") {
    val id: Column<Int> = integer("id").primaryKey().autoIncrement()
    val survey: Column<Int> = integer("survey")
    val content: Column<String> = varchar("content", 255)
}

object SurveyRecipients : Table("survey_recipients") {
    val user: Column<Int> = integer("user").primaryKey()
    val survey: Column<Int> = integer("survey").primaryKey()
    val custom: Column<Boolean> = bool("custom")
}

object Entries : IntIdTable("entries") {
    val author: Column<Int> = integer("author")
    val title: Column<String> = varchar("title", 255)
    val content: Column<String> = text("content")
    val views: Column<Int> = integer("views")
    val deadline = date("valid_until")
    val attachment: Column<String> = varchar("attachment", 255)
}

object EntryRecipients : Table("entry_recipients") {
    val user: Column<Int> = integer("user").primaryKey()
    val entry: Column<Int> = integer("survey").primaryKey()
    val custom: Column<Boolean> = bool("custom")
}

object FeatureUsage : Table("feature_usage") {
    val user: Column<Int> = integer("user").primaryKey()
    val feature: Column<Long> = long("feature").primaryKey()
    val interactions: Column<Int> = integer("interactions")
    val averageTime: Column<Float> = float("average_time")
}
