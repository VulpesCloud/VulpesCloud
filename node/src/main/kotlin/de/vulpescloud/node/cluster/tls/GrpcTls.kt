package de.vulpescloud.node.cluster.tls

import io.grpc.netty.GrpcSslContexts
import io.netty.handler.ssl.ClientAuth
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.security.PrivateKey
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Every node authenticates every other node the same way: verify the
 * peer's cert chains up to the ONE shared cluster CA cert.
 */
object GrpcTls {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    /** Used when starting this node's gRPC server. Requires client certs too (mTLS). */
    fun buildServerSslContext(nodeCertPem: String, nodeKeyPem: String, caCertPem: String): SslContext {
        val nodeCert = parseCertificate(nodeCertPem)
        val nodeKey = parsePrivateKey(nodeKeyPem)
        val caCert = parseCertificate(caCertPem)

        return SslContextBuilder.forServer(nodeKey, nodeCert)
            .trustManager(caCert)
            .clientAuth(ClientAuth.REQUIRE)
            .let { GrpcSslContexts.configure(it).build() }
    }

    /** Used when this node dials another node. Also presents its own cert (mTLS). */
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
