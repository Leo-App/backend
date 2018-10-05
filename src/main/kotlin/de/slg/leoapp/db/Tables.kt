package de.slg.leoapp.db

import de.slg.leoapp.annotation.*
import tel.egram.kuery.Table

@SQLText
object Users : Table("users") {
    @SQLInteger val id = Column("id")
    @SQLVarchar val firstName = Column("first_name")
    @SQLVarchar val lastName = Column("last_name")
    @SQLVarchar val defaultname = Column("name")
    @SQLVarchar val grade = Column("grade")
    @SQLInteger val permission = Column("permission")
    @SQLDate val createdate = Column("createdate")
}

object UserVotes : Table("user_votes") {
    @SQLInteger val user = Column("user")
    @SQLInteger val answerId = Column("answer_id")
}

object Devices : Table("devices") {
    @SQLVarchar val identifier = Column("identifier")
    @SQLInteger val user = Column("user")
    @SQLVarchar val checksum = Column("checksum")
    @SQLDatetime val timestamp = Column("timestamp")
}

object Surveys : Table("surveys") {
    @SQLInteger val author = Column("author")
    @SQLVarchar val title = Column("title")
    @SQLText val content = Column("content")
    @SQLInteger val multiple = Column("multiple")
    @SQLDate val createdate = Column("createdate")
}

object Answers : Table("answers") {
    @SQLInteger val id = Column("id")
    @SQLInteger val survey = Column("survey")
    @SQLVarchar val content = Column("content")
}

object SurveyRecipients : Table("survey_recipients") {
    @SQLInteger val user = Column("user")
    @SQLInteger val survey = Column("survey")
    @SQLInteger val custom = Column("custom")
}

object Entries : Table("entries") {
    @SQLInteger val id = Column("id")
    @SQLInteger val author = Column("author")
    @SQLVarchar val title = Column("title")
    @SQLText val content = Column("content")
    @SQLInteger val views = Column("views")
    @SQLDate val deadline = Column("valid_until")
    @SQLVarchar val attachment = Column("attachment")
}

object EntryRecipients : Table("entry_recipients") {
    @SQLInteger val user = Column("user")
    @SQLInteger val entry = Column("survey")
    @SQLInteger val custom = Column("custom")
}

object FeatureUsage : Table("feature_usage") {
    @SQLInteger val user = Column("user")
    @SQLLong val feature = Column("feature")
    @SQLInteger val interactions = Column("interactions")
    @SQLFloat val averageTime = Column("average_time")
}
