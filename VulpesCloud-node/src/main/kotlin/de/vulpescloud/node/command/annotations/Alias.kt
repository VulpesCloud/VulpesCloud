package de.vulpescloud.node.command.annotations

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Alias(
    val alias: Array<String> = []
)
