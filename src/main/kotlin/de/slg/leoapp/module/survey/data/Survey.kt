package de.slg.leoapp.module.survey.data

data class Survey(val id: Int,
                  val author: String,
                  val title: String,
                  val description: String,
                  val multiple: Boolean,
                  val createdate: Long,
                  val answers: List<Answer>) {

    data class Answer(val id: Int, val content: String, val votes: Int)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Survey

        if (id != other.id) return false
        if (author != other.author) return false
        if (title != other.title) return false
        if (description != other.description) return false
        if (multiple != other.multiple) return false
        if (createdate != other.createdate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + author.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + multiple.hashCode()
        result = 31 * result + createdate.hashCode()
        return result
    }
}