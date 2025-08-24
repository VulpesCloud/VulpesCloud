package de.vulpescloud.node.setup

import de.vulpescloud.node.setup.annotations.SetupCancel
import de.vulpescloud.node.setup.annotations.SetupFinish
import de.vulpescloud.node.setup.annotations.SetupQuestion
import de.vulpescloud.node.terminal.Terminal
import kotlinx.coroutines.delay
import org.jline.utils.InfoCmp
import org.slf4j.LoggerFactory
import java.util.*

class SetupProvider(private val terminal: Terminal) {

    private val logger = LoggerFactory.getLogger(SetupProvider::class.java)
    var currentSetup: SetupInfo? = null
    private var currentQuestion: SetupQuestionInfo? = null
    private var currentQuestionIndex = 0
    private var oldTerminalInput = terminal.terminalContent
    private var inputLocked = false
    private var lockedInputCount = 0

    fun startSetup(setup: Setup) {
        try {
            val questions = mutableListOf<SetupQuestionInfo>()
            val methods = setup::class.java.methods
            methods.filter { it.isAnnotationPresent(SetupQuestion::class.java) }.forEach {
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
            val finishMethod = finishMethods.firstOrNull()
            val cancelMethod = cancelMethods.firstOrNull()

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
            terminal.changePrompt("&f> ")
            terminal.printSetup(setup.header)
            terminal.printSetup("")
            printCurrentQuestion()
        } catch (e: Exception) {
            logger.error("Failed to start setup", e)
        }
    }

    private fun printCurrentQuestion() {
        val currentQuestion =
            this.currentQuestion ?: throw IllegalStateException("There is no setup at the moment")
        val questionSetupAnswer = currentQuestion.setupQuestion.answer.java.getDeclaredConstructor().newInstance()
        val answers = questionSetupAnswer.suggest()
        val suffix =
            if (answers.isNotEmpty()) "&ePossible answers: " + answers.joinToString() else ""
        if (suffix.isEmpty()) {
            terminal.printSetup(currentQuestion.setupQuestion.translationKey)
        } else {
            terminal.printSetup(currentQuestion.setupQuestion.translationKey)
            terminal.printSetup(suffix)
        }
    }

    private fun nextQuestionExists(setup: SetupInfo) =
        this.currentQuestionIndex + 1 in setup.questions.indices

    private fun finishSetup() {
        terminal.clear()
        this.currentQuestion = null
        this.currentQuestionIndex = 0
        this.currentSetup!!.callFinish()
        this.currentSetup = null

        oldTerminalInput.forEach { line ->
            terminal.printNoCheck(line)
        }

        terminal.changePrompt("")

        logger.info("Setup &2Finished")
    }

    fun cancelSetup() {
        terminal.clear()
        this.currentSetup?.callCancel()
        this.currentSetup = null
        this.currentQuestion = null
        this.currentQuestionIndex = 0

        oldTerminalInput.forEach { line ->
            terminal.printNoCheck(line)
        }

        terminal.changePrompt("")

        logger.info("Setup &cCancelled")
    }

    suspend fun input(input: String) {
        if (inputLocked) {
            lockedInputCount++
            terminal.terminal.writer().print('\r')
            terminal.terminal.puts(InfoCmp.Capability.clr_eol)
            terminal.terminal.puts(InfoCmp.Capability.cursor_up)
            terminal.terminal.flush()
            return
        }

        val q = this.currentQuestion ?: return

        val answers = q.setupQuestion.answer.java
            .getDeclaredConstructor()
            .newInstance()
            .suggest()

        fun resetCurrentInput() {
            terminal.terminal.puts(InfoCmp.Capability.clr_bol)
            terminal.terminal.puts(InfoCmp.Capability.clr_eol)
            terminal.terminal.puts(InfoCmp.Capability.cursor_up)
            terminal.terminal.puts(InfoCmp.Capability.clr_bol)
            terminal.terminal.puts(InfoCmp.Capability.clr_eol)
            terminal.terminal.puts(InfoCmp.Capability.cursor_up)
            terminal.terminal.puts(InfoCmp.Capability.carriage_return)
            terminal.terminal.puts(InfoCmp.Capability.clr_eol)
            terminal.terminal.puts(InfoCmp.Capability.cursor_normal)
            terminal.changePrompt("&f> ")
            terminal.terminal.writer().print(terminal.replaceColors("&f> "))
            terminal.terminal.flush()
        }

        fun failAndLock() {
            inputLocked = true
            lockedInputCount = 0
        }

        suspend fun unlockAfterDelay() {
            delay(2500)
            resetCurrentInput()
            inputLocked = false
        }

        if (answers.isNotEmpty() && !answers.contains(input) && q.setupQuestion.forceAnswer) {
            terminal.printSetup("&cInvalid input, please try again!")
            failAndLock()
            unlockAfterDelay()
            return
        }

        val invoke = try {
            q.method.invoke(this.currentSetup!!.setup, input)
        } catch (_: Exception) {
            terminal.printSetup("&cInvalid input, please try again!")
            failAndLock()
            unlockAfterDelay()
            return
        }

        if (invoke is Boolean && !invoke) {
            terminal.printSetup("&cInvalid input, please try again!")
            failAndLock()
            unlockAfterDelay()
            return
        }

        terminal.terminal.puts(InfoCmp.Capability.cursor_up)
        terminal.terminal.puts(InfoCmp.Capability.clr_bol)
        terminal.terminal.puts(InfoCmp.Capability.clr_eol)
        terminal.terminal.puts(InfoCmp.Capability.cursor_up)
        terminal.terminal.puts(InfoCmp.Capability.clr_bol)
        terminal.terminal.puts(InfoCmp.Capability.clr_eol)
        terminal.terminal.puts(InfoCmp.Capability.cursor_up)
        terminal.terminal.puts(InfoCmp.Capability.clr_bol)
        terminal.terminal.puts(InfoCmp.Capability.clr_eol)
        terminal.printSetup("${q.setupQuestion.translationKey} &7> &f$input")
        terminal.terminal.flush()

        val setup = this.currentSetup ?: return
        if (!nextQuestionExists(setup)) {
            finishSetup()
            return
        }

        this.currentQuestionIndex++
        this.currentQuestion = setup.questions[currentQuestionIndex]
        printCurrentQuestion()
    }

    fun getSetupAnswers(input: String): List<String> {
        if (currentSetup == null) {
            throw IllegalStateException("There is no Setup running!")
        }
        val answers = currentQuestion!!.setupQuestion.answer.java.getDeclaredConstructor().newInstance().suggest()
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
