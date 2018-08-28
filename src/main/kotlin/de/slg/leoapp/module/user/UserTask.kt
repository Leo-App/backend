package de.slg.leoapp.module.user

import de.slg.leoapp.Secure
import de.slg.leoapp.TaskResponse
import de.slg.leoapp.module.user.data.*
import org.jetbrains.exposed.sql.Database

object UserTask {

    private val database: Database

    init {
        val credentials = Secure.getDatabaseCredentials()

        database = Database
                .connect("jdbc:mysql://ucloud.sql.regioit.intern:3306/leoapp",
                        driver = "com.mysql.jdbc.Driver",
                        user = credentials.first,
                        password = credentials.second)
    }

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