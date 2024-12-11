package de.vulpescloud.api.utils

import kotlin.random.Random

object StringUtils {

    private const val ALPHABET = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private val RANDOM = Random

    fun generateRandomString(length: Int): String {
        val builder = StringBuilder()
        for (i in 0 until length) {
            builder.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)])
        }
        return builder.toString()
    }

}