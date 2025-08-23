package de.vulpescloud.node.grpc.security

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.*
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

object CertGen {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    /**
     * Loads existing PEM files or generates new ones if they don't exist.
     * @param certFile The file to load/save the certificate.
     * @param keyFile The file to load/save the private key.
     * @param commonName The common name for the self-signed certificate if generated.
     * @return A pair containing the private key and the X.509 certificate.
     */
    fun loadOrCreate(certFile: File, keyFile: File, commonName: String = "VulpesCloudNode"): Pair<PrivateKey, X509Certificate> {
        return if (certFile.exists() && keyFile.exists()) {
            val certPem = certFile.readText()
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replace("\\s".toRegex(), "")
            val certBytes = Base64.getDecoder().decode(certPem)
            val certHolder = X509CertificateHolder(certBytes)
            val cert = JcaX509CertificateConverter().getCertificate(certHolder)

            val keyPem = keyFile.readText()
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\s".toRegex(), "")
            val keyBytes = Base64.getDecoder().decode(keyPem)
            val privateKey = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(keyBytes))

            privateKey to cert
        } else {
            val (key, cert) = generateSelfSignedCert(commonName)
            savePem(cert, key, certFile, keyFile)
            key to cert
        }
    }

    /**
     * Generates a self-signed X.509 certificate and its corresponding private key.
     */
    private fun generateSelfSignedCert(commonName: String = "VulpesCloudNode"): Pair<PrivateKey, X509Certificate> {
        val keyPairGen = KeyPairGenerator.getInstance("RSA")
        keyPairGen.initialize(4096)
        val keyPair: KeyPair = keyPairGen.generateKeyPair()

        val now = Date()
        val farFuture = Date(now.time + 1000L * 365 * 24 * 60 * 60 * 1000) // ~1000 Jahre

        val subject = X500Name("CN=$commonName")
        val serial = BigInteger.valueOf(System.currentTimeMillis())

        val certBuilder = JcaX509v3CertificateBuilder(
            subject, serial, now, farFuture, subject, keyPair.public
        )
        val signer = JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.private)
        val certHolder: X509CertificateHolder = certBuilder.build(signer)
        val cert: X509Certificate = JcaX509CertificateConverter().getCertificate(certHolder)

        return keyPair.private to cert
    }

    /**
     * Export pem
     */
    private fun savePem(cert: X509Certificate, key: PrivateKey, certFile: File, keyFile: File) {
        certFile.parentFile?.mkdirs()
        keyFile.parentFile?.mkdirs()
        certFile.writeText(
            "-----BEGIN CERTIFICATE-----\n" +
                    Base64.getEncoder().encodeToString(cert.encoded).chunked(64).joinToString("\n") +
                    "\n-----END CERTIFICATE-----\n"
        )
        keyFile.writeText(
            "-----BEGIN PRIVATE KEY-----\n" +
                    Base64.getEncoder().encodeToString(key.encoded).chunked(64).joinToString("\n") +
                    "\n-----END PRIVATE KEY-----\n"
        )
    }
}
