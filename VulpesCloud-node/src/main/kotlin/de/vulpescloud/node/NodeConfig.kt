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

package de.vulpescloud.node


import de.vulpescloud.api.cluster.NodeInformation
import de.vulpescloud.api.language.Languages
import de.vulpescloud.node.config.LogLevels
import de.vulpescloud.node.config.RedisEndpointData
import de.vulpescloud.node.networking.mysql.MySQLEndpointData
import java.util.*

class NodeConfig {

    var uuid: UUID
    var name: String
    var redis: RedisEndpointData?
    var logLevel: LogLevels?
    var mysql: MySQLEndpointData?
    var nodes: MutableList<NodeInformation>
    var language: Languages
    var ranFirstSetup: Boolean


    init {
        this.uuid = UUID.randomUUID()
        this.name = "Node-1"
        this.redis = RedisEndpointData("default", "0.0.0.0", 6379, "password")
        this.logLevel = LogLevels.INFO
        this.mysql = MySQLEndpointData("vulpescloud", "", "vulpescloud", "127.0.0.1", 3306, false)
        this.nodes = mutableListOf()
        this.language = Languages.en_US
        this.ranFirstSetup = false
    }
}