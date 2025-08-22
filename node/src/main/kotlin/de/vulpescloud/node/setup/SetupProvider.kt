package de.vulpescloud.node.setup

import de.vulpescloud.node.Node
import de.vulpescloud.node.setup.annotations.SetupCancel
import de.vulpescloud.node.setup.annotations.SetupFinish
import de.vulpescloud.node.setup.annotations.SetupQuestion
import org.jline.consoleui.prompt.ConsolePrompt
import org.jline.terminal.Terminal
import org.slf4j.LoggerFactory
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation

class SetupProvider {

    private val logger = LoggerFactory.getLogger("SetupProvider")
    lateinit var prompt: ConsolePrompt
    var currentSetup: Setup? = null
    var currentIndex = 0
    var answers = mutableMapOf<Int, Any?>()

    fun init() {
        val uiConfig = ConsolePrompt.UiConfig()
        val terminal = Node.instance.terminal.terminal
        val lineReader = Node.instance.terminal.lineReader

        prompt = ConsolePrompt(lineReader, terminal, uiConfig)
    }

    fun parseSetup(setup: Setup) {
        val functions = setup::class.functions
        val setupQuestions = functions.filter { it.hasAnnotation<SetupQuestion>() }
            .sortedBy { it.findAnnotation<SetupQuestion>()!!.index }
        val finishFunction = functions.firstOrNull { it.hasAnnotation<SetupFinish>() }
        val cancelFunction = functions.firstOrNull { it.hasAnnotation<SetupCancel>() }

        if (setupQuestions.isEmpty()) {
            logger.error("Tried to start a setup without questions!")
            return
        }
        if (finishFunction == null) {
            logger.error("Tried to start a setup without a finish function!")
            return
        }
        if (cancelFunction == null) {
            logger.error("Tried to start a setup without a cancel function!")
            return
        }

        currentSetup = setup

        while (currentIndex < setupQuestions.size) {
            val func = setupQuestions[currentIndex]
            val annotation = func.findAnnotation<SetupQuestion>()!!
            val builder = prompt.promptBuilder

            builder.createInputPrompt()
                .name("answer")
                .message("[${annotation.index}] ${annotation.question} (Tippe /skip <index>, /goto <index> oder /cancel)")
                .addPrompt()

            val result = prompt.prompt(builder.build())
            val input = result["answer"]!!.result as String

            when {
                input.startsWith("goto") -> {
                    val idx = input.removePrefix("goto").trim().toIntOrNull()
                    if (idx != null && setupQuestions.any { it.findAnnotation<SetupQuestion>()!!.index == idx }) {
                        currentIndex = setupQuestions.indexOfFirst { it.findAnnotation<SetupQuestion>()!!.index == idx }
                        continue
                    }
                }
                input == "cancel" -> {
                    cancelFunction.call(setup)
                    println("Setup abgebrochen.")
                    return
                }
                else -> {
                    val param: KParameter = func.parameters[1]
                    val value: Any = when (param.type.classifier) {
                        Int::class -> input.toIntOrNull() ?: 0
                        Boolean::class -> input.equals("true", ignoreCase = true) || input == "1"
                        else -> input
                    }
                    answers[annotation.index] = value
                    val result = func.call(setup, value) as? Boolean
                }
            }
            currentIndex++
        }
        finishFunction.call(setup)
        println("Setup abgeschlossen!")
    }

    fun test() {
        demonstrateInputPrompt(Node.instance.terminal.terminal)
    }

    fun demonstrateInputPrompt(terminal: Terminal) {
        val prompt = ConsolePrompt(terminal)
        val builder = prompt.promptBuilder

        builder
            .createInputPrompt()
            .name("username")
            .message("Enter your username")
            .defaultValue("admin")
            .addPrompt()

        builder
            .createInputPrompt()
            .name("password")
            .message("Enter your password")
            .mask('*')
            .addPrompt()

        builder.createListPrompt()
            .name("color")
            .message("Choose your favorite color")
            .newItem()
            .text("Red")
            .add()
            .newItem("green")
            .text("Green")
            .add()
            .newItem("blue")
            .text("Blue")
            .add()
            .newItem("yellow")
            .text("Yellow")
            .add()
            .pageSize(3)
            .addPrompt();

        try {
            val result = prompt.prompt(builder.build())
            val username = result["username"]!!.result
            val password = result["password"]!!.result
            println("Logged in as: $username with password: $password")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}