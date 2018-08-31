package de.slg.leoapp.module.news.data

import de.slg.leoapp.annotation.EditIndicator
import de.slg.leoapp.annotation.Editable
import de.slg.leoapp.annotation.Optional

data class PostEntry(@EditIndicator val id: Int?,
                     val title: String?,
                     @Editable val content: String?,
                     @Optional("recipients") val recipient: String?,
                     @Optional("recipient") val recipients: List<Int>?,
                     val author: Int,
                     @Editable val deadline: Long?)