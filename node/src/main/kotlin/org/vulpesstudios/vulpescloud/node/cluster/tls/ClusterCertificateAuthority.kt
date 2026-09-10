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

package org.vulpesstudios.vulpescloud.node.cluster.tls

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.*
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import java.io.StringReader
import java.io.StringWriter
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Security
import java.security.cert.X509Certificate
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Everything needed to run VulpesCloud's internal cluster CA.
 */
object ClusterCertificateAuthority {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    data class CaMaterial(
        val certificate: X509Certificate,
        val privateKey: PrivateKey,
    )

    /**
     * Run exactly once, during first-ever cluster bootstrap.
     * The resulting private key must be encrypted (e.g. with a key
     * derived from the cluster secret) before being persisted to the DB.
     */
    fun generateClusterCa(commonName: String = "VulpesCloud Cluster CA"): CaMaterial {
        val keyPair = generateRsaKeyPair()

        val now = Instant.now()
        val notBefore = Date.from(now.minus(Duration.ofMinutes(5)))
        val notAfter = Date.from(now.plus(Duration.ofDays(3650))) // 10y root

        val subject = X500Name("CN=$commonName")
        val serial = BigInteger.valueOf(now.toEpochMilli())

        val builder = JcaX509v3CertificateBuilder(
            subject, serial, notBefore, notAfter, subject, keyPair.public,
        )

        val extUtils = JcaX509ExtensionUtils()
        builder.addExtension(Extension.basicConstraints, true, BasicConstraints(true))
        builder.addExtension(
            Extension.keyUsage, true,
            KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign),
        )
        builder.addExtension(
            Extension.subjectKeyIdentifier, false,
            extUtils.createSubjectKeyIdentifier(keyPair.public),
        )

        val signer = signerFor(keyPair.private)
        val certHolder = builder.build(signer)
        val cert = JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder)

        return CaMaterial(cert, keyPair.private)
    }

    /**
     * Signs an incoming node CSR. `nodeId` is embedded as the certificate's
     * Subject Alternative Name so peers can identify the node without a
     * separate lookup.
     */
    fun signNodeCsr(
        csrPem: String,
        nodeId: String,
        ca: CaMaterial,
        validity: Duration = Duration.ofDays(60),
        ips: List<String> = emptyList(),
    ): X509Certificate {
        val csr = parseCsr(csrPem)

        val now = Instant.now()
        val notBefore = Date.from(now.minus(Duration.ofMinutes(5)))
        val notAfter = Date.from(now.plus(validity))
        val serial = BigInteger.valueOf(now.toEpochMilli())

        val issuer = X500Name(ca.certificate.subjectX500Principal.name)

        val builder = X509v3CertificateBuilder(
            issuer, serial, notBefore, notAfter, csr.subject, csr.subjectPublicKeyInfo,
        )

        builder.addExtension(Extension.basicConstraints, true, BasicConstraints(false))
        builder.addExtension(
            Extension.keyUsage, true,
            KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyEncipherment),
        )
        // Used by peers to match "which node is this cert for" during mTLS.
        val altNames = mutableListOf(
            GeneralName(GeneralName.dNSName, nodeId)
        )
        ips.forEach { ip ->
            altNames.add(GeneralName(GeneralName.iPAddress, ip))
        }

        builder.addExtension(
            Extension.subjectAlternativeName, false,
            GeneralNames(altNames.toTypedArray())
        )

        val signer = signerFor(ca.privateKey)
        val holder: X509CertificateHolder = builder.build(signer)
        return JcaX509CertificateConverter().setProvider("BC").getCertificate(holder)
    }

    // ---- helpers ----

    private fun generateRsaKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(4096)
        return kpg.generateKeyPair()
    }

    private fun signerFor(key: PrivateKey): ContentSigner =
        JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(key)

    private fun parseCsr(pem: String): PKCS10CertificationRequest {
        PEMParser(StringReader(pem)).use { parser ->
            return parser.readObject() as PKCS10CertificationRequest
        }
    }

    fun toPem(cert: X509Certificate): String = StringWriter().use { sw ->
        JcaPEMWriter(sw).use { it.writeObject(cert) }
        sw.toString()
    }

    fun toPem(key: PrivateKey): String = StringWriter().use { sw ->
        JcaPEMWriter(sw).use { it.writeObject(key) }
        sw.toString()
    }

    /** Needed after decrypting the CA key back out of the database. */
    fun privateKeyFromPem(pem: String): PrivateKey {
        PEMParser(StringReader(pem)).use { parser ->
            val obj = parser.readObject()
            val converter = org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter().setProvider("BC")
            return when (obj) {
                is org.bouncycastle.openssl.PEMKeyPair -> converter.getKeyPair(obj).private
                else -> error("Unsupported private key PEM format: ${obj?.javaClass}")
            }
        }
    }

    fun certificateFromPem(pem: String): X509Certificate {
        val factory = java.security.cert.CertificateFactory.getInstance("X.509")
        return java.io.ByteArrayInputStream(pem.toByteArray()).use {
            factory.generateCertificate(it) as X509Certificate
        }
    }
}

/** Used client-side when a node builds its own CSR. */
object NodeKeyMaterial {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    data class KeyAndCsr(val keyPair: KeyPair, val csrPem: String)

    fun generateCsr(nodeId: String): KeyAndCsr {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(4096)
        val keyPair = kpg.generateKeyPair()

        val subject = X500Name("CN=$nodeId")
        val csrBuilder = JcaPKCS10CertificationRequestBuilder(subject, keyPair.public)
        val signer = JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(keyPair.private)
        val csr: PKCS10CertificationRequest = csrBuilder.build(signer)

        val pem = StringWriter().use { sw ->
            JcaPEMWriter(sw).use { it.writeObject(csr) }
            sw.toString()
        }
        return KeyAndCsr(keyPair, pem)
    }
}
