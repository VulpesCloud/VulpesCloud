package de.vulpescloud.api.event.events.task

import de.vulpescloud.api.tasks.Task

data class TaskCreateEvent(val task: Task)