package de.slg.leoapp.module.survey.data

data class PostSurvey(val author: Int,
                      val title: String,
                      val description: String,
                      val recipient: String,
                      val multiple: Boolean,
                      val answers: List<String>)
