package de.slg.leoapp.module.survey

import io.ktor.routing.Route
import io.ktor.routing.get

fun Route.userExtensionSurvey() {
    //get a list of all surveys relevant for me
    get("user/{id}/surveys") {

    }
}