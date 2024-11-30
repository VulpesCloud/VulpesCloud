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

package de.vulpescloud.node.command.parser

//import io.github.thecguygithub.node.Node
//import io.github.thecguygithub.node.command.source.CommandSource
//import io.github.thecguygithub.node.platforms.Platform
//import org.incendo.cloud.context.CommandContext
//import org.incendo.cloud.context.CommandInput
//import org.incendo.cloud.parser.ArgumentParseResult
//import org.incendo.cloud.parser.ArgumentParser
//import org.incendo.cloud.parser.ParserDescriptor
//
//class PlatformParser : ArgumentParser<CommandSource, Platform> {
//
//    override fun parse(
//        commandContext: CommandContext<CommandSource>,
//        commandInput: CommandInput,
//    ): ArgumentParseResult<Platform> {
//        val input = commandInput.peekString()
//
//        try {
//            val platform = Node.platformService!!.find(input)
//            if (platform != null) {
//                return ArgumentParseResult.success(platform)
//            } else {
//                return ArgumentParseResult.failure(NullPointerException(platform))
//            }
//        } catch (e: Exception) {
//            return ArgumentParseResult.failure(e)
//        }
//    }
//
//    companion object {
//        fun platformParser(): ParserDescriptor<CommandSource, Platform> {
//            return ParserDescriptor.of(PlatformParser(), Platform::class.java)
//        }
//    }
//
//
//}