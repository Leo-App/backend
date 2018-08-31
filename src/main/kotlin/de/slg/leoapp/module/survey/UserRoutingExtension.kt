package de.slg.leoapp.module.survey

import de.slg.leoapp.respondError
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get

/**
 * Adds the possibility to query all surveys that are currently relevant for a specific user, identified by {id}.
 */
fun Route.userExtensionSurvey() {
    //get a list of all surveys relevant for me
    get("user/{id}/surveys") {
        val id = call.parameters["id"]

        if (id == null) {
            call.respondError(400, "You need to supply a valid user id")
            return@get
        }

        call.respond(SurveyTask.getSurveysForUser(id))
    }

}