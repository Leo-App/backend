package de.slg.leoapp.module.user

import de.slg.leoapp.TaskResponse
import de.slg.leoapp.module.user.data.*

object UserTask {

    fun getAllUsers(): List<User> {
        TODO()
    }

    fun getUserSurveyVotes(id: String) {
        TODO()
    }

    fun getUserByIdentifier(id: String): User {
        TODO()
    }

    fun registerSurveyVote(id: String, answerId: PostArbitraryId) {
        TODO()
    }

    fun addDeviceOrRegister(id: String, checksum: PostChecksum): TaskResponse {
        TODO()
    }

    fun blockDeviceForUser(user: String, device: String) {
        TODO()
    }

    fun updateUser(id: String, user: PostUser) {
        TODO()
    }

}