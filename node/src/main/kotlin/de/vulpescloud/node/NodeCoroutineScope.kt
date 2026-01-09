package de.vulpescloud.node

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object NodeCoroutineScope : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default)