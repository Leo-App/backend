package de.slg.leoapp.module.news.data

data class Entry(val id: Int, val title: String, val content: String, val views: Int, val valid_until: Long, val attachment: String)