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

package de.vulpescloud.node.command

import de.vulpescloud.node.command.source.CommandSource
import lombok.NonNull
import org.incendo.cloud.CommandManager
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.internal.CommandRegistrationHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Consumer


class CloudCommandManager(
    executionCoordinator: ExecutionCoordinator<CommandSource>,
) : CommandManager<CommandSource>(executionCoordinator, CommandRegistrationHandler.nullCommandRegistrationHandler()) {

    companion object {
        lateinit var instance: CloudCommandManager
    }

    val logger: Logger = LoggerFactory.getLogger(CloudCommandManager::class.java)

    fun bootstrap(
        @NonNull args: @NonNull Array<String?>,
        @NonNull managerConsumer: Consumer<CommandManager<CommandSource>?>,
    ) {
        val cliCommandManager: CommandManager<CommandSource> =
            CloudCommandManager(ExecutionCoordinator.asyncCoordinator())
        managerConsumer.accept(cliCommandManager)
        instance.run(args)
    }

//    private var permissionFunction: PermissionFunction = PermissionFunction.alwaysTrue()

    init {
        instance = this

        this.registerDefaultExceptionHandlers(
            { triplet ->
                val message = triplet.first().formatCaption(triplet.second(), triplet.third())
                logger.error(message)
            },
            { pair ->
                logger.error(pair.first())
                pair.second().printStackTrace()
            }
        )
    }

    override fun hasPermission(sender: CommandSource, permission: String): Boolean {
        return true
    }

    fun run(args: Array<String?>) {
        Objects.requireNonNull(args, "args")
        try {
            commandExecutor().executeCommand(CommandSource.console(), java.lang.String.join(" ", *args)).join()
        } catch (e: Exception) {
            logger.error(e.toString())
        }
    }

//    fun permissionFunction(permissionFunction: PermissionFunction) {
//        this.permissionFunction = Objects.requireNonNull(permissionFunction)
//    }


}