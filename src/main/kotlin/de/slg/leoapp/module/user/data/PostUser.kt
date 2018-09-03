package de.slg.leoapp.module.user.data

import com.fasterxml.jackson.annotation.JsonProperty
import de.slg.leoapp.annotation.Editable

data class PostUser(@Editable val grade: String?,
                    @JsonProperty("first_name") @Editable val firstName: String?,
                    @JsonProperty("last_name") @Editable val lastName: String?,
                    @Editable val permission: Int?)