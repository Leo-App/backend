package de.slg.leoapp.module.user

import de.slg.leoapp.PATH_TO_PROFILE_PICTURE
import de.slg.leoapp.annotation.Editable
import de.slg.leoapp.db.*
import de.slg.leoapp.module.user.data.*
import de.slg.leoapp.utils.Secure
import de.slg.leoapp.utils.TaskResponse
import de.slg.leoapp.utils.toFile
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import tel.egram.kuery.*
import tel.egram.kuery.dml.Assignment
import java.util.*

object UserTask {

    fun getAllUsers(): List<User> {
        return from(Users).select {
            it.id..it.firstName..it.lastName..it.defaultname..it.grade..it.permission..it.createdate
        }.execute { User(it[id], it[firstName], it[lastName], it[defaultname], it[grade], it[permission], it[createdate]) }
    }

    fun getUserSurveyVotes(id: String): Map<*, *> {
        var numericId = id.toIntOrNull()
        if (numericId == null) numericId = getIdFromDefaultName(id)

        val ids = from(UserVotes).where { it.user eq numericId }.orderBy { it.answerId.asc }.select { it.answerId }.execute {
            val survey: Int = it[user]
            survey
        }

        return mapOf("votes" to ids)
    }

    fun getUserByIdentifier(id: String): User? {
        val users = from(Users)
                .where { if (id.toIntOrNull() == null) Users.defaultname eq id else Users.id eq id.toInt() }
                .select { it.id..it.firstName..it.lastName..it.defaultname..it.grade..it.permission..it.createdate }
                .execute { User(it[this.id], it[firstName], it[lastName], it[defaultname], it[grade], it[permission], it[createdate]) }

        return if (users.isEmpty()) null else users[0]
    }

    fun registerSurveyVote(id: String, answerId: PostArbitraryId) {
        var numericId = id.toIntOrNull()
        if (numericId == null) numericId = getIdFromDefaultName(id)
        if (numericId == -1) return

        into(UserVotes).insert { it.user(numericId)..it.answerId(answerId.id!!) }.execute()
    }

    fun registerSurveyVotes(id: String, postIds: PostArbitraryIds) {
        var numericId = id.toIntOrNull()
        if (numericId == null) numericId = getIdFromDefaultName(id)
        if (numericId == -1) return

        into(UserVotes).batchInsert { it.user(arrayOf(numericId))..it.answerId(values = postIds.ids!!) }.execute()
    }

    fun addDeviceOrRegister(id: String, deviceChecksum: PostDeviceChecksum): TaskResponse {
        var username = id
        if (username.toIntOrNull() != null) {
            username = getDefaultNameFromId(id.toInt()) ?: return TaskResponse.ID_INVALID
        }

        if (deviceChecksum.checksum == null || deviceChecksum.device == null) return TaskResponse.GENERIC_ERROR
        if (!Secure.isUserChecksumValid(username, deviceChecksum.checksum)) return TaskResponse.CHECKSUM_INVALID

        val numericId = id.toIntOrNull() ?: getIdFromDefaultName(username)

        if (numericId == -1) {
            into(Users).insert {
                it.defaultname(username)..it.createdate(Date().toSQLFormat())..it.permission(2 * (6 / username.length))
            }.execute()
        }

        into(Devices).insert {
            it.user(numericId)..it.checksum(deviceChecksum.checksum)..it.identifier(deviceChecksum.device)..it.timestamp(Date().toSQLFormat())
        }.execute()

        return TaskResponse.SUCCESS
    }

    fun blockDeviceForUser(user: String, device: String) {
        var numericId = user.toIntOrNull()
        if (numericId == null) numericId = getIdFromDefaultName(user)

        from(Devices).where { (it.identifier eq device) and (it.user eq numericId) }.delete().execute()
    }

    fun updateUser(id: String, user: PostUser) { //if that works first try, you can call me reflection god

        val updateFields = mutableMapOf<Table.Column, Any>()
        //Here, we are making sure only fields marked with @Editable are edited
        for (cur in user.javaClass.declaredFields) {
            val annotation = cur.getAnnotation(Editable::class.java)
            if (annotation != null) {
                @Suppress("unchecked_cast")
                updateFields[Users.javaClass.getField(annotation.databaseName).get(Users) as Table.Column] = cur.get(user)
            } else {
                @Suppress("unchecked_cast")
                updateFields[Users.javaClass.getField(cur.name).get(Users) as Table.Column] = cur.get(user)
            }
        }

        from(Users)
                .where { if (id.toIntOrNull() != null) Users.id eq id.toInt() else Users.defaultname eq id }
                .update { _ ->
                    val iterable = mutableListOf<Assignment>()
                    updateFields.forEach {
                        when (it.value) {
                            is Number -> iterable.add(it.key.invoke(it.value as Number))
                            is String -> iterable.add(it.key.invoke(it.value as String))
                            is Boolean -> iterable.add(it.key.invoke(it.value as Boolean))
                        }
                    }
                    iterable
                }
                .execute()

    }

    fun getUserFeatureUsage(id: String): List<FeatureStatistics> {
        var numericId = id.toIntOrNull()
        if (numericId == null) numericId = getIdFromDefaultName(id)

        return from(FeatureUsage)
                .where { FeatureUsage.user eq numericId }
                .orderBy { it.averageTime.desc }
                .select { it.user..it.averageTime..it.feature..it.interactions }
                .execute { FeatureStatistics(it[feature], it[interactions], it[averageTime]) }
    }

    fun logNewFeatureInteraction(id: String, body: PostFeatureUsage) {
        var numericId = id.toIntOrNull()
        if (numericId == null) numericId = getIdFromDefaultName(id)

        val average = from(FeatureUsage)
                .where { (it.user eq numericId) and (it.feature eq body.featureId!!) }
                .select { it.interactions..it.averageTime }
                .execute { Pair<Float, Int>(it[averageTime], it[interactions]) }

        if (average.isEmpty()) average.plusElement(Pair(0, 0)) //this is not necessary but better safe than sorry

        into(FeatureUsage)
                .upsert { it.user(numericId)..it.feature(body.featureId!!)..it.averageTime(body.time!!)..it.interactions(1) to
                        it.interactions(average[0].second + 1)..it.averageTime((average[0].first * average[0].second + body.time) / (average[0].second + 1))
                }
                .execute()
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
        val ids = from(Users).where { it.defaultname eq name }.select { it.id }.execute { it.get<Int>(id) }
        return if (ids.isEmpty()) -1 else ids[0]
    }

    private fun getDefaultNameFromId(id: Int): String? {
        val ids: List<String> = from(Users).where { it.id eq id }.select { it.defaultname }.execute {
            val name: String = it[defaultname]
            name
        }

        return if (ids.isEmpty()) null else ids[0]
    }
}