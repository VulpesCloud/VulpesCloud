/*
 * MIT License
 *
 * Copyright (c) 2024 VulpesCloud
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.vulpescloud.node.util

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