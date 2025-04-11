package de.vulpescloud.api.lang

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.file.Paths
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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
            Files.createDirectory(langDir)
            val finalLangDir = langDir.resolve(lang.name)
            Files.createDirectory(finalLangDir)

            this::class.java.classLoader.getResourceAsStream("lang/${lang.name}").let {
                if (it == null) {
                    logger.error("Cannot get Language data, InputStream is null, check location of Language Data")
                    return@launch
                }
                Files.copy(it, finalLangDir, StandardCopyOption.REPLACE_EXISTING)
            }
            Files.walk(finalLangDir).filter { it.toString().endsWith(".json") }.forEach {
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
