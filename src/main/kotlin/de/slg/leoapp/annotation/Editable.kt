package de.slg.leoapp.annotation

/**
 * Marks a field in a dataset representation object as editable, meaning it can be edited after creation via api endpoints
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Editable(val databaseName: String = "")