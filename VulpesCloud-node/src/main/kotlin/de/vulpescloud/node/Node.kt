package de.vulpescloud.node

import de.vulpescloud.api.cluster.AuthenticationProvider
import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.api.lang.Translator
import de.vulpescloud.jediswrapper.JedisWrapper
import de.vulpescloud.node.cluster.AuthenticationProviderImpl
import de.vulpescloud.node.cluster.ClusterProviderImpl
import de.vulpescloud.node.command.CommandProvider
import de.vulpescloud.node.command.impl.CommandProviderImpl
import de.vulpescloud.node.commands.ExitCommand
import de.vulpescloud.node.commands.HelpCommand
import de.vulpescloud.node.config.NodeConfig
import de.vulpescloud.node.setup.SetupProvider
import de.vulpescloud.node.setup.impl.SetupProviderImpl
import de.vulpescloud.node.setup.setups.FirstSetup
import de.vulpescloud.node.terminal.JLineTerminal
import de.vulpescloud.node.terminal.impl.JLineTerminalImpl
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.module

class Node : KoinComponent {
    private val nodeModule = module {
        single<JLineTerminal> { JLineTerminalImpl(get()) }
        single<CommandProvider> { CommandProviderImpl(get()) }
        single { NodeConfig() }
        single { Translator() }
        single<SetupProvider> { SetupProviderImpl(get(), get()) }
        single<AuthenticationProvider> { AuthenticationProviderImpl() }
        single<ClusterProvider> { ClusterProviderImpl(get()) }
    }

    private val terminal: JLineTerminal by inject()
    private val commandProvider: CommandProvider by inject()
    private val config: NodeConfig by inject()
    private val translator: Translator by inject()
    private val setupProvider: SetupProvider by inject()
    private val authenticationProvider: AuthenticationProvider by inject()
    private val clusterProvider: ClusterProvider by inject()

    val setupLock = ReentrantLock()
    val setupCondition: Condition = setupLock.newCondition()

    init {
        startKoin { modules(nodeModule) }
        terminal.initTerminal()

        config.initializeConfig()

        translator.setLang(config.language())
        translator.loadFromDefaultClassPath()

        if (!config.ranFirstSetup()) {
            CompletableFuture.runAsync {
                setupLock.withLock {
                    terminal.allowInput()
                    setupProvider.startSetup(FirstSetup(terminal, translator, config, this))
                }
            }
            setupLock.withLock { setupCondition.await() }
        }

        val authProv = authenticationProvider as AuthenticationProviderImpl
        authProv.initializeToken()

        JedisWrapper.initializeRedisControllerWithSecret(
            config.redis().password,
            config.redis().port,
            config.redis().hostname,
            authenticationProvider.getAuthenticationToken(),
        )

        val clusterProv = clusterProvider as ClusterProviderImpl
        clusterProv.initialize()

        commandProvider.initialize()

        commandProvider.register(ExitCommand())
        commandProvider.register(HelpCommand(commandProvider))

        terminal.allowInput()
    }
}
