package de.vulpescloud.node.command.annotations


@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Description(
    val description: String,
    val translatable: Boolean = true
)