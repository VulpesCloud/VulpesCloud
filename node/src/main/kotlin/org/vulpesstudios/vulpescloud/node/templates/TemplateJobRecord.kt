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

package org.vulpesstudios.vulpescloud.node.templates

import java.security.MessageDigest
import java.util.UUID
import kotlinx.serialization.Serializable
import org.vulpesstudios.vulpescloud.api.templates.TemplateJobProgress

@Serializable
enum class TemplateJobPhase {
    ACCEPTED,
    SCANNING_SOURCE,
    SCANNING_DESTINATION,
    PREPARING_DESTINATION,
    COPYING,
    PURGING,
    VERIFYING_CHECKPOINT,
    DELETING_SOURCE,
}

@Serializable
data class TemplateFileSnapshot(
    val path: String,
    val directory: Boolean,
    val size: Long,
    val modifiedAt: Long,
)

@Serializable
data class TemplateJobRecord(
    val progress: TemplateJobProgress,
    val workerToken: String = UUID.randomUUID().toString(),
    val phase: TemplateJobPhase = TemplateJobPhase.ACCEPTED,
    val sourceInventory: List<TemplateFileSnapshot> = emptyList(),
    val sourceDirectoryInventory: List<String> = emptyList(),
    val destinationExisted: Boolean = false,
    val initialDestinationInventory: List<TemplateFileSnapshot> = emptyList(),
    val expectedDestinationInventory: List<TemplateFileSnapshot> = emptyList(),
    val completedPaths: Set<String> = emptySet(),
    val purgedPaths: Set<String> = emptySet(),
    val inFlightPath: String? = null,
    val inFlightPurgePath: String? = null,
    val workerNodeName: String? = null,
    val workerLeaseUntil: Long = 0,
    val cancelRequested: Boolean = false,
    val lockKeys: Set<String> = emptySet(),
)

@Serializable
data class TemplateJobIdentity(
    val jobId: String,
    val token: String,
)

@Serializable
data class TemplateJobLock(
    val jobId: String,
    val lockedAt: Long,
    val description: String,
    val leaseUntil: Long = 0,
)

object TemplateLockKey {
    fun template(templateId: String): String = "template:$templateId"

    fun reservation(name: String, storageName: String, nodeName: String?): String {
        val identity = "$name\u0000$storageName\u0000${nodeName.orEmpty()}"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "reservation:$digest"
    }
}
