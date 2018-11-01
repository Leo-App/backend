package de.slg.leoapp.module.survey

import de.slg.leoapp.checkAuthorized
import de.slg.leoapp.module.survey.data.PostSurvey
import de.slg.leoapp.respondError
import de.slg.leoapp.respondSuccess
import de.slg.leoapp.utils.TaskResponse
import de.slg.leoapp.utils.parseJSON
import io.ktor.application.call
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post

fun Route.survey() {
    //get a list of surveys
    get("/survey") {
        if (!call.request.checkAuthorized()) return@get
        call.respond(SurveyTask.getSurveys())
    }

    //get information about a survey by id
    get("/survey/{id}") {
        if (!call.request.checkAuthorized()) return@get

        val id = call.parameters["id"]

        if (id == null || id.toIntOrNull() == null) {
            call.respondError(400, "You need to use a valid survey id")
            return@get
        }

        val survey = SurveyTask.getSurveyInformation(id.toInt())
        if (survey == null) call.respondError(400, "Bad request")
        else call.respond(survey)
    }

    //add a new survey
    post("/survey") {
        if (!call.request.checkAuthorized()) return@post

        val data = call.receiveText().parseJSON<PostSurvey>()

        if (data == null)  {
            call.respondError(400, "Bad request")
            return@post
        }

        val status: TaskResponse = SurveyTask.addNewSurvey(data)

        when(status) {
            TaskResponse.SUCCESS -> call.respondSuccess()
            TaskResponse.GENERIC_ERROR -> call.respondError(400, "Bad Request")
            else -> call.respondSuccess(false)
        }
    }

    //delete a survey with specific id
    delete("/survey/{id}") {
        if (!call.request.checkAuthorized()) return@delete

        val id = call.parameters["id"]

        if (id == null || id.toIntOrNull() == null) {
            call.respondError(400, "You need to use a valid survey id")
            return@delete
        }

        SurveyTask.deleteSurvey(id.toInt())
        call.respondSuccess()
    }
}