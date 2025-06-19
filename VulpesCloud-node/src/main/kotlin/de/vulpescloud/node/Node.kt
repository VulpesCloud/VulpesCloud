package de.vulpescloud.node

import de.vulpescloud.api.cluster.AuthenticationProvider
import de.vulpescloud.api.cluster.ClusterProvider
import de.vulpescloud.api.event.EventManager
import de.vulpescloud.api.lang.Translator
import de.vulpescloud.api.player.PlayerProvider
import de.vulpescloud.api.service.ServiceProvider
import de.vulpescloud.api.task.TaskProvider
import de.vulpescloud.api.template.TemplateStorageProvider
import de.vulpescloud.api.version.VersionProvider
import de.vulpescloud.jediswrapper.JedisWrapper
import de.vulpescloud.jediswrapper.JedisWrapper.getRC
import de.vulpescloud.node.cluster.AuthenticationProviderImpl
import de.vulpescloud.node.cluster.ClusterProviderImpl
import de.vulpescloud.node.cluster.NodeCommunicationChannelListener
import de.vulpescloud.node.command.CommandProvider
import de.vulpescloud.node.command.impl.CommandProviderImpl
import de.vulpescloud.node.commands.*
import de.vulpescloud.node.config.NodeConfig
import de.vulpescloud.node.event.EventManagerImpl
import de.vulpescloud.node.event.listeners.cluster.NodeStateChangeEventListener
import de.vulpescloud.node.event.listeners.module.ModuleLoadEventListener
import de.vulpescloud.node.event.listeners.module.ModuleStartEventListener
import de.vulpescloud.node.event.listeners.module.ModuleUnloadEventListener
import de.vulpescloud.node.event.listeners.service.ServiceLogEventListener
import de.vulpescloud.node.event.listeners.service.ServiceStateChangeEventListener
import de.vulpescloud.node.event.redis.cluster.NodeLogEventTrigger
import de.vulpescloud.node.event.redis.cluster.NodeStateChangeEventTrigger
import de.vulpescloud.node.event.redis.module.ModuleLoadEventTrigger
import de.vulpescloud.node.event.redis.module.ModuleStartEventTrigger
import de.vulpescloud.node.event.redis.module.ModuleUnloadEventTrigger
import de.vulpescloud.node.event.redis.service.ServiceLogEventTrigger
import de.vulpescloud.node.event.redis.service.ServiceStateChangeEventTrigger
import de.vulpescloud.node.module.ModuleProvider
import de.vulpescloud.node.module.impl.ModuleProviderImpl
import de.vulpescloud.node.module.impl.ServicePrepareListener
import de.vulpescloud.node.mysql.DatabaseProvider
import de.vulpescloud.node.player.PlayerProviderImpl
import de.vulpescloud.node.service.LocalServiceFactory
import de.vulpescloud.node.service.ServiceLogBuffer
import de.vulpescloud.node.service.ServiceProviderImpl
import de.vulpescloud.node.service.ServiceScheduler
import de.vulpescloud.node.setup.SetupProvider
import de.vulpescloud.node.setup.impl.SetupProviderImpl
import de.vulpescloud.node.setup.setups.FirstSetup
import de.vulpescloud.node.task.TaskProviderImpl
import de.vulpescloud.node.template.LocalTemplateStorage
import de.vulpescloud.node.template.TemplateStorageProviderImpl
import de.vulpescloud.node.terminal.JLineTerminal
import de.vulpescloud.node.terminal.impl.JLineTerminalImpl
import de.vulpescloud.node.utils.StringUtils
import de.vulpescloud.node.version.VersionProviderImpl
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Node : KoinComponent {
    private val nodeModule = module {
        single<JLineTerminal> { JLineTerminalImpl(get()) }
        single<CommandProvider> { CommandProviderImpl(get()) }
        single { NodeConfig() }
        single { Translator() }
        single<SetupProvider> { SetupProviderImpl(get(), get()) }
        single<AuthenticationProvider> { AuthenticationProviderImpl() }
        single { DatabaseProvider(get()) }
        single<EventManager> { EventManagerImpl() }
        single<ClusterProvider> { ClusterProviderImpl(get(), get(), get()) }
        single<ModuleProvider> { ModuleProviderImpl(get(), get()) }
        single<TemplateStorageProvider> { TemplateStorageProviderImpl() }
        single<TaskProvider> { TaskProviderImpl() }
        single<VersionProvider> { VersionProviderImpl() }
        single<ServiceProvider> { ServiceProviderImpl() }
        single<PlayerProvider> { PlayerProviderImpl() }
        single { LocalServiceFactory(get(), get(), get(), get(), get(), get(), get()) }
    }

    private val terminal: JLineTerminal by inject()
    private val commandProvider: CommandProvider by inject()
    private val config: NodeConfig by inject()
    private val translator: Translator by inject()
    private val setupProvider: SetupProvider by inject()
    private val authenticationProvider: AuthenticationProvider by inject()
    private val clusterProvider: ClusterProvider by inject()
    private val eventManager: EventManager by inject()
    private val moduleProvider: ModuleProvider by inject()
    private val templateStorageProvider: TemplateStorageProvider by inject()
    private val databaseProvider: DatabaseProvider by inject()
    private val taskProvider: TaskProvider by inject()
    private val versionProvider: VersionProvider by inject()
    private val serviceFactory: LocalServiceFactory by inject()
    private val serviceProvider: ServiceProvider by inject()
    private val playerProvider: PlayerProvider by inject()

    private val logger = LoggerFactory.getLogger(Node::class.java)

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

        databaseProvider.initialize()
        databaseProvider.generateTables()

        val clusterProviderImpl = clusterProvider as ClusterProviderImpl
        clusterProviderImpl.initialize()

        NodeStateChangeEventTrigger(eventManager)
        ModuleLoadEventTrigger(eventManager)
        ModuleUnloadEventTrigger(eventManager)
        ModuleStartEventTrigger(eventManager)
        ServiceLogEventTrigger(eventManager)
        ServiceStateChangeEventTrigger(eventManager)
        NodeLogEventTrigger(eventManager)

        eventManager.registerListener(NodeStateChangeEventListener(translator))
        eventManager.registerListener(ModuleLoadEventListener(translator, clusterProvider))
        eventManager.registerListener(ModuleStartEventListener(translator, clusterProvider))
        eventManager.registerListener(ModuleUnloadEventListener(translator, clusterProvider))
        eventManager.registerListener(ServiceLogEventListener(serviceProvider))
        eventManager.registerListener(ServiceStateChangeEventListener())

        val versionProviderImplementation = versionProvider as VersionProviderImpl

        versionProviderImplementation.initialize()

        templateStorageProvider.registerTemplateStorage(LocalTemplateStorage())

        moduleProvider.loadAllModules()

        commandProvider.initialize()

        moduleProvider.startAllModules()

        (serviceProvider as ServiceProviderImpl).serviceFactories.add(serviceFactory)

        commandProvider.register(ExitCommand())
        commandProvider.register(HelpCommand(commandProvider))
        commandProvider.register(ClusterCommand(clusterProvider))
        commandProvider.register(ClearCommand(terminal))
        commandProvider.register(ModuleCommand(moduleProvider))
        commandProvider.register(TemplateCommand(templateStorageProvider))
        commandProvider.register(
            TaskCommand(
                setupProvider,
                taskProvider,
                translator,
                terminal,
                config,
                versionProvider,
                serviceProvider,
            )
        )
        commandProvider.register(ServiceCommand(serviceProvider, clusterProvider))
        commandProvider.register(PlayerCommand(playerProvider))
        commandProvider.register(InfoCommand())

        NodeCommunicationChannelListener(clusterProvider)

        eventManager.registerListener(ServicePrepareListener())

        logger.info(
            translator.trans("NODE.ONLINE"),
            System.currentTimeMillis() - System.getProperty("startup").toLong(),
        )

        terminal.allowInput()

        clusterProviderImpl.markOnline()

        ServiceScheduler.run()
        ServiceLogBuffer
    }

    companion object {
        fun getForwardingSecret(): String {
            return if (getRC()?.getString("VULPESCLOUD:FORWARDING:SECRET") != null) {
                getRC()?.getString("VULPESCLOUD:FORWARDING:SECRET")!!
            } else {
                val secret = StringUtils.generateRandomString(8)
                getRC()?.setString("VULPESCLOUD:FORWARDING:SECRET", secret)
                secret
            }
        }
    }
}
