package de.slg.leoapp.module.news.data

import de.slg.leoapp.annotation.EditIndicator
import de.slg.leoapp.annotation.Editable

data class PostEntry(@EditIndicator val id: Int?,
                     val title: String?,
                     @Editable val content: String?,
                     val recipient: String?,
                     val recipients: List<Int>?,
                     val author: Int,
                     @Editable val deadline: Long?)