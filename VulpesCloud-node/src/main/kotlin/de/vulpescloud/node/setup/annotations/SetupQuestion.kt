package de.vulpescloud.node.setup.annotations

import de.vulpescloud.node.setup.answer.NullSetupAnswer
import de.vulpescloud.node.setup.answer.SetupAnswer
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SetupQuestion(
    val number: Int,
    val message: String,
    val answer: KClass<out SetupAnswer> = NullSetupAnswer::class
)
