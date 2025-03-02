package de.vulpescloud.api.event.events.task

import de.vulpescloud.api.event.Event
import de.vulpescloud.api.tasks.Task

data class TaskUpdateEvent(val task: Task) : Event {
    override fun name(): String {
        return "TaskUpdateEvent"
    }
}
