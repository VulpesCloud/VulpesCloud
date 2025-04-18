package de.vulpescloud.node

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

abstract class Scheduler : CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.Default + job

    fun cancel() {
        job.cancel()
    }

    abstract fun run(): Job
}
