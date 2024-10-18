package io.github.thecguygithub.node.util

import java.io.InputStream
import java.net.URI
import kotlin.random.Random


object StringUtils {

    private const val ALPHABET = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private val RANDOM: Random = Random

    fun randomString(length: Int): String {
        val builder = StringBuilder()

        for (i in 0 until length) {
            builder.append(ALPHABET[getRandomNumber(ALPHABET.length)])
        }

        return builder.toString()
    }

    fun getRandomNumber(max: Int): Int {
        return RANDOM.nextInt(max)
    }

    fun downloadStringContext(link: String?): String {
        val stream: InputStream? = link?.let { URI(it).toURL().openStream() }
        val context: String = stream?.let { String(it.readAllBytes()) }.toString()
        stream?.close()
        return context
    }

}