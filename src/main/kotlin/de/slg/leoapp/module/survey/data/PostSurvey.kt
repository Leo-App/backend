package de.slg.leoapp.module.survey.data

import java.util.*

data class PostSurvey(val author: Int, val title: String, val description: String, val recipient: String, val multiple: Boolean, val answers: Array<String>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PostSurvey

        if (author != other.author) return false
        if (title != other.title) return false
        if (description != other.description) return false
        if (recipient != other.recipient) return false
        if (multiple != other.multiple) return false
        if (!Arrays.equals(answers, other.answers)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = author
        result = 31 * result + title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + recipient.hashCode()
        result = 31 * result + multiple.hashCode()
        result = 31 * result + Arrays.hashCode(answers)
        return result
    }
}