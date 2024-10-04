package io.github.thecguygithub.launcher.dependency


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

object SubDependencyHelper {

    fun getPomUrl(groupId: String, artifactId: String, version: String): String {
        return "https://repo1.maven.org/maven2/${groupId.replace('.', '/')}/$artifactId/$version/$artifactId-$version.pom"
    }

    fun findSubDependencies(dependency: Dependency): MutableList<Dependency> {
        val subDependencies = mutableListOf<Dependency>()

        val document = fetchPomXmlDocument(dependency.groupId, dependency.artifactId, dependency.version)
        val dependencyNodes = document.getElementsByTagName("dependency")

        for (i in 0 until dependencyNodes.length) {
            val dependencyElement = dependencyNodes.item(i) as Element
            val groupIdValue = getTextContent(dependencyElement, "groupId")
            val artifactIdValue = getTextContent(dependencyElement, "artifactId")
            var versionValue = getTextContent(dependencyElement, "version")

            if (versionValue.isNullOrEmpty() || isPlaceholder(versionValue)) {
                versionValue = getLatestStableVersion(groupIdValue!!, artifactIdValue!!)
            }

            val subDependency = Dependency(groupIdValue!!, artifactIdValue!!, versionValue)
            subDependencies.add(subDependency)
        }

        return subDependencies
    }

    fun getLatestStableVersion(groupId: String, artifactId: String): String {
        val document = fetchMetadataXmlDocument(groupId, artifactId)

        val versionsElement = document.getElementsByTagName("versions").item(0) as Element
        val versionNodes = versionsElement.getElementsByTagName("version")

        var latestStableVersion: String? = null
        for (i in 0 until versionNodes.length) {
            val version = versionNodes.item(i).textContent
            if (isStableVersion(version)) {
                latestStableVersion = version
            }
        }

        return latestStableVersion ?: throw IllegalStateException("Unable to determine latest stable version for $groupId:$artifactId")
    }

    private fun fetchPomXmlDocument(groupId: String, artifactId: String, version: String): Document {
        val url = URL(getPomUrl(groupId, artifactId, version))
        return fetchXmlDocument(url)
    }

    private fun fetchMetadataXmlDocument(groupId: String, artifactId: String): Document {
        val metadataUrl = URL("https://repo1.maven.org/maven2/${groupId.replace(".", "/")}/$artifactId/maven-metadata.xml")
        return fetchXmlDocument(metadataUrl)
    }

    private fun fetchXmlDocument(url: URL): Document {
        val connection = url.openConnection() as HttpURLConnection
        connection.inputStream.use { inputStream ->
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            return builder.parse(InputSource(inputStream))
        }
    }

    private fun isPlaceholder(versionValue: String): Boolean {
        return versionValue.startsWith("\${") && versionValue.endsWith("}")
    }

    private fun isStableVersion(version: String): Boolean {
        return !version.contains("alpha") && !version.contains("beta")
    }

    private fun getTextContent(element: Element, tagName: String): String? {
        val nodeList = element.getElementsByTagName(tagName)
        if (nodeList.length > 0) {
            return nodeList.item(0).textContent
        }
        return null
    }
}