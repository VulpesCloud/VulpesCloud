package de.vulpescloud.node.grpc.security

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.*
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
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

object CertGen {

    init {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    fun loadOrCreate(
        certFile: File,
        keyFile: File,
        commonName: String = "localhost"
    ): Pair<PrivateKey, X509Certificate> {
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
            val privateKey = KeyFactory.getInstance("RSA")
                .generatePrivate(PKCS8EncodedKeySpec(keyBytes))

            privateKey to cert
        } else {
            val (key, cert) = generateSelfSignedCert(commonName)
            savePem(cert, key, certFile, keyFile)
            key to cert
        }
    }

    private fun generateSelfSignedCert(
        commonName: String = "localhost"
    ): Pair<PrivateKey, X509Certificate> {
        val keyPairGen = KeyPairGenerator.getInstance("RSA")
        keyPairGen.initialize(4096)
        val keyPair = keyPairGen.generateKeyPair()

        val now = Instant.now()
        val notBefore = Date.from(now.minus(1, ChronoUnit.DAYS))
        val notAfter = Date.from(now.plus(365, ChronoUnit.DAYS))

        val subject = X500Name("CN=$commonName")
        val serial = BigInteger(160, SecureRandom())

        val builder = JcaX509v3CertificateBuilder(
            subject,
            serial,
            notBefore,
            notAfter,
            subject,
            keyPair.public
        )

        val sanNames = GeneralNames(
            arrayOf(
                GeneralName(GeneralName.dNSName, "localhost"),
                GeneralName(GeneralName.iPAddress, "127.0.0.1"),
                GeneralName(GeneralName.iPAddress, "::1")
            )
        )
        builder.addExtension(Extension.subjectAlternativeName, false, sanNames)

        builder.addExtension(Extension.basicConstraints, true, BasicConstraints(false))

        builder.addExtension(
            Extension.keyUsage,
            true,
            KeyUsage(
                KeyUsage.digitalSignature or
                        KeyUsage.keyEncipherment
            )
        )

        builder.addExtension(
            Extension.extendedKeyUsage,
            false,
            ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth)
        )

        val signer = JcaContentSignerBuilder("SHA256withRSA")
            .build(keyPair.private)

        val certHolder = builder.build(signer)
        val cert = JcaX509CertificateConverter()
            .getCertificate(certHolder)

        cert.verify(keyPair.public)

        return keyPair.private to cert
    }

    private fun savePem(cert: X509Certificate, key: PrivateKey, certFile: File, keyFile: File) {
        certFile.parentFile?.mkdirs()
        keyFile.parentFile?.mkdirs()
        certFile.writeText(
            buildString {
                appendLine("-----BEGIN CERTIFICATE-----")
                appendLine(Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(cert.encoded))
                appendLine("-----END CERTIFICATE-----")
            }
        )
        keyFile.writeText(
            buildString {
                appendLine("-----BEGIN PRIVATE KEY-----")
                appendLine(Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(key.encoded))
                appendLine("-----END PRIVATE KEY-----")
            }
        )
    }
}