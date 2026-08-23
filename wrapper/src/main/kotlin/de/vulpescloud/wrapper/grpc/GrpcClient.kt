package de.vulpescloud.wrapper.grpc

import build.buf.gen.vulpescloud.auth.v1.AuthServiceGrpcKt
import build.buf.gen.vulpescloud.events.v1.EventServiceGrpcKt
import build.buf.gen.vulpescloud.cluster.v2.ClusterAPIServiceGrpcKt
import build.buf.gen.vulpescloud.players.v1.PlayersServiceGrpc
import build.buf.gen.vulpescloud.players.v1.PlayersServiceGrpcKt
import build.buf.gen.vulpescloud.services.v1.ServiceAPIServiceGrpc
import build.buf.gen.vulpescloud.services.v1.ServiceAPIServiceGrpcKt
import build.buf.gen.vulpescloud.tasks.v1.TasksAPIServiceGrpc
import build.buf.gen.vulpescloud.tasks.v1.TasksAPIServiceGrpcKt
import io.grpc.ManagedChannel
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.net.InetSocketAddress
import java.security.PrivateKey
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class GrpcClient {

    lateinit var channel: ManagedChannel
    lateinit var serviceAPI: ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub
    lateinit var tasksAPI: TasksAPIServiceGrpcKt.TasksAPIServiceCoroutineStub
    lateinit var playerAPI: PlayersServiceGrpcKt.PlayersServiceCoroutineStub
    lateinit var clusterAPI: ClusterAPIServiceGrpcKt.ClusterAPIServiceCoroutineStub
    lateinit var authAPI: AuthServiceGrpcKt.AuthServiceCoroutineStub

    lateinit var futureServiceAPI: ServiceAPIServiceGrpc.ServiceAPIServiceFutureStub
    lateinit var futureTasksAPI: TasksAPIServiceGrpc.TasksAPIServiceFutureStub
    lateinit var futurePlayerAPI: PlayersServiceGrpc.PlayersServiceFutureStub

    lateinit var eventsAPI: EventServiceGrpcKt.EventServiceCoroutineStub

    fun connect(
        host: String = "127.0.0.1",
        port: Int = 6565,
        sslContext: SslContext? = null,
        secret: String
    ) {
        println("Connecting to $host:$port")

        val builder = NettyChannelBuilder.forAddress(InetSocketAddress(host, port))
            .eventLoopGroup(NioEventLoopGroup()) // force NIO transport
            .channelType(NioSocketChannel::class.java) // force TCP

        if (sslContext != null) {
            builder.sslContext(sslContext)
        } else {
            builder.usePlaintext()
        }

        channel = builder.build()

        serviceAPI =
            ServiceAPIServiceGrpcKt.ServiceAPIServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        tasksAPI =
            TasksAPIServiceGrpcKt.TasksAPIServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        playerAPI =
            PlayersServiceGrpcKt.PlayersServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        futureServiceAPI =
            ServiceAPIServiceGrpc.newFutureStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        futureTasksAPI =
            TasksAPIServiceGrpc.newFutureStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        futurePlayerAPI =
            PlayersServiceGrpc.newFutureStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        eventsAPI =
            EventServiceGrpcKt.EventServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        clusterAPI =
            ClusterAPIServiceGrpcKt.ClusterAPIServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
        authAPI =
            AuthServiceGrpcKt.AuthServiceCoroutineStub(channel)
                .withInterceptors(AuthClientInterceptor(secret))
    }

    companion object {
        init {
            Security.addProvider(BouncyCastleProvider())
        }

        fun buildClientSslContext(nodeCertPem: String, nodeKeyPem: String, caCertPem: String): SslContext {
            val nodeCert = parseCertificate(nodeCertPem)
            val nodeKey = parsePrivateKey(nodeKeyPem)
            val caCert = parseCertificate(caCertPem)

            return SslContextBuilder.forClient()
                .keyManager(nodeKey, nodeCert)
                .trustManager(caCert)
                .let { GrpcSslContexts.configure(it).build() }
        }

        private fun parseCertificate(pem: String): X509Certificate {
            val factory = CertificateFactory.getInstance("X.509")
            return ByteArrayInputStream(pem.toByteArray()).use {
                factory.generateCertificate(it) as X509Certificate
            }
        }

        private fun parsePrivateKey(pem: String): PrivateKey {
            PEMParser(StringReader(pem)).use { parser ->
                val obj = parser.readObject()
                val converter = JcaPEMKeyConverter().setProvider("BC")
                return when (obj) {
                    is PEMKeyPair -> converter.getKeyPair(obj).private
                    else -> error("Unsupported private key PEM format: ${obj?.javaClass}")
                }
            }
        }
    }
}
