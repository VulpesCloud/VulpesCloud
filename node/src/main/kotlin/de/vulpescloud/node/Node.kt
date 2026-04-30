package de.vulpescloud.node

import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import de.vulpescloud.api.players.OnlinePlayer
import de.vulpescloud.node.auth.AuthServiceImpl
import de.vulpescloud.node.cluster.ClusterAPIServiceImpl
import de.vulpescloud.node.cluster.ClusterProvider
import de.vulpescloud.node.command.CommandProvider
import de.vulpescloud.node.commands.*
import de.vulpescloud.node.config.ConfigProvider
import de.vulpescloud.node.db.DatabaseProvider
import de.vulpescloud.node.db.impl.mariadb.MariaDBDatabaseProvider
import de.vulpescloud.node.db.impl.mongo.MongoDBDatabaseProvider
import de.vulpescloud.node.db.impl.sqlite.SQLiteDatabaseProvider
import de.vulpescloud.node.event.EventListenHelper
import de.vulpescloud.node.event.EventsService
import de.vulpescloud.node.grpc.GrpcServer
import de.vulpescloud.node.grpc.LocalGrpcClient
import de.vulpescloud.node.grpc.LoggingServerInterceptor
import de.vulpescloud.node.grpc.security.AuthInterceptor
import de.vulpescloud.node.grpc.security.PermissionInterceptor
import de.vulpescloud.node.modules.ModuleProvider
import de.vulpescloud.node.secret.SecretFactory
import de.vulpescloud.node.serversoftware.ServerSoftwareProvider
import de.vulpescloud.node.serversoftware.impl.FoliaDownloader
import de.vulpescloud.node.serversoftware.impl.PaperDownloader
import de.vulpescloud.node.serversoftware.impl.PurpurDownloader
import de.vulpescloud.node.serversoftware.impl.VelocityDownloader
import de.vulpescloud.node.services.AbstractService
import de.vulpescloud.node.services.ServiceFactoryProvider
import de.vulpescloud.node.services.ServiceScheduler
import de.vulpescloud.node.services.ServicesAPIService
import de.vulpescloud.node.services.impl.docker.DockerServiceFactory
import de.vulpescloud.node.services.impl.local.LocalServiceFactory
import de.vulpescloud.node.setup.SetupProvider
import de.vulpescloud.node.setup.setups.FirstSetup
import de.vulpescloud.node.tasks.TasksAPIService
import de.vulpescloud.node.templates.TemplateStorageProvider
import de.vulpescloud.node.terminal.Terminal
import de.vulpescloud.node.virtualconfig.VirtualConfigProvider
import de.vulpescloud.node.virtualconfig.VirtualConfigServiceImpl
import io.grpc.BindableService
import io.grpc.ChannelCredentials
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

class Node {
    private val logger = LoggerFactory.getLogger("Node")

    val terminal = Terminal()
    val commandProvider = CommandProvider()
    val configProvider = ConfigProvider()

    lateinit var grpcServer: GrpcServer
    lateinit var secret: String
    lateinit var setupProvider: SetupProvider
    lateinit var credentials: ChannelCredentials
    var inputJob: Job? = null
        private set

    lateinit var dockerClientConfig: DockerClientConfig
    lateinit var dockerHttpClient: DockerHttpClient

    lateinit var internalEventsService: EventsService

    val templateStorageProvider = TemplateStorageProvider()
    val localGrpcClient = LocalGrpcClient()
    val serviceFactoryProvider = ServiceFactoryProvider()
    val nodeServices = mutableListOf<AbstractService>()
    val nodeProxyPlayers: MutableMap<String, MutableList<OnlinePlayer>> = ConcurrentHashMap()   // ProxyName<Player>
    val nodeServerPlayers: MutableMap<String, MutableList<OnlinePlayer>> = ConcurrentHashMap()  // ServerName<Player>
    val virtualConfigProvider = VirtualConfigProvider()
    val clusterProvider = ClusterProvider()
    val moduleProvider =
        ModuleProvider(
            Path("modules"),
            System.getProperty(
                "vulpescloud.modules.url",
                "https://github.com/VulpesCloud/VulpesCloud-meta/raw/refs/heads/main/modules.json",
            ),
        )
    val virtualConfigServiceImpl = VirtualConfigServiceImpl()
    val serverSoftwareProvider = ServerSoftwareProvider()

    private val grpcServices = mutableListOf<BindableService>()
    private var allowGrpcServiceAdding = true

