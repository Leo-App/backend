package de.slg.leoapp.module.user

import de.slg.leoapp.checkAuthorized
import de.slg.leoapp.module.user.data.*
import de.slg.leoapp.respondError
import de.slg.leoapp.respondSuccess
import de.slg.leoapp.utils.TaskResponse
import de.slg.leoapp.utils.parseJSON
import io.ktor.application.call
import io.ktor.request.isMultipart
import io.ktor.request.receive
import io.ktor.request.receiveMultipart
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post

fun Route.user() {

    //list all registered Users
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

        val user = UserTask.getUserByIdentifier(id)
        if (user == null) {
            call.respondError(400, "$id is not a valid user identifier")
            return@get
        }

        call.respond(user)
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

    //returns the feature usage for a specific user
    get("user/{id}/features") {
        if (!call.request.checkAuthorized()) return@get
        val id = call.parameters["id"]

        if (id == null) {
            call.respondError(400, "You need to use a valid user id")
            return@get
        }

        call.respond(UserTask.getUserFeatureUsage(id))
    }

    //log a new feature interaction for user {id}
    post("user/{id}/features") {
        if (!call.request.checkAuthorized()) return@post
        val id = call.parameters["id"]

        if (id == null) {
            call.respondError(400, "You need to use a valid user id")
            return@post
        }

        val body = call.receiveText().parseJSON<PostFeatureUsage>()

        if (body?.featureId == null || body.time == null) {
            call.respondError(400, "Bad Request")
            return@post
        }

        UserTask.logNewFeatureInteraction(id, body)

        call.respondSuccess()
    }

    //register a new vote / votes for the specified user for an answer id
    post("user/{id}/votes") {
        if (!call.request.checkAuthorized()) return@post
        val id = call.parameters["id"]

        if (id == null) {
            call.respondError(400, "You need to use a valid user id")
            return@post
        }

        val postId = call.receiveText().parseJSON<PostArbitraryId>()

        if (postId?.id != null) {
            UserTask.registerSurveyVote(id, postId)
        } else {
            val postIds = call.receiveText().parseJSON<PostArbitraryIds>()
            if (postIds?.ids == null) {
                call.respondError(400, "Bad request")
                return@post
            }
            UserTask.registerSurveyVotes(id, postIds)
        }
        call.respondSuccess()
    }

    //add new user device. If the user with id is not yet known, a new one will be registered. This is the only way to
    //add new Users! No need to authorize.
    post("/user/{id}/device") {
        val id = call.parameters["id"]

        if (id == null) {
            call.respondError(400, "You need to use a valid user id")
            return@post
        }

        println("id: $id")

        val checksum = call.receiveText().parseJSON<PostDeviceChecksum>()

        if (checksum == null) {
            call.respondError(400, "Bad request")
            return@post
        }

        println("checksum ${checksum.checksum}, device: ${checksum.device}")
        val taskStatus: TaskResponse = UserTask.addDeviceOrRegister(id, checksum)

        when(taskStatus) {
            TaskResponse.SUCCESS -> call.respondSuccess()
            TaskResponse.CHECKSUM_INVALID -> call.respondError(400, "The provided checksum is invalid")
            TaskResponse.GENERIC_ERROR -> call.respondError(400, "Bad request")
            TaskResponse.ID_INVALID -> call.respondError(400, "The provided id does not exist yet")
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

        val user = call.receiveText().parseJSON<PostUser>()

        if (user == null) {
            call.respondError(400, "Bad Request")
            return@post
        }

        UserTask.updateUser(id, user)
        call.respondSuccess()
    }

    //set the profilepicture of the user with id
    post("/user/{id}/picture") {
        val id = call.parameters["id"]

        if (id == null) {
            call.respondError(400, "You need to use a valid user id")
            return@post
        }

        if (!call.request.isMultipart()) {
            call.respondError(400, "Bad request")
            return@post
        }

        UserTask.setProfilePictureForUser(id, call.receiveMultipart())

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