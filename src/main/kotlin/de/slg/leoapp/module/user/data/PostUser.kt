package de.slg.leoapp.module.user.data

import de.slg.leoapp.annotation.Editable

data class PostUser(@Editable val grade: String?,
                    @Editable val firstName: String?,
                    @Editable val lastName: String?,
                    @Editable val permission: Int?)