package de.slg.leoapp.module.user

import de.slg.leoapp.*
import de.slg.leoapp.annotation.Editable
import de.slg.leoapp.module.user.data.*
import io.ktor.content.MultiPartData
import io.ktor.content.PartData
import org.jetbrains.exposed.sql.*
import org.joda.time.DateTime

object UserTask {

    fun getAllUsers(): List<User> {
        return runOnDatabase {
            Users.selectAll().map {
                User(it[Users.id], it[Users.firstName], it[Users.lastName], it[Users.defaultname], it[Users.grade], it[Users.permission], it[Users.createdate].millis)
            }
        }
    }

    fun getUserSurveyVotes(id: String): Map<*, *> {
        var numericId = id.toIntOrNull()
        if (numericId == null) numericId = getIdFromDefaultName(id)

        val ids = runOnDatabase {
            UserVotes.slice(UserVotes.answerId).select { UserVotes.user eq numericId }.orderBy(UserVotes.answerId).map { it[UserVotes.answerId] }
        }
        return mapOf("votes" to ids)
    }

    fun getUserByIdentifier(id: String): User? {
        val users = runOnDatabase {
            Users.select { if (id.toIntOrNull() == null) Users.defaultname eq id else Users.id eq id.toInt() }.map {
                User(it[Users.id], it[Users.firstName], it[Users.lastName], it[Users.defaultname], it[Users.grade], it[Users.permission], it[Users.createdate].millis)
            }
        }

        return if (users.isEmpty()) null else users[0]
    }

    fun registerSurveyVote(id: String, answerId: PostArbitraryId) {
        var numericId = id.toIntOrNull()
        if (numericId == null) numericId = getIdFromDefaultName(id)
        if (numericId == -1) return

        runOnDatabase {
            UserVotes.insert {
                it[UserVotes.user] = numericId
                it[UserVotes.answerId] = answerId.id!!
            }
        }
    }

    fun registerSurveyVotes(id: String, postIds: PostArbitraryIds) {
        var numericId = id.toIntOrNull()
        if (numericId == null) numericId = getIdFromDefaultName(id)
        if (numericId == -1) return

        runOnDatabase {
            UserVotes.batchInsert(postIds.ids!!) { id ->
                this[UserVotes.answerId] = id
                this[UserVotes.user] = numericId
            }
        }
    }

    fun addDeviceOrRegister(id: String, deviceChecksum: PostDeviceChecksum): TaskResponse {
        var username = id
        if (username.toIntOrNull() != null) {
            username = getDefaultNameFromId(id.toInt()) ?: return TaskResponse.ID_INVALID
        }
        if (deviceChecksum.checksum == null || deviceChecksum.device == null) return TaskResponse.GENERIC_ERROR
        if (!Secure.isUserChecksumValid(username, deviceChecksum.checksum)) return TaskResponse.CHECKSUM_INVALID

        var numericId = id.toIntOrNull() ?: getIdFromDefaultName(username)

        runOnDatabase {

            if (numericId == -1) {
                Users.insert {
                    it[Users.defaultname] = username
                    it[Users.createdate] = DateTime.now()
                    it[Users.permission] = 2 * (6 / username.length)
                }

                numericId = Users.slice(Users.id).select { Users.defaultname eq username }.map { it[Users.id] }[0]
            }

            Devices.insert {
                it[Devices.user] = numericId
                it[Devices.checksum] = deviceChecksum.checksum
                it[Devices.identifier] = deviceChecksum.device
                it[Devices.timestamp] = DateTime.now()
            }
        }

        return TaskResponse.SUCCESS
    }

    fun blockDeviceForUser(user: String, device: String) {
        var numericId = user.toIntOrNull()
        if (numericId == null) numericId = getIdFromDefaultName(user)

        runOnDatabase {
            Devices.deleteWhere { (Devices.identifier eq device) and (Devices.user eq numericId) }
        }
    }

    fun updateUser(id: String, user: PostUser) { //if that works first try, you can call me reflection god

        val updateFields = mutableMapOf<Column<Any>, Any>()
        //Here, we are making sure only fields marked with @Editable are edited
        for (cur in user.javaClass.declaredFields) {
            val annotation = cur.getAnnotation(Editable::class.java)
            if (annotation != null) {
                @Suppress("unchecked_cast")
                updateFields[Users.javaClass.getField(annotation.databaseName).get(Users) as Column<Any>] = cur.get(user)
            } else {
                @Suppress("unchecked_cast")
                updateFields[Users.javaClass.getField(cur.name).get(Users) as Column<Any>] = cur.get(user)
            }
        }

        runOnDatabase {
            Users.update(where = { if (id.toIntOrNull() != null) Users.id eq id.toInt() else Users.defaultname eq id },
                    body = {
                        for (cur in updateFields) {
                            it[cur.key] = cur.value
                        }
                    })
        }
    }

    suspend fun setProfilePictureForUser(id: String, multipart: MultiPartData) {
        var numericId = id.toIntOrNull()

        if (numericId == null) numericId = getIdFromDefaultName(id)

        while (true) {
            val part = multipart.readPart() ?: break
            if (part is PartData.FileItem) {
                part.streamProvider.invoke().use { input ->
                    input.toFile(PATH_TO_PROFILE_PICTURE.format(numericId))
                }
            }
        }
    }

    private fun getIdFromDefaultName(name: String): Int {
        val ids = runOnDatabase {
            Users.slice(Users.id).select { Users.defaultname eq name }.map { it[Users.id] }
        }

        return if (ids.isEmpty()) -1 else ids[0]
    }

    private fun getDefaultNameFromId(id: Int): String? {
        val ids = runOnDatabase {
            Users.slice(Users.defaultname).select { Users.id eq id }.map { it[Users.defaultname] }
        }

        return if (ids.isEmpty()) null else ids[0]
    }

}