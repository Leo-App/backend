package de.slg.leoapp.annotation

/**
 * Marks a field in a dataset representation object as an "edit-indicator". For some ambiguous endpoints, it is necessary
 * to determine whether a new dataset should be created or an existing one updated. If the the field marked as "edit-indicator"
 * is supplied in the respective post body, the request is interpreted as update, otherwise a new dataset will be created.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class EditIndicator(val databaseName: String = "")