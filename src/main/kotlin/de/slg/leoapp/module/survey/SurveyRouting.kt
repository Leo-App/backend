package de.slg.leoapp.module.survey

import de.slg.leoapp.checkAuthorized
import de.slg.leoapp.module.survey.data.PostSurvey
import de.slg.leoapp.TaskResponse
import de.slg.leoapp.respondError
import de.slg.leoapp.respondSuccess
import io.ktor.application.call
import io.ktor.request.receive
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

        call.respond(SurveyTask.getSurveyInformation(id.toInt()))
    }

    //add a new survey
    post("/survey") {
        if (!call.request.checkAuthorized()) return@post

        val data = call.receive<PostSurvey>()
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

        SurveyTask.deleteSurvey(id)
        call.respondSuccess()
    }
}