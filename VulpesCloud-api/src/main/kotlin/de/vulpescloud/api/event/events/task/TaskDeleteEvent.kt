package de.vulpescloud.api.event.events.task

import de.vulpescloud.api.event.Event
import de.vulpescloud.api.tasks.Task

data class TaskDeleteEvent(val task: Task) : Event {
    override fun name(): String {
        return "TaskDeleteEvent"
    }
}
