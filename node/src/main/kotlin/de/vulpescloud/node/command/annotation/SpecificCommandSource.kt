package de.vulpescloud.node.command.annotation

import de.vulpescloud.node.command.CommandSource
import kotlin.reflect.KClass

annotation class SpecificCommandSource(val value: KClass<out CommandSource>)
