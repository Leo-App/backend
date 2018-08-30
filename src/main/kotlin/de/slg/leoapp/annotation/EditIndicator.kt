package de.slg.leoapp.annotation

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class EditIndicator(val databaseName: String = "")