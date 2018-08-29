package de.slg.leoapp.module.user.data

data class User(val id: Int,
                val firstName: String,
                val lastName: String,
                val defaultname: String,
                val grade: String,
                val permission: Int,
                val createdate: Long)