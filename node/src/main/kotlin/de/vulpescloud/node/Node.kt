package de.vulpescloud.node

import de.vulpescloud.node.command.CommandProvider
import de.vulpescloud.node.commands.ClearCommand
import de.vulpescloud.node.commands.ExitCommand
import de.vulpescloud.node.commands.HelpCommand
import de.vulpescloud.node.commands.InfoCommand
import de.vulpescloud.node.config.ConfigProvider
import de.vulpescloud.node.grpc.GrpcServer
import de.vulpescloud.node.terminal.Terminal
import kotlinx.coroutines.*

class Node {

    val terminal = Terminal()
    val commandProvider = CommandProvider()
    val configProvider = ConfigProvider()

    suspend fun init() = withContext(Dispatchers.Default) {
        instance = this@Node

        terminal.init()

        configProvider.loadConfig()

        commandProvider.initialize()
        commandProvider.apply {
            register(ClearCommand(terminal))
            register(HelpCommand(commandProvider))
            register(ExitCommand())
            register(InfoCommand())
        }

        GrpcServer(
            services = listOf(

            )
        ).serve(NodeCoroutineScope)
    }

    fun startInput(scope: CoroutineScope): Job =
        scope.launch(Dispatchers.IO) { terminal.allowInput() }

    companion object {
        lateinit var instance: Node

        suspend fun create(scope: CoroutineScope): Pair<Node, Job> {
            val node = Node()
            node.init()
            val inputJob = node.startInput(scope)
            return node to inputJob
        }

        @JvmStatic
        fun main(args: Array<String>) = runBlocking {
            val (_, inputJob) = create(this)
            inputJob.join()
        }
    }
}