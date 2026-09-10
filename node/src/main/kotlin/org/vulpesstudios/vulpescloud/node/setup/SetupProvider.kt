/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.node.setup

import kotlinx.coroutines.delay
import org.jline.utils.InfoCmp
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.node.setup.annotations.SetupCancel
import org.vulpesstudios.vulpescloud.node.setup.annotations.SetupFinish
import org.vulpesstudios.vulpescloud.node.setup.annotations.SetupQuestion
import org.vulpesstudios.vulpescloud.node.setup.answers.NullSetupAnswer
import org.vulpesstudios.vulpescloud.node.terminal.Terminal
import java.util.*

class SetupProvider(private val terminal: Terminal) {

    private val logger = LoggerFactory.getLogger(SetupProvider::class.java)
    var currentSetup: SetupInfo? = null
    private var currentQuestion: SetupQuestionInfo? = null
    var currentQuestionIndex = 0
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

            currentQuestionIndex = 0

            if (currentSetup == null) {
                currentSetup = setupInfo
                this.currentQuestion = setupInfo.questions[currentQuestionIndex]
            } else {
                return
            }

            terminal.clear()
            terminal.changePrompt("<white>> </white>")
            terminal.printSetup(setup.header)
            terminal.printSetup("")
            printCurrentQuestion()
        } catch (e: Exception) {
            logger.error("<red>Failed to start setup</red>", e)
        }
    }

    private fun printCurrentQuestion() {
        val currentQuestion =
            this.currentQuestion ?: throw IllegalStateException("There is no setup at the moment")
        val questionSetupAnswer = currentQuestion.setupQuestion.answer.java.getDeclaredConstructor().newInstance()
        val answers = questionSetupAnswer.suggest()
        val suffix =
            if (answers.isNotEmpty()) "<yellow>Possible answers<dark_gray>:</dark_gray> <white>" + answers.joinToString() + "</white></yellow>" else ""
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

        logger.info("<gray>Setup</gray> <green>finished</green><dark_gray>.</dark_gray>")
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

        logger.info("<gray>Setup</gray> <red>cancelled</red><dark_gray>.</dark_gray>")
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
            terminal.changePrompt("<white>> </white>")
            terminal.terminal.writer().print(terminal.replaceColors("<white>> </white>"))
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
            terminal.printSetup("<red>Invalid input, please try again!</red>")
            failAndLock()
            unlockAfterDelay()
            return
        }

        val invoke = try {
            q.method.invoke(this.currentSetup!!.setup, input)
        } catch (_: Exception) {
            terminal.printSetup("<red>Invalid input, please try again!</red>")
            failAndLock()
            unlockAfterDelay()
            return
        }

        if (invoke is Boolean && !invoke) {
            terminal.printSetup("<red>Invalid input, please try again!</red>")
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
        if (currentQuestion!!.setupQuestion.answer != NullSetupAnswer::class) {
            terminal.terminal.puts(InfoCmp.Capability.cursor_up)
            terminal.terminal.puts(InfoCmp.Capability.clr_bol)
            terminal.terminal.puts(InfoCmp.Capability.clr_eol)
        }

        terminal.printSetup("<gray>${q.setupQuestion.translationKey}</gray> <dark_gray>»</dark_gray> <white>$input</white>")
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
