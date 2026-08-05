package de.vulpescloud.node.commands

import de.vulpescloud.node.Node
import de.vulpescloud.node.cluster.tls.ClusterCertificateAuthority
import de.vulpescloud.node.cluster.tls.NodeKeyMaterial
import de.vulpescloud.node.cluster.tls.CaKeyEncryption
import de.vulpescloud.node.command.CommandSource
import de.vulpescloud.node.command.ConsoleCommandSource
import de.vulpescloud.node.command.annotation.SpecificCommandSource
import de.vulpescloud.node.db.DatabaseProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import java.io.File
import de.vulpescloud.node.cluster.tls.StoredCa

@SpecificCommandSource(ConsoleCommandSource::class)
class TlsCommand {

    @Command("tls export <nodeId>")
    suspend fun exportTls(source: CommandSource, @Argument("nodeId") nodeId: String) {
        val database = DatabaseProvider.getMainDatabaseProvider().getOrCreateDatabase("cluster_tls")
        val storedCaElement = database.get("ca") ?: run {
            source.sendMessage("<red>Cluster TLS is not initialized!</red>")
            return
        }
        val storedCa = Json.decodeFromJsonElement<StoredCa>(storedCaElement)
        
        val secret = Node.instance.secret
        val caKeyPem = try {
            CaKeyEncryption.decrypt(storedCa.encryptedKey, secret)
        } catch (e: Exception) {
            source.sendMessage("<red>Failed to decrypt CA key. Is the cluster secret correct?</red>")
            return
        }

        val caCert = ClusterCertificateAuthority.certificateFromPem(storedCa.caCertPem)
        val caPrivateKey = ClusterCertificateAuthority.privateKeyFromPem(caKeyPem)
        val ca = ClusterCertificateAuthority.CaMaterial(caCert, caPrivateKey)

        val (keyPair, csrPem) = NodeKeyMaterial.generateCsr(nodeId)
        
        // Try to find the node's IP in the cluster config to include it in the SAN
        val ips = try {
            val clusterConfig = Node.instance.clusterProvider.getClusterConfig()
            val nodeDetails = clusterConfig.nodes.find { it.name == nodeId || it.uuid.toString() == nodeId }
            if (nodeDetails != null) listOf(nodeDetails.host) else emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val signedCert = ClusterCertificateAuthority.signNodeCsr(
            csrPem = csrPem,
            nodeId = nodeId,
            ca = ca,
            ips = ips
        )

        val exportDir = File("exports/tls/$nodeId")
        exportDir.mkdirs()
        File(exportDir, "node.key.pem").writeText(ClusterCertificateAuthority.toPem(keyPair.private))
        File(exportDir, "node.cert.pem").writeText(ClusterCertificateAuthority.toPem(signedCert))
        File(exportDir, "ca.cert.pem").writeText(storedCa.caCertPem)

        source.sendMessage("<green>Successfully exported TLS credentials for <white>$nodeId</white> to <white>${exportDir.path}</white></green>")
        source.sendMessage("<gray>This folder contains:</gray>")
        source.sendMessage("<gray> - <white>node.key.pem</white> (Private Key)</gray>")
        source.sendMessage("<gray> - <white>node.cert.pem</white> (Certificate)</gray>")
        source.sendMessage("<gray> - <white>ca.cert.pem</white> (Root CA Certificate)</gray>")
    }
}
