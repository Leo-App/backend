package de.slg.leoapp.module.news

import de.slg.leoapp.respondError
import de.slg.leoapp.respondSuccess
import de.slg.leoapp.utils.parseJSON
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post

fun Route.news() {

    //get an entry by its id
    get("/news/{id}") {
        val id = call.parameters["id"]

        if (id == null || id.toIntOrNull() == null) {
            call.respondError(400, "You need to use a valid entry id")
            return@get
        }

        val entry = NewsTask.getEntryById(id.toInt()) ?: return@get
        call.respond(entry)
    }

    //add a new entry or update its "editable" properties if id is supplied
    post("/news") {
        val success = NewsTask.addOrUpdateEntry(call.receiveText().parseJSON())
        if (!success) {
            call.respondError(400, "Invalid request body")
            return@post
        }

        call.respondSuccess()
    }

    //delete the entry with {id}
    delete("/news/{id}") {
        val id = call.parameters["id"]

        if (id == null || id.toIntOrNull() == null) {
            call.respondError(400, "You need to use a valid entry id")
            return@delete
        }

        NewsTask.deleteEntryWithId(id.toInt())
        call.respondSuccess()
    }

}