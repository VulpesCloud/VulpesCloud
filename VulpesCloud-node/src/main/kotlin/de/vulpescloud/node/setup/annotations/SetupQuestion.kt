package de.vulpescloud.node.setup.annotations

import de.vulpescloud.node.setup.answers.NullSetupAnswer
import de.vulpescloud.node.setup.answers.SetupAnswer
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SetupQuestion(
    val index: Int,
    val translationKey: String,
    val answer: KClass<out SetupAnswer> = NullSetupAnswer::class,
    val forceAnswer: Boolean = false,
    val default: Array<String> = []
)
