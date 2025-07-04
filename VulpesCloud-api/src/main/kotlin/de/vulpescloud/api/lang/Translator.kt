package de.vulpescloud.api.lang

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.file.Paths
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.net.JarURLConnection
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.name
import kotlin.io.path.readText

class Translator {

    private lateinit var lang: Languages
    private val logger = LoggerFactory.getLogger(Translator::class.java)
    private val langDir = Paths.get("launcher/Language")
    private val langJson = JSONObject()

    fun setLang(setLang: Languages) {
        lang = setLang
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun loadFromDefaultClassPath() {
        GlobalScope.launch {
            Files.createDirectories(langDir)
            val finalLangDir = langDir.resolve(lang.name)
            Files.createDirectories(finalLangDir)

            logger.info("Loading files for Language &m${lang.name}")

            this::class.java.classLoader.getResource("lang/${lang.name}").let { url ->
                if (url == null) {
                    logger.error("Cannot get Language data, URL is null, check location of Language Data")
                    return@launch
                }
                if (url.protocol != "jar") {
                    logger.error("Protocol is not 'jar', i don't really know why this could happen, but anyways")
                    return@launch
                }
                val connection = url.openConnection() as JarURLConnection
                val jarFile = connection.jarFile

                jarFile.entries().asSequence()
                    .filter { it.name.startsWith("lang/${lang.name}/") && !it.isDirectory }
                    .forEach { entry ->
                        val relativePath = entry.name.removePrefix("lang/${lang.name}/")
                        val outPath = finalLangDir.resolve(relativePath)
                        jarFile.getInputStream(entry).use { input ->
                            Files.createDirectories(outPath.parent)
                            Files.copy(input, outPath, StandardCopyOption.REPLACE_EXISTING)
                            logger.debug("Copied file: &m{} &7to <yellow>{} &7additional &b{}", entry.name, outPath, outPath.toFile().absolutePath)
                        }
                    }
            }
            Files.walk(finalLangDir).filter { it.toString().endsWith(".json") }.forEach {
                logger.debug("Loading JSON from file: &m${it.parent.parent.name}/${it.parent.name}/${it.name}")
                JSONObject(it.readText()).let { json ->
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        langJson.put(key, json.get(key))
                    }
                }
            }
        }
    }

    fun trans(key: String): String {
        return if (langJson.has(key)) {
            langJson.getString(key)
        } else {
            "&cLanguage String not found for key $key"
        }
    }
}
