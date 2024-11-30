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

package de.vulpescloud.api.tasks

import de.vulpescloud.api.Detail
import de.vulpescloud.api.Named
import de.vulpescloud.api.services.ClusterService
import de.vulpescloud.api.version.VersionInfo



interface Task : Named, Detail {

    fun nodes(): List<String?>

    fun templates(): List<String?>

    fun maxMemory(): Int

    fun maxPlayers(): Int

    fun staticService(): Boolean

    fun minOnlineCount(): Int

    fun serviceCount(): Long?

    fun services(): List<ClusterService?>?

    fun version(): VersionInfo

    fun maintenance(): Boolean

    fun startPort(): Int

    fun update()

    fun fallback(): Boolean

}