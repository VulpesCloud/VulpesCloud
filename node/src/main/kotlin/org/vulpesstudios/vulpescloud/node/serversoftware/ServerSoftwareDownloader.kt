/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.node.serversoftware

import org.vulpesstudios.vulpescloud.api.serversoftware.ServerSoftware
import java.net.URI
import java.nio.file.Path

interface ServerSoftwareDownloader {
    suspend fun downloadSoftware(version: String)

    suspend fun getDownloadUrl(version: String): URI

    suspend fun getAvailableVersions(refreshList: Boolean = false): List<ServerSoftware>

    suspend fun getLatestVersion(version: String? = null): ServerSoftware

    suspend fun getLatestVersionPath(version: String): Path

    val displayName: String
    val id: String
}
