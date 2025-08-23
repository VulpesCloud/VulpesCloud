package de.vulpescloud.node.setup

import java.lang.reflect.Method

data class SetupInfo(
    val setup: Setup,
    private val finishMethod: Method?,
    private val cancelMethod: Method?,
    val questions: List<SetupQuestionInfo>
) {
    fun callFinish() {
        finishMethod?.invoke(setup)
    }
    fun callCancel() {
        cancelMethod?.invoke(setup)
    }
}