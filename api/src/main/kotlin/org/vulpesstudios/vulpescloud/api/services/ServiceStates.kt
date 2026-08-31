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

package org.vulpesstudios.vulpescloud.api.services

import build.buf.gen.vulpescloud.services.v1.ServiceState

enum class ServiceStates {
    UNKNOWN,
    RUNNING,
    STARTING,
    PREPARED,
    STOPPED;

    fun toServiceState(): ServiceState {
        return when (this) {
            UNKNOWN -> ServiceState.SERVICE_STATE_UNSPECIFIED
            RUNNING -> ServiceState.SERVICE_STATE_RUNNING
            STARTING -> ServiceState.SERVICE_STATE_STARTING
            PREPARED -> ServiceState.SERVICE_STATE_PREPARED
            STOPPED -> ServiceState.SERVICE_STATE_STOPPED
        }
    }
}

fun ServiceState.toServiceStates(): ServiceStates {
    return when (this) {
        ServiceState.SERVICE_STATE_UNSPECIFIED -> ServiceStates.UNKNOWN
        ServiceState.SERVICE_STATE_RUNNING -> ServiceStates.RUNNING
        ServiceState.SERVICE_STATE_STARTING -> ServiceStates.STARTING
        ServiceState.SERVICE_STATE_PREPARED -> ServiceStates.PREPARED
        ServiceState.SERVICE_STATE_STOPPED -> ServiceStates.STOPPED
        ServiceState.UNRECOGNIZED -> ServiceStates.UNKNOWN
    }
}
