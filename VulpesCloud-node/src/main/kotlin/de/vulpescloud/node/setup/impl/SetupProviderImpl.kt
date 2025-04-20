package de.vulpescloud.node.setup.impl

import de.vulpescloud.api.lang.Translator
import de.vulpescloud.node.setup.Setup
import de.vulpescloud.node.setup.SetupInfo
import de.vulpescloud.node.setup.SetupProvider
import de.vulpescloud.node.setup.SetupQuestionInfo
import de.vulpescloud.node.setup.annotations.SetupCancel
import de.vulpescloud.node.setup.annotations.SetupFinish
import de.vulpescloud.node.setup.annotations.SetupQuestion
import de.vulpescloud.node.terminal.JLineTerminal
import java.util.*
import org.slf4j.LoggerFactory

class SetupProviderImpl(private val terminal: JLineTerminal, private val translator: Translator) :
    SetupProvider {

    private val logger = LoggerFactory.getLogger(SetupProvider::class.java)
    override var currentSetup: SetupInfo? = null
    private var currentQuestion: SetupQuestionInfo? = null
    private var currentQuestionIndex = 0

    override fun startSetup(setup: Setup) {
        val questions = mutableListOf<SetupQuestionInfo>()
        val methods =
            setup::class.java.methods.filter { it.isAnnotationPresent(SetupQuestion::class.java) }
        methods.forEach {
            check(it.parameters.size == 1) {
                "Function has @SetupQuestion annotation must have 1 parameter!"
            }
            questions.add(
                SetupQuestionInfo(it.getAnnotation(SetupQuestion::class.java), it, it.parameters[0])
            )
        }

        val finishMethods = methods.filter { it.isAnnotationPresent(SetupFinish::class.java) }
        val cancelMethods = methods.filter { it.isAnnotationPresent(SetupCancel::class.java) }
        check(finishMethods.size <= 1) {
            "There can only be one Function with the @SetupFinish annotation!"
        }
        check(cancelMethods.size <= 1) {
            "There can only be one Function with the @SetupCancel annotation!"
        }
        val finishMethod = finishMethods.firstOrNull()!!
        val cancelMethod = cancelMethods.firstOrNull()!!

        val setupInfo =
            SetupInfo(
                setup,
                finishMethod,
                cancelMethod,
                questions.sortedBy { it.setupQuestion.index },
            )

        if (currentSetup == null) {
            currentSetup = setupInfo
            this.currentQuestion = setupInfo.questions[currentQuestionIndex]
        } else {
            return
        }

        terminal.clear()
        terminal.printSetup(translator.trans("SETUP.GENERAL.STARTED"))
        printCurrentQuestion()
    }

    private fun printCurrentQuestion() {
        val currentQuestion =
            this.currentQuestion ?: throw IllegalStateException("There is no setup at the moment")
        val questionSetupAnswer = currentQuestion.setupQuestion.answer.java.newInstance()
        val answers = questionSetupAnswer.suggest()
        val suffix =
            if (answers.isNotEmpty()) "&ePossible answers: " + answers.joinToString() else ""
        if (suffix.isEmpty()) {
            terminal.printSetup(translator.trans(currentQuestion.setupQuestion.translationKey))
            terminal.printSetup("")
        } else {
            terminal.printSetup(translator.trans(currentQuestion.setupQuestion.translationKey))
            terminal.printSetup(suffix)
            terminal.printSetup("")
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
        terminal.printSetup("")
        printCurrentQuestion()
    }

    private fun nextQuestionExists(setup: SetupInfo) =
        this.currentQuestionIndex + 1 in setup.questions.indices

    private fun finishSetup() {
        terminal.clear()
        this.currentQuestion = null
        this.currentQuestionIndex = 0
        this.currentSetup!!.callFinish()
        this.currentSetup = null
        logger.info("Setup &2Finished")
    }

    override fun cancelSetup() {
        terminal.clear()
        this.currentSetup?.callCancel()
        this.currentSetup = null
        this.currentQuestion = null
        this.currentQuestionIndex = 0
        logger.info("Setup &cCancelled")
    }

    override fun input(input: String) {
        val currentQuestion = this.currentQuestion ?: return
        val answers = currentQuestion.setupQuestion.answer.java.newInstance().suggest()
        if (
            answers.isNotEmpty() &&
                !answers.contains(input) &&
                currentQuestion.setupQuestion.forceAnswer
        ) {
            terminal.printSetup("&cInvalid Response!")
            return
        }
        val invoke =
            try {
                currentQuestion.method.invoke(this.currentSetup!!.setup, input)
            } catch (e: Exception) {
                terminal.printSetup("&cInvalid Response!")
                return
            }
        if (invoke is Boolean && invoke == false) {
            return
        }
        prepareNextQuestion()
    }

    override fun getSetupAnswers(input: String): List<String> {
        if (currentSetup == null) {
            throw IllegalStateException("There is no Setup running!")
        }
        val answers = currentQuestion!!.setupQuestion.answer.java.newInstance().suggest()
        if (currentQuestion!!.setupQuestion.default.isNotEmpty()) {
            val mList = answers.toMutableList()
            mList.addAll(currentQuestion!!.setupQuestion.default)

            return mList.filter {
                it.lowercase(Locale.getDefault()).startsWith(input.lowercase(Locale.getDefault()))
            }
        }

        return answers.filter {
            it.lowercase(Locale.getDefault()).startsWith(input.lowercase(Locale.getDefault()))
        }
    }
}
