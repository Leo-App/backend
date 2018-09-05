package de.slg.leoapp.module.survey.data

import de.slg.leoapp.annotation.Optional

data class PostSurvey(val author: Int,
                      val title: String,
                      val description: String,
                      @Optional("recipients") val recipient: String?,
                      @Optional("recipient") val recipients: List<Int>?,
                      val multiple: Boolean,
                      val answers: List<String>)
