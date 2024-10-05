package io.github.thecguygithub.node.terminal.setup

data class SetupQuestion(
    val answerKey: String,
    val question: String,
    val possibleAnswers: (Map<String, String>) -> List<String>,
    val predicate: ((Pair<String, Map<String, String>>) -> Boolean)? = null
)