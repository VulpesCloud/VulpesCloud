package de.vulpescloud.node.setup

import de.vulpescloud.node.Node
import de.vulpescloud.node.setup.annotations.SetupCancel
import de.vulpescloud.node.setup.annotations.SetupFinish
import de.vulpescloud.node.setup.annotations.SetupQuestion
import de.vulpescloud.node.terminal.ConsoleAppender
import org.slf4j.LoggerFactory
import java.lang.reflect.Method
import java.lang.reflect.Parameter

class SetupProvider {

    private val logger = LoggerFactory.getLogger(SetupProvider::class.java)
    var currentSetup: SetupInfo? = null
    var currentQuestion: SetupQuestionInfo? = null
        private set
    private var currentQuestionIndex = 0

    fun startSetup(setup: Setup) {
        val questions = ArrayList<SetupQuestionInfo>()
        val methods = setup::class.java.methods
        methods.filter { it.isAnnotationPresent(SetupQuestion::class.java) }.forEach {
            check(it.parameters.size == 1) { "Function has @SetupQuestion annotation must have 1 parameter!" }
            questions.add(
                SetupQuestionInfo(
                    it.getAnnotation(SetupQuestion::class.java),
                    it,
                    it.parameters[0]
                )
            )
        }

        val finishMethods = methods.filter { it.isAnnotationPresent(SetupFinish::class.java) }
        val cancelMethods = methods.filter { it.isAnnotationPresent(SetupCancel::class.java) }
        check(finishMethods.size <= 1) { "There can only be one Function with the @SetupFinish annotation!" }
        check(cancelMethods.size <= 1) { "There can only be one Function with the @SetupCancel annotation!" }
        val finishMethod = finishMethods.firstOrNull()
        val cancelMethod = cancelMethods.firstOrNull()

        val sInfo = SetupInfo(setup, finishMethod, cancelMethod, questions.sortedBy { it.setupQuestion.number })

        if (this.currentSetup == null) {
            this.currentSetup = sInfo
            this.currentQuestion = sInfo.questions[currentQuestionIndex]

            Node.terminal!!.clear()
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.all.started"))
            printCurrentQuestion()
        } else {
            logger.warn("Trying to start a Setup whilst another Setup is running!")
        }
    }

    private fun printCurrentQuestion() {
        val currentQuestion = this.currentQuestion ?: throw IllegalStateException("There is no setup at the moment")
        val questionSetupAnswer = currentQuestion.setupQuestion.answer.java.newInstance()
        val answers = questionSetupAnswer.suggest()
        val suffix = if (answers.isNotEmpty()) "&ePossible answers: " + answers.joinToString() else ""
        if (suffix.isEmpty()) {
            Node.terminal!!.printSetup(Node.languageProvider.translate(currentQuestion.setupQuestion.message))
            Node.terminal!!.printSetup("")
        } else {
            Node.terminal!!.printSetup(Node.languageProvider.translate(currentQuestion.setupQuestion.message))
            Node.terminal!!.printSetup(suffix)
            Node.terminal!!.printSetup("")
        }
    }

    private fun prepareNextQuestion() {
        val setup = this.currentSetup ?: return
        if (!nextQuestionExists(setup)) {
            finishSetup()
            return
        }

        this.currentQuestionIndex++
        this.currentQuestion = setup.questions[currentQuestionIndex]
        Node.terminal!!.printSetup("")
        printCurrentQuestion()
    }

    private fun nextQuestionExists(setup: SetupInfo) = this.currentQuestionIndex + 1 in setup.questions.indices

    private fun finishSetup() {
        Node.terminal!!.clear()
        this.currentSetup?.callFinish()
        this.currentSetup = null
        this.currentQuestion = null
        this.currentQuestionIndex = 0
        logger.info("Setup &2Finished")
    }

    fun cancelSetup() {
        Node.terminal!!.clear()
        this.currentSetup?.callCancel()
        this.currentSetup = null
        this.currentQuestion = null
        this.currentQuestionIndex = 0
        logger.info("Setup &cCancelled")
    }

    fun input(input: String) {
        val currentQuestion = this.currentQuestion ?: return
        val answers = currentQuestion.setupQuestion.answer.java.newInstance().suggest()
        if (answers.isNotEmpty() && !answers.contains(input)) {
            Node.terminal!!.printSetup("&cInvalid Response!")
            return
        }
        val invoke = try {
            currentQuestion.method.invoke(this.currentSetup!!.setup, input)
        } catch (e: Exception) {
            Node.terminal!!.printSetup("&cInvalid Response!")
            return
        }
        if (invoke is Boolean && invoke == false) {
            return
        }
        prepareNextQuestion()
    }


    class SetupInfo(
        val setup: Setup,
        val finishMethod: Method?,
        val cancelMethod: Method?,
        val questions: List<SetupQuestionInfo>
    ) {
        fun callFinish() {
            finishMethod?.invoke(setup)
        }
        fun callCancel() {
            cancelMethod?.invoke(setup)
        }
    }
    class SetupQuestionInfo(
        val setupQuestion: SetupQuestion,
        val method: Method,
        val parameter: Parameter
    )

}