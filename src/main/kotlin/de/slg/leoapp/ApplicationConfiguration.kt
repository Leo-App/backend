package de.slg.leoapp

import com.fasterxml.jackson.databind.SerializationFeature
import de.slg.leoapp.module.news.news
import de.slg.leoapp.module.news.userExtensionNews
import de.slg.leoapp.module.survey.survey
import de.slg.leoapp.module.survey.userExtensionSurvey
import de.slg.leoapp.module.user.user
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.ApplicationRequest
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.route


const val MEDIA_LOCATION = "/media"
const val PATH_TO_PROFILE_PICTURE = "$MEDIA_LOCATION/usr_%d_pp"

//DISCLAIMER: The API only logs important errors, negligible errors like redundant json fields, trying to change non existing
//data, etc. get dropped. This may change in the future, but for now take that into account when debugging.

/**
 * In this function we register all modules which should be available at api runtime. Normally a module is responsible for management of
 * one top level path, e.g. "/user". However it is possible to "extend" a module if it helps the logical structure of the api codebase
 * (An example would be the option to list relevant news for a specific user "/user/{id}/entries": Here we provided the implementation
 * in the news module).
 */
fun Application.main() {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
    install(Routing) {
        route("/api") {
            user()
            userExtensionNews()
            userExtensionSurvey()
            survey()
            news()
        }
    }
}

suspend fun ApplicationCall.respondSuccess(value: Boolean = true) = respond("""{"success": "$value"}""")
suspend fun ApplicationCall.respondError(code: Int, message: String) {
    response.status(HttpStatusCode.fromValue(code))
    respond(mapOf("error" to mapOf("code" to code, "message" to message)))
}

suspend fun ApplicationRequest.checkAuthorized(): Boolean {
  /*  val device = header("device")
    val auth = header("authentication")
    if (device == null || auth == null) {
        call.respondError(401, "Not authorized")
        return false
    }
    if (!Secure.isAuthorized(device, auth)) {
        call.respondError(401, "Not authorized")
        return false
    } */
    return true
}