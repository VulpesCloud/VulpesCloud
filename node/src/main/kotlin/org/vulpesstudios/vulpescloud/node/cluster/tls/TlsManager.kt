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

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.slf4j.LoggerFactory
import org.vulpesstudios.vulpescloud.node.db.DatabaseProvider
import org.vulpesstudios.vulpescloud.node.utils.AddressUtils
import java.io.File
import java.security.KeyPair

@Serializable
data class StoredCa(
    val caCertPem: String,
    val encryptedKey: CaKeyEncryption.EncryptedBlob,
)

data class NodeCertBundle(
    val nodeKey: KeyPair,
    val nodeCertPem: String,
    val caCertPem: String,
)

class TlsManager(
    private val clusterSecret: String,
    private val nodeId: String
) {
    private val logger = LoggerFactory.getLogger(TlsManager::class.java)
    private val database = DatabaseProvider.getMainDatabaseProvider().getOrCreateDatabase("cluster_tls")

    suspend fun isClusterInitialized(): Boolean {
        return database.get("ca") != null
    }

    suspend fun initCluster() {
        if (isClusterInitialized()) {
            logger.info("Cluster TLS already initialized.")
            return
        }

        logger.info("Initializing Cluster TLS CA...")
        val ca = ClusterCertificateAuthority.generateClusterCa()
        val caCertPem = ClusterCertificateAuthority.toPem(ca.certificate)
        val caKeyPem = ClusterCertificateAuthority.toPem(ca.privateKey)

        val encryptedKey = CaKeyEncryption.encrypt(caKeyPem, clusterSecret)
        val storedCa = StoredCa(caCertPem, encryptedKey)

        database.upsert("ca", Json.encodeToJsonElement(storedCa))
        logger.info("Cluster TLS CA initialized and stored in database.")
    }

    suspend fun bootstrapNode(): NodeCertBundle {
        val certDir = File("launcher/.secret/certs")
        if (hasLocalCertBundle(certDir)) {
            logger.info("Loading existing local certificate bundle.")
            return loadLocalBundle(certDir)
        }

        if (!isClusterInitialized()) {
            // In a real scenario, we might want to wait or fail.
            // But if this is the first node, we might want to init it.
            // The issue says: "The Cluster will have a root CA stored in the Cluster Database"
            // "run cluster init explicitly before starting a node" was in the example.
            // Let's check if we should auto-init or not.
            // For now, let's assume if it's not initialized, we try to init it if we are the first node?
            // Actually, Node.kt has FirstSetup.
            initCluster()
        }

        logger.info("Bootstrapping node certificate...")
        val storedCaElement = database.get("ca") ?: throw IllegalStateException("Cluster CA not found in database!")
        val storedCa = Json.decodeFromJsonElement<StoredCa>(storedCaElement)

        val caKeyPem = CaKeyEncryption.decrypt(storedCa.encryptedKey, clusterSecret)
        val caCert = ClusterCertificateAuthority.certificateFromPem(storedCa.caCertPem)
        val caPrivateKey = ClusterCertificateAuthority.privateKeyFromPem(caKeyPem)
        val ca = ClusterCertificateAuthority.CaMaterial(caCert, caPrivateKey)

        val (keyPair, csrPem) = NodeKeyMaterial.generateCsr(nodeId)
        val signedCert = ClusterCertificateAuthority.signNodeCsr(
            csrPem = csrPem,
            nodeId = nodeId,
            ca = ca,
            ips = AddressUtils.getAvailableAddresses()
        )

        val bundle = NodeCertBundle(
            nodeKey = keyPair,
            nodeCertPem = ClusterCertificateAuthority.toPem(signedCert),
            caCertPem = storedCa.caCertPem
        )

        persist(bundle, certDir)
        logger.info("Node certificate bootstrapped and persisted.")
        return bundle
    }

    private fun persist(bundle: NodeCertBundle, dir: File) {
        dir.mkdirs()
        File(dir, "node.key.pem").writeText(ClusterCertificateAuthority.toPem(bundle.nodeKey.private))
        File(dir, "node.cert.pem").writeText(bundle.nodeCertPem)
        File(dir, "ca.cert.pem").writeText(bundle.caCertPem)
    }

    private fun loadLocalBundle(dir: File): NodeCertBundle {
        val nodeKeyPem = File(dir, "node.key.pem").readText()
        val nodeCertPem = File(dir, "node.cert.pem").readText()
        val caCertPem = File(dir, "ca.cert.pem").readText()

        val nodeKey = ClusterCertificateAuthority.privateKeyFromPem(nodeKeyPem)
        // We only need the private key for the server/client, but NodeCertBundle wants a KeyPair.
        // We can just use null for public key if it's not used, or better, don't use KeyPair there if not needed.
        // But for consistency with example:
        return NodeCertBundle(
            nodeKey = KeyPair(null, nodeKey),
            nodeCertPem = nodeCertPem,
            caCertPem = caCertPem
        )
    }

    private fun hasLocalCertBundle(dir: File): Boolean =
        File(dir, "node.key.pem").exists() &&
                File(dir, "node.cert.pem").exists() &&
                File(dir, "ca.cert.pem").exists()
}
