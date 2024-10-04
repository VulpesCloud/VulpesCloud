package io.github.thecguygithub.node.command.specific

import io.github.thecguygithub.api.services.ClusterService
import io.github.thecguygithub.node.Node
import io.github.thecguygithub.node.command.CommandArgument
import io.github.thecguygithub.node.command.CommandContext


abstract class ServiceArgument(key: String?) : CommandArgument<ClusterService?>() {
}