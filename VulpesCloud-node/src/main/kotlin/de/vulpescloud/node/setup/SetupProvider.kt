/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.vulpescloud.node.setup

import de.vulpescloud.api.language.Translator
import de.vulpescloud.node.Node
import de.vulpescloud.node.setup.annotations.SetupCancel
import de.vulpescloud.node.setup.annotations.SetupFinish
import de.vulpescloud.node.setup.annotations.SetupQuestion
import org.slf4j.LoggerFactory
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.util.*

class SetupProvider {

    private val logger = LoggerFactory.getLogger(SetupProvider::class.java)
    var currentSetup: SetupInfo? = null
    private var currentQuestion: SetupQuestionInfo? = null
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

            Node.instance.terminal.clear()
            Node.instance.terminal.printSetup(Translator.trans("node.setup.all.started"))
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
            Node.instance.terminal.printSetup(Translator.trans(currentQuestion.setupQuestion.message))
            Node.instance.terminal.printSetup("")
        } else {
            Node.instance.terminal.printSetup(Translator.trans(currentQuestion.setupQuestion.message))
            Node.instance.terminal.printSetup(suffix)
            Node.instance.terminal.printSetup("")
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
        Node.instance.terminal.printSetup("")
        printCurrentQuestion()
    }

    private fun nextQuestionExists(setup: SetupInfo) = this.currentQuestionIndex + 1 in setup.questions.indices

    private fun finishSetup() {
        Node.instance.terminal.clear()
        this.currentSetup?.callFinish()
        this.currentSetup = null
        this.currentQuestion = null
        this.currentQuestionIndex = 0
        logger.info("Setup &2Finished")
    }

    fun cancelSetup() {
        Node.instance.terminal.clear()
        this.currentSetup?.callCancel()
        this.currentSetup = null
        this.currentQuestion = null
        this.currentQuestionIndex = 0
        logger.info("Setup &cCancelled")
    }

    fun input(input: String) {
        val currentQuestion = this.currentQuestion ?: return
        val answers = currentQuestion.setupQuestion.answer.java.newInstance().suggest()
        if (answers.isNotEmpty() && !answers.contains(input) && currentQuestion.setupQuestion.forceAnswer) {
            Node.instance.terminal.printSetup("&cInvalid Response!")
            return
        }
        val invoke = try {
            currentQuestion.method.invoke(this.currentSetup!!.setup, input)
        } catch (e: Exception) {
            Node.instance.terminal.printSetup("&cInvalid Response!")
            return
        }
        if (invoke is Boolean && invoke == false) {
            return
        }
        prepareNextQuestion()
    }

    fun getSetupAnswers(input: String): List<String> {
        if (currentSetup == null) {
            throw IllegalStateException("There is no Setup running!")
        }
        val answers = currentQuestion!!.setupQuestion.answer.java.newInstance().suggest()
        if (currentQuestion!!.setupQuestion.default.isNotEmpty()) {
            val mList = answers.toMutableList()
            mList.addAll(currentQuestion!!.setupQuestion.default)

            return mList.filter { it.lowercase(Locale.getDefault()).startsWith(input.lowercase(Locale.getDefault())) }
        }

        return answers.filter { it.lowercase(Locale.getDefault()).startsWith(input.lowercase(Locale.getDefault())) }
    }


    class SetupInfo(
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
    class SetupQuestionInfo(
        val setupQuestion: SetupQuestion,
        val method: Method,
        val parameter: Parameter
    )

}