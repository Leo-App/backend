package de.slg.leoapp.module.user

import de.slg.leoapp.checkAuthorized
import de.slg.leoapp.module.user.data.PostArbitraryId
import de.slg.leoapp.module.user.data.PostChecksum
import de.slg.leoapp.module.user.data.PostUser
import de.slg.leoapp.module.user.data.TaskResponse
import de.slg.leoapp.respondError
import de.slg.leoapp.respondSuccess
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post

fun Route.user() {
    //list all registered users
    get("/user") {
        if (!call.request.checkAuthorized()) return@get
        call.respond(UserTask.getAllUsers())
    }

    //get user by identifier
    get("/user/{id}") {
        if (!call.request.checkAuthorized()) return@get
        val id = call.parameters["id"]

        if (id == null) {
            call.respondError(400, "You need to use a valid user id")
            return@get
        }

        call.respond(UserTask.getUserByIdentifier(id))
    }

    //get the ids of survey answers the user has voted for
    get("user/{id}/votes") {
        if (!call.request.checkAuthorized()) return@get
        val id = call.parameters["id"]

        if (id == null) {
            call.respondError(400, "You need to use a valid user id")
            return@get
        }

        call.respond(UserTask.getUserSurveyVotes(id))
    }

    //register a new vote for the specified user for an answer id
    post("user/{id}/votes") {
        if (!call.request.checkAuthorized()) return@post
        val id = call.parameters["id"]

        if (id == null) {
            call.respondError(400, "You need to use a valid user id")
            return@post
        }

        val data = call.receive<PostArbitraryId>()
        UserTask.registerSurveyVote(id, data)
        call.respondSuccess()
    }

    //add new user device. If the user with id is not yet known, a new one will be registered. This is the only way to
    //add new users! No need to authorize.
    post("/user/{id}/device") {
        val id = call.parameters["id"]

        if (id == null) {
            call.respondError(400, "You need to use a valid user id")
            return@post
        }

        val checksum = call.receive<PostChecksum>()
        val taskStatus: TaskResponse = UserTask.addDeviceOrRegister(id, checksum)

        when(taskStatus) {
            TaskResponse.SUCCESS -> call.respondSuccess()
            TaskResponse.CHECKSUM_INVALID -> call.respondError(400, "The provided checksum is invalid")
            TaskResponse.GENERIC_ERROR -> call.respondError(400, "Bad request")
        }
    }

    //edit editable user data, namely grade and username
    post("/user/{id}") {
        if (!call.request.checkAuthorized()) return@post
        val id = call.parameters["id"]

        if (id == null) {
            call.respondError(400, "You need to use a valid user id")
            return@post
        }

        val user = call.receive<PostUser>()
        UserTask.updateUser(id, user)
        call.respondSuccess()
    }

    //blocks the access to API functionality TODO add secondary authentication, maybe registration checksum?
    delete("/user/{id}/device/{device}") {
        val id = call.parameters["id"]
        val device = call.parameters["device"]

        if (id == null || device == null) {
            call.respondError(400, "You need to use a valid user- and device id")
            return@delete
        }

        UserTask.blockDeviceForUser(id, device)
        call.respondSuccess()
    }
}