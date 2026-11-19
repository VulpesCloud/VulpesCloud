/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.vulpesstudios.vulpescloud.api.templates

import build.buf.gen.vulpescloud.templates.v1.TemplateJobPhase as ProtoTemplateJobPhase

enum class TemplateJobPhase {
    ACCEPTED,
    SCANNING_SOURCE,
    SCANNING_DESTINATION,
    PREPARING_DESTINATION,
    COPYING_FILES,
    PURGING_DESTINATION,
    VERIFYING_CHECKPOINT,
    DELETING_SOURCE,
    ;

    fun toDefinition(): ProtoTemplateJobPhase = when (this) {
        ACCEPTED -> ProtoTemplateJobPhase.TEMPLATE_JOB_PHASE_ACCEPTED
        SCANNING_SOURCE -> ProtoTemplateJobPhase.TEMPLATE_JOB_PHASE_SCANNING_SOURCE
        SCANNING_DESTINATION -> ProtoTemplateJobPhase.TEMPLATE_JOB_PHASE_SCANNING_DESTINATION
        PREPARING_DESTINATION -> ProtoTemplateJobPhase.TEMPLATE_JOB_PHASE_PREPARING_DESTINATION
        COPYING_FILES -> ProtoTemplateJobPhase.TEMPLATE_JOB_PHASE_COPYING_FILES
        PURGING_DESTINATION -> ProtoTemplateJobPhase.TEMPLATE_JOB_PHASE_PURGING_DESTINATION
        VERIFYING_CHECKPOINT -> ProtoTemplateJobPhase.TEMPLATE_JOB_PHASE_VERIFYING_CHECKPOINT
        DELETING_SOURCE -> ProtoTemplateJobPhase.TEMPLATE_JOB_PHASE_DELETING_SOURCE
    }

    companion object {
        fun fromDefinition(phase: ProtoTemplateJobPhase): TemplateJobPhase = when (phase) {
            ProtoTemplateJobPhase.TEMPLATE_JOB_PHASE_SCANNING_SOURCE -> SCANNING_SOURCE
            ProtoTemplateJobPhase.TEMPLATE_JOB_PHASE_SCANNING_DESTINATION -> SCANNING_DESTINATION
            ProtoTemplateJobPhase.TEMPLATE_JOB_PHASE_PREPARING_DESTINATION -> PREPARING_DESTINATION
            ProtoTemplateJobPhase.TEMPLATE_JOB_PHASE_COPYING_FILES -> COPYING_FILES
            ProtoTemplateJobPhase.TEMPLATE_JOB_PHASE_PURGING_DESTINATION -> PURGING_DESTINATION
            ProtoTemplateJobPhase.TEMPLATE_JOB_PHASE_VERIFYING_CHECKPOINT -> VERIFYING_CHECKPOINT
            ProtoTemplateJobPhase.TEMPLATE_JOB_PHASE_DELETING_SOURCE -> DELETING_SOURCE
            else -> ACCEPTED
        }
    }
}
