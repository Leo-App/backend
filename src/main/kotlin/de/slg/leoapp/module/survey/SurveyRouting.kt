package de.slg.leoapp.module.survey

import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post

fun Route.survey() {
    //get a list of surveys
    get("/survey") {


    }

    //get information about a survey by id
    get("/survey/{id}") {


    }

    //add a new survey
    post("/survey") {


    }

    //delete a survey with specific id
    delete("/survey/{id}") {

    }
}