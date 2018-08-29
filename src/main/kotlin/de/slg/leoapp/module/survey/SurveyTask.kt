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
                    .map { Survey.Answer(it[Answers.id], it[Answers.content], it[countAlias]) } //todo check if that works

            Surveys.select { Surveys.author eq id }.map {
                Survey(it[Surveys.author], name, it[Surveys.title], it[Surveys.content],
                        it[Surveys.multiple], it[Surveys.createdate].millis, matchingAnswers)
            }

        }

        return if (surveyList.isNotEmpty()) surveyList[0] else null
    }

    fun getSurveys(): List<Survey> {
        TODO()
    }

    fun getSurveysForUser(id: Int): List<Survey> {
        TODO()
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

}