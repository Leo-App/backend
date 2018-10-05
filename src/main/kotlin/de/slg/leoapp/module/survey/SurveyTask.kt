package de.slg.leoapp.module.survey

import de.slg.leoapp.db.*
import de.slg.leoapp.module.survey.data.PostSurvey
import de.slg.leoapp.module.survey.data.Survey
import de.slg.leoapp.utils.TaskResponse
import tel.egram.kuery.eq
import tel.egram.kuery.from
import tel.egram.kuery.into
import tel.egram.kuery.rangeTo
import java.util.*

object SurveyTask {

    fun getSurveyInformation(id: Int): Survey? {
        val matchingUsers = from(Users)
                .where { it.id eq id }
                .select { it.firstName..it.lastName }
                .execute { "${it.get<String>(firstName)} ${it.get<String>(lastName)}" }

        val name = if (matchingUsers.isNotEmpty()) matchingUsers[0] else "null"

        val matchingAnswers = from(Answers)
                .join(UserVotes).on { answers, userVotes -> answers.id eq userVotes.answerId }
                .where { answers, _ -> answers.id eq id }
                .groupBy { _, userVotes -> userVotes.answerId }
                .select { answers, userVotes -> userVotes.answerId.count..answers.id..answers.content }
                .execute { Survey.Answer(it[Answers.id], it[Answers.content], it[UserVotes.answerId.count]) }

        val surveyList = from(Surveys)
                .where { it.author eq id }
                .select { it.author..it.title..it.content..it.multiple..it.createdate }
                .execute {
                    Survey(it[author], name, it[title], it[content], it.get<Int>(multiple) == 1, it.get<Date>(createdate).time, matchingAnswers)
                }

        return if (surveyList.isNotEmpty()) surveyList[0] else null
    }

    fun getSurveys(): List<Survey> {

        data class SurveyData(val id: Int,
                              val author: String,
                              val title: String,
                              val description: String,
                              val multiple: Boolean,
                              val createdate: Long)

        val surveys = from(Surveys)
                .join(Users)
                .on { surveys, users -> surveys.author eq users.id }
                .orderBy { surveys, _ -> surveys.createdate.desc }
                .select { surveys, users -> surveys.author..users.firstName..users.lastName..surveys.title..surveys.content..surveys.multiple..surveys.createdate }
                .execute {
                    SurveyData(it[Surveys.author], it.get<String>(Users.firstName) + " " + it.get<String>(Users.lastName),
                            it[Surveys.title], it[Surveys.content], it[Surveys.multiple], it.get<Date>(Surveys.createdate).time)
                }

        val surveyListing = mutableListOf<Survey>()

        for (cur in surveys) {
            val matchingAnswers = from(Answers)
                    .join(UserVotes)
                    .on { answers, userVotes -> answers.id eq userVotes.answerId }
                    .where { answers, _ -> answers.survey eq cur.id }
                    .groupBy { _, userVotes -> userVotes.answerId }
                    .select { answers, userVotes -> answers.id..answers.content..userVotes.answerId.count }
                    .execute { Survey.Answer(it[Answers.id], it[Answers.content], it[UserVotes.answerId.count]) }

            surveyListing.add(Survey(cur.id, cur.author, cur.title, cur.description, cur.multiple, cur.createdate, matchingAnswers))
        }

        return surveyListing
    }


    fun getSurveysForUser(id: String): List<Survey> {
        var numericId = id.toIntOrNull()
        if (numericId == null) numericId = getIdFromDefaultName(id)

        data class SurveyData(val id: Int,
                              val author: String,
                              val title: String,
                              val description: String,
                              val multiple: Boolean,
                              val createdate: Long)

        val matchingSurveys = from(Surveys)
                .join(SurveyRecipients)
                .on { surveys, surveyRecipients -> surveys.author eq surveyRecipients.survey }
                .join(Users)
                .on { _, surveyRecipients, users -> surveyRecipients.user eq users.id }
                .where { _, surveyRecipients, _ -> surveyRecipients.user eq numericId }
                .select { surveys, _, users -> surveys.author..users.firstName..users.lastName..surveys.title..surveys.content..surveys.multiple..surveys.createdate }
                .execute {
                    SurveyData(
                            it[Surveys.author],
                            it.get<String>(Users.firstName) + " " + it.get<String>(Users.lastName),
                            it[Surveys.title],
                            it[Surveys.content],
                            it[Surveys.multiple],
                            it.get<Date>(Surveys.createdate).time
                    )
                }

        val surveyListing = mutableListOf<Survey>()

        for (cur in matchingSurveys) {

            val matchingAnswers = from(Answers)
                    .join(UserVotes)
                    .on { answers, userVotes -> answers.id eq userVotes.answerId }
                    .where { answers, _ -> answers.survey eq cur.id }
                    .groupBy { _, userVotes -> userVotes.answerId }
                    .select { answers, userVotes -> answers.id..answers.content..userVotes.answerId.count }
                    .execute { Survey.Answer(it[Answers.id], it[Answers.content], it[UserVotes.answerId.count]) }

            surveyListing.add(Survey(cur.id, cur.author, cur.title, cur.description, cur.multiple, cur.createdate, matchingAnswers))
        }

        return surveyListing
    }

    fun addNewSurvey(data: PostSurvey): TaskResponse {

        into(Surveys)
                .insert { it.author(data.author)..it.title(data.title)..it.content(data.description)..it.multiple(data.multiple)..it.createdate(Date().toSQLFormat()) }
                .execute()

        into(Answers)
                .batchInsert { it.survey(data.author)..it.content(data.answers) }
                .execute()

        return TaskResponse.SUCCESS
    }

    fun deleteSurvey(id: Int) {
        from(Surveys).where { it.author eq id }.delete().execute()
    }

    private fun getIdFromDefaultName(name: String): Int {
        val ids = from(Users).where { it.defaultname eq name }.select { it.id }.execute { it.get<Int>(id) }
        return if (ids.isEmpty()) -1 else ids[0]
    }

}