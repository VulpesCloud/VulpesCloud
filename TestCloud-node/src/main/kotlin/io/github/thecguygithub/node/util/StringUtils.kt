package io.github.thecguygithub.node.util

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

}