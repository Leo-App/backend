package de.slg.leoapp.module.news

import de.slg.leoapp.respondError
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.Route

fun Route.userExtensionNews() {
    //get all currently relevant entries for the user with {id}
    get("/user/{id}/news") {
        val id = call.parameters["id"]

        if (id == null || id.toIntOrNull() == null) {
            call.respondError(400, "You need to supply a valid user id")
            return@get
        }

        call.respond(NewsTask.getEntriesForUser(id.toInt()))
    }
}