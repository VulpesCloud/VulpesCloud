package de.vulpescloud.node.command.annotation

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Description(
    val description: String,
    val translatable: Boolean = true
)