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

package org.vulpesstudios.vulpescloud.node

import build.buf.gen.vulpescloud.services.v1.ServiceSnapshot
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import io.grpc.BindableService
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.api.players.OnlinePlayer
import org.vulpesstudios.vulpescloud.node.auth.AuthServiceImpl
import org.vulpesstudios.vulpescloud.node.cluster.ClusterAPIServiceImpl
import org.vulpesstudios.vulpescloud.node.cluster.ClusterProvider
import org.vulpesstudios.vulpescloud.node.cluster.tls.ClusterCertificateAuthority
import org.vulpesstudios.vulpescloud.node.cluster.tls.GrpcTls
import org.vulpesstudios.vulpescloud.node.cluster.tls.TlsManager
import org.vulpesstudios.vulpescloud.node.command.CommandProvider
import org.vulpesstudios.vulpescloud.node.commands.*
import org.vulpesstudios.vulpescloud.node.config.ConfigProvider
import org.vulpesstudios.vulpescloud.node.db.DatabaseProvider
import org.vulpesstudios.vulpescloud.node.db.impl.mariadb.MariaDBDatabaseProvider
import org.vulpesstudios.vulpescloud.node.db.impl.mongo.MongoDBDatabaseProvider
import org.vulpesstudios.vulpescloud.node.db.impl.sqlite.SQLiteDatabaseProvider
import org.vulpesstudios.vulpescloud.node.event.EventListenHelper
import org.vulpesstudios.vulpescloud.node.event.EventsService
import org.vulpesstudios.vulpescloud.node.grpc.GrpcServer
import org.vulpesstudios.vulpescloud.node.grpc.LocalGrpcClient
import org.vulpesstudios.vulpescloud.node.grpc.LoggingServerInterceptor
import org.vulpesstudios.vulpescloud.node.grpc.security.AuthInterceptor
import org.vulpesstudios.vulpescloud.node.grpc.security.PermissionInterceptor
import org.vulpesstudios.vulpescloud.node.modules.ModuleProvider
import org.vulpesstudios.vulpescloud.node.players.PlayerActionServiceImpl
import org.vulpesstudios.vulpescloud.node.players.PlayerServiceImpl
import org.vulpesstudios.vulpescloud.node.secret.SecretFactory
import org.vulpesstudios.vulpescloud.node.serversoftware.ServerSoftwareProvider
import org.vulpesstudios.vulpescloud.node.serversoftware.impl.FoliaDownloader
import org.vulpesstudios.vulpescloud.node.serversoftware.impl.PaperDownloader
import org.vulpesstudios.vulpescloud.node.serversoftware.impl.PurpurDownloader
import org.vulpesstudios.vulpescloud.node.serversoftware.impl.VelocityDownloader
import org.vulpesstudios.vulpescloud.node.services.AbstractService
import org.vulpesstudios.vulpescloud.node.services.ServiceFactoryProvider
import org.vulpesstudios.vulpescloud.node.services.ServiceScheduler
import org.vulpesstudios.vulpescloud.node.services.ServicesAPIService
import org.vulpesstudios.vulpescloud.node.services.impl.docker.DockerServiceFactory
import org.vulpesstudios.vulpescloud.node.services.impl.local.LocalServiceFactory
import org.vulpesstudios.vulpescloud.node.setup.SetupProvider
import org.vulpesstudios.vulpescloud.node.setup.setups.FirstSetup
import org.vulpesstudios.vulpescloud.node.tasks.TasksAPIService
import org.vulpesstudios.vulpescloud.node.templates.LocalTemplateStorage
import org.vulpesstudios.vulpescloud.node.templates.TemplateServiceImpl
import org.vulpesstudios.vulpescloud.node.templates.TemplateStorageRegistry
import org.vulpesstudios.vulpescloud.node.terminal.Terminal
import org.vulpesstudios.vulpescloud.node.virtualconfig.VirtualConfigProvider
import org.vulpesstudios.vulpescloud.node.virtualconfig.VirtualConfigServiceImpl
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.milliseconds

class Node {
    private val logger = LoggerFactory.getLogger("Node")

    val nodeServices: MutableSet<AbstractService> = ConcurrentHashMap.newKeySet()
    val nodeServiceSnapshots: MutableSet<ServiceSnapshot> = ConcurrentHashMap.newKeySet()
    val nodeProxyPlayers: MutableMap<String, MutableList<OnlinePlayer>> =
        ConcurrentHashMap() // ProxyName<Player>
    val nodeServerPlayers: MutableMap<String, MutableList<OnlinePlayer>> =
        ConcurrentHashMap() // ServerName<Player>

    val terminal = Terminal()
    val commandProvider = CommandProvider()
    val configProvider = ConfigProvider()

    lateinit var grpcServer: GrpcServer
    lateinit var secret: String
    lateinit var setupProvider: SetupProvider
    var inputJob: Job? = null
        private set

    lateinit var dockerClientConfig: DockerClientConfig
    lateinit var dockerHttpClient: DockerHttpClient

    lateinit var internalEventsService: EventsService

    val localGrpcClient = LocalGrpcClient()
    val serviceFactoryProvider = ServiceFactoryProvider()
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

            val secretFactory = SecretFactory()
            secret = secretFactory.loadOrCreateSecret(Path("launcher/.secret/.auth.secret"))

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
                    register(PlayersCommand())
                    register(TlsCommand())
                    register(TemplateCommand())
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

            val tlsManager = TlsManager(secret, configProvider.config.nodeName)
            val bundle = tlsManager.bootstrapNode()
            val serverSslContext = GrpcTls.buildServerSslContext(
                bundle.nodeCertPem,
                ClusterCertificateAuthority.toPem(bundle.nodeKey.private),
                bundle.caCertPem
            )
            val clientSslContext = GrpcTls.buildClientSslContext(
                bundle.nodeCertPem,
                ClusterCertificateAuthority.toPem(bundle.nodeKey.private),
                bundle.caCertPem
            )

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
                    PlayerServiceImpl(),
                    PlayerActionServiceImpl(),
                    TemplateServiceImpl(),
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
                    sslContext = serverSslContext,
                )
            grpcServer.start()
            NodeCoroutineScope.launch { grpcServer.awaitTermination() }

            localGrpcClient.connect(
                host = configProvider.config.grpcHost,
                port = configProvider.config.grpcPort,
                sslContext = clientSslContext,
                secret = secret,
            )

            EventListenHelper.subscribeToEvents()

            clusterProvider.initClusterConfig()
            clusterProvider.init()
            clusterProvider.connectToOtherNodes(clientSslContext)

            TemplateStorageRegistry.registerTemplateStorage(LocalTemplateStorage())

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

            serverSoftwareProvider.triggerReCache()

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
