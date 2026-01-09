package de.vulpescloud.node.setup

import de.vulpescloud.node.setup.annotations.SetupQuestion
import java.lang.reflect.Method
import java.lang.reflect.Parameter

data class SetupQuestionInfo(
    val setupQuestion: SetupQuestion,
    val method: Method,
    val parameter: Parameter
)