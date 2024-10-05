package io.github.thecguygithub.node.terminal.setup

import io.github.thecguygithub.api.Named
import io.github.thecguygithub.node.Node

abstract class Setup(
    val name: String
) : Named {

    private var index: Int = 0
    private val questions: MutableList<SetupQuestion> = ArrayList()
    private val answers: MutableMap<String, String> = HashMap()

    fun question(
        answerKey: String,
        question: String,
        possibleAnswers: (Map<String, String>) -> List<String> = { emptyList() },
        prediction: (Pair<String, Map<String, String>>) -> Boolean
    ) {
        questions.add(SetupQuestion(answerKey, question, possibleAnswers, prediction))
    }

    abstract fun complete(context: Map<String, String>)

    fun displayQuestion(remark: String? = null) {
        val terminal = Node.terminal!!
        terminal.updatePrompt("&8» &7")
        terminal.clear()

        val currentQuestion = question()
        terminal.printLine("&b$name &8- &7Question &8(&7${index + 1}&8/&7${questions.size}&8) &7${currentQuestion.question}")

        val possibleAnswers = currentQuestion.possibleAnswers(answers)
        if (possibleAnswers.isNotEmpty()) {
            terminal.printLine("&7Possible answers&8: &f${possibleAnswers.joinToString("&8, &f")}")
        }

        answers[currentQuestion.answerKey]?.let { previousAnswer ->
            terminal.printLine("&7The previous response was&8: &f$previousAnswer")
        }

        remark?.let {
            terminal.printLine(it)
        }

        terminal.printLine(" ")
        terminal.printLine("Enter &8'&7exit&8' &7to leave the setup or enter &8'&7back&8' &7to see the previous question&8!")
    }

    fun run() {
        Node.terminal!!.setup = this
        displayQuestion()
    }

    fun possibleAnswers(): List<String> = question().possibleAnswers(answers)

    fun question(): SetupQuestion = questions[index]

    fun answer(answer: String) {
        val currentQuestion = question()
        if (!currentQuestion.predicate?.let { it(Pair(answer, answers)) }!!) {
            displayQuestion("&cThe given answer is not correct.")
            return
        }

        answers[currentQuestion.answerKey] = answer
        index++

        if (index >= questions.size) {
            exit(true)
            return
        }

        displayQuestion()
    }

    fun previousQuestion() {
        if (index > 0) index--
        displayQuestion()
    }

    fun exit(completed: Boolean) {
        val terminal = Node.terminal!!
        terminal.setup = null
        terminal.clear()

        if (completed) {
            complete(answers)
        }
    }
}

