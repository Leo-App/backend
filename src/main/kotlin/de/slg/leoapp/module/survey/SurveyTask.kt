package de.slg.leoapp.module.survey

import de.slg.leoapp.*
import de.slg.leoapp.module.survey.data.PostSurvey
import de.slg.leoapp.module.survey.data.Survey
import org.jetbrains.exposed.sql.*
import org.joda.time.DateTime

object SurveyTask {

    fun getSurveyInformation(id: Int): Survey? {
        val surveyList = runOnDatabase {

            val matchingUsers = UserVotes.slice(Users.firstName, Users.lastName)
                    .select { Users.id eq id }
                    .map { "${it[Users.firstName]} ${it[Users.lastName]}" }

            val name = if (matchingUsers.isNotEmpty()) matchingUsers[0] else "null"

            val countAlias = UserVotes.answerId.count().alias("votes")

            val matchingAnswers = (Answers innerJoin UserVotes).slice(Answers.id, Answers.content, countAlias)
                    .select { (Answers.id eq UserVotes.answerId) and (Answers.survey eq id) }
                    .groupBy(UserVotes.answerId)
                    .map { Survey.Answer(it[Answers.id], it[Answers.content], it[countAlias]) }

            Surveys.select { Surveys.author eq id }.map {
                Survey(it[Surveys.author], name, it[Surveys.title], it[Surveys.content],
                        it[Surveys.multiple], it[Surveys.createdate].millis, matchingAnswers)
            }

        }

        return if (surveyList.isNotEmpty()) surveyList[0] else null
    }

    fun getSurveys(): List<Survey> {
        return runOnDatabase {

            data class SurveyData(val id: Int,
                                  val author: String,
                                  val title: String,
                                  val description: String,
                                  val multiple: Boolean,
                                  val createdate: Long)

            val surveys = (Surveys innerJoin Users).select { Surveys.author eq Users.id }.orderBy(Surveys.createdate to false)
                    .map {
                        SurveyData(it[Surveys.author], it[Users.firstName] + " " + it[Users.lastName],
                                it[Surveys.title], it[Surveys.content], it[Surveys.multiple], it[Surveys.createdate].millis)
                    }

            val surveyListing = mutableListOf<Survey>()

            for (cur in surveys) {
                val countAlias = UserVotes.answerId.count().alias("votes")

                val matchingAnswers = (Answers innerJoin UserVotes).slice(Answers.id, Answers.content, countAlias)
                        .select { (Answers.id eq UserVotes.answerId) and (Answers.survey eq cur.id) }
                        .groupBy(UserVotes.answerId)
                        .map { Survey.Answer(it[Answers.id], it[Answers.content], it[countAlias]) }

                surveyListing.add(Survey(cur.id, cur.author, cur.title, cur.description, cur.multiple, cur.createdate, matchingAnswers))
            }

            surveyListing
        }

    }


    fun getSurveysForUser(id: String): List<Survey> {
        var numericId = id.toIntOrNull()
        if (numericId == null) numericId = getIdFromDefaultName(id)

        return runOnDatabase {

            data class SurveyData(val id: Int,
                                  val author: String,
                                  val title: String,
                                  val description: String,
                                  val multiple: Boolean,
                                  val createdate: Long)

            val matchingSurveys = (Surveys innerJoin SurveyRecipients innerJoin Users).select {
                (Surveys.author eq SurveyRecipients.survey) and (SurveyRecipients.user eq numericId) and (SurveyRecipients.user eq Users.id)
            }.map {
                SurveyData(it[Surveys.author],
                        it[Users.firstName] + " " + it[Users.lastName],
                        it[Surveys.title],
                        it[Surveys.content],
                        it[Surveys.multiple],
                        it[Surveys.createdate].millis)
            }

            val surveyListing = mutableListOf<Survey>()

            for (cur in matchingSurveys) {
                val countAlias = UserVotes.answerId.count().alias("votes")

                val matchingAnswers = (Answers innerJoin UserVotes).slice(Answers.id, Answers.content, countAlias)
                        .select { (Answers.id eq UserVotes.answerId) and (Answers.survey eq cur.id) }
                        .groupBy(UserVotes.answerId)
                        .map { Survey.Answer(it[Answers.id], it[Answers.content], it[countAlias]) }

                surveyListing.add(Survey(cur.id, cur.author, cur.title, cur.description, cur.multiple, cur.createdate, matchingAnswers))
            }

            surveyListing

        }

    }

    fun addNewSurvey(data: PostSurvey): TaskResponse {
        runOnDatabase {
            Surveys.insert {
                it[Surveys.author] = data.author
                it[Surveys.title] = data.title
                it[Surveys.content] = data.description
                it[Surveys.multiple] = data.multiple
                it[Surveys.createdate] = DateTime.now()
            }

            Answers.batchInsert(data.answers) { answer ->
                this[Answers.survey] = data.author
                this[Answers.content] = answer
            }
        }
        return TaskResponse.SUCCESS
    }

    fun deleteSurvey(id: Int) {
        runOnDatabase {
            Surveys.deleteWhere { Surveys.author eq id }
        }
    }

    private fun getIdFromDefaultName(name: String): Int {
        val ids = runOnDatabase {
            Users.slice(Users.id).select { Users.defaultname eq name }.map { it[Users.id] }
        }

        return if (ids.isEmpty()) -1 else ids[0]
    }

}