    suspend fun init(scope: CoroutineScope) =
        withContext(Dispatchers.IO) {
            instance = this@Node

            virtualConfigProvider.tempConfigsPath.toFile().mkdirs()

            val configExists = configProvider.loadConfig()

            setupProvider = SetupProvider(terminal)
            terminal.init()

            //            CertGen.loadOrCreate(
            //                keyFile = File("certs/server.key"),
            //                certFile = File("certs/server.crt"),
            //            )

            //            val serverCertBytes = File("certs/server.crt")
            //            creds =
            // TlsChannelCredentials.newBuilder().trustManager(serverCertBytes).build()

            val secretFactory = SecretFactory()
            secret = secretFactory.loadOrCreateSecret(Path("launcher/secret/.auth.secret"))

            inputJob = scope.launch(Dispatchers.IO) { terminal.allowInput() }

            if (!configExists) {
                setupProvider.startSetup(FirstSetup())
            }

            while (setupProvider.currentSetup?.setup is FirstSetup) {
                delay(500.milliseconds)
            }

            terminal.changePrompt("")

            commandProvider.initialize()

            allowGrpcServiceAdding = true
            moduleProvider.loadAllModules()

            try {
                commandProvider.apply {
                    register(ClearCommand(terminal))
                    register(HelpCommand(commandProvider))
                    register(ExitCommand())
                    register(InfoCommand())
                    register(ServiceCommand())
                    register(DebugCommand())
                    register(TaskCommand())
                    register(VirtualConfigCommand())
                    register(ClusterCommand())
                    register(AuthCommand())
                    register(ModuleCommand())
                    register(SoftwareCommand())
                }
            } catch (e: Exception) {
                logger.error("Failed to initialize commands: ${e.stackTraceToString()}")
                return@withContext
            }

            DatabaseProvider.apply {
                addDatabaseProvider("sqlite", SQLiteDatabaseProvider())
                addDatabaseProvider("mariadb", MariaDBDatabaseProvider())
                addDatabaseProvider("mongodb", MongoDBDatabaseProvider())

                lockDatabaseProviderAdding()
                setAndInitializeMainDatabaseProvider()
            }

            internalEventsService = EventsService()

            grpcServices.addAll(
                listOf(
                    TasksAPIService(),
                    ServicesAPIService(),
                    internalEventsService,
                    virtualConfigServiceImpl,
                    ClusterAPIServiceImpl(),
                    AuthServiceImpl(
                        configProvider.config.auth.jwtSecret,
                        configProvider.config.auth.jwtRefreshSecret,
                    ),
                )
            )

            allowGrpcServiceAdding = false
            grpcServer =
                GrpcServer(
                    host = configProvider.config.grpcHost,
                    port = configProvider.config.grpcPort,
                    services = grpcServices,
                    interceptors =
                        listOf(
                            PermissionInterceptor(),
                            AuthInterceptor(secret, configProvider.config.auth.jwtSecret),
                            LoggingServerInterceptor(),
                        ),
                )
            grpcServer.start()
            NodeCoroutineScope.launch { grpcServer.awaitTermination() }

            localGrpcClient.connect(
                host = configProvider.config.grpcHost,
                port = configProvider.config.grpcPort,
                secret = secret,
            )

            EventListenHelper.subscribeToEvents()

            clusterProvider.initClusterConfig()
            clusterProvider.init()
            clusterProvider.connectToOtherNodes()

            serviceFactoryProvider.apply {
                registerServiceFactory(DockerServiceFactory())
                registerServiceFactory(LocalServiceFactory())
            }

            if (configProvider.config.docker.enabled) {
                try {
                    dockerClientConfig =
                        DefaultDockerClientConfig.createDefaultConfigBuilder()
                            .withDockerHost(configProvider.config.docker.host)
                            .withDockerCertPath(configProvider.config.docker.dockerCertPath)
                            .withDockerTlsVerify(
                                configProvider.config.docker.dockerCertPath != null
                            )
                            .withRegistryUsername(configProvider.config.docker.registryUsername)
                            .withRegistryPassword(configProvider.config.docker.registryPassword)
                            .withRegistryEmail(configProvider.config.docker.registryEmail)
                            .withRegistryUrl("https://index.docker.io/v1/")
                            .build()

                    dockerHttpClient =
                        ApacheDockerHttpClient.Builder()
                            .dockerHost(dockerClientConfig.dockerHost)
                            .sslConfig(dockerClientConfig.sslConfig)
                            .maxConnections(100)
                            .connectionTimeout(Duration.ofSeconds(30))
                            .responseTimeout(Duration.ofSeconds(45))
                            .build()

                    logger.info("Trying to connect to Docker...")
                    val dockerClient =
                        DockerClientImpl.getInstance(dockerClientConfig, dockerHttpClient)
                    val version = dockerClient.versionCmd().exec()
                    logger.info(
                        "Successfully connected to Docker! Version: ${version.version}, API Version: ${version.apiVersion}"
                    )
                } catch (e: Exception) {
                    logger.error("Failed to connect to Docker: ${e.message}")
                    return@withContext
                }
            }

            serverSoftwareProvider.apply {
                registerDownloader(FoliaDownloader)
                registerDownloader(PaperDownloader)
                registerDownloader(PurpurDownloader)
                registerDownloader(VelocityDownloader)

                lock()
            }

            moduleProvider.startAllModules()

            clusterProvider.startupDone()

            moduleProvider.checkAllLoadedModulesForUpdates()

            val time =
                System.currentTimeMillis() - (System.getProperty("startup").toLongOrNull() ?: 0)
            logger.info("Startup Done! Took {}ms", time)

            ServiceScheduler.start()

            // Runtime.getRuntime().addShutdownHook(Thread { NodeShutdown.shutdown() })
        }

    fun getVelocitySecret(): String {
        val secretFactory = SecretFactory()
        return secretFactory.loadOrCreateSecret(Path("launcher/secret/.velocity.secret"))
    }

    fun addGrpcService(service: BindableService) {
        if (allowGrpcServiceAdding) {
            grpcServices.add(service)
        } else {
            logger.error(
                "Cannot add gRPC Service ${service.bindService().serviceDescriptor.name} after startup!"
            )
        }
    }

    fun getDatabaseProvider() = DatabaseProvider.getMainDatabaseProvider()

    companion object {
        lateinit var instance: Node

        suspend fun create(scope: CoroutineScope): Node {
            val node = Node()
            node.init(scope)
            return node
        }

        @JvmStatic
        fun main(args: Array<String>) = runBlocking {
            val node = create(this)
            node.inputJob?.join()
        }
    }
}
