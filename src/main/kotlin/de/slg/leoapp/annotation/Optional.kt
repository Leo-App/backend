package de.slg.leoapp.annotation

/**
 * Marks a field as optional for creation. It is possible to provide the name of a replacement field (i.e. if the optional field is not set
 * the replacement field has to be set).
 */
annotation class Optional(val replacement: String = "")