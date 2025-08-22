package de.vulpescloud.node.setup.annotations

annotation class SetupQuestion(
    val index: Int,
    val question: String,
    val answers: Array<String> = []
)
