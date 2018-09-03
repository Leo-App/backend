package de.slg.leoapp.module.user.data

import com.fasterxml.jackson.annotation.JsonProperty

data class User(val id: Int,
                @JsonProperty("first_name") val firstName: String,
                @JsonProperty("last_name") val lastName: String,
                val defaultname: String,
                val grade: String,
                val permission: Int,
                val createdate: Long)