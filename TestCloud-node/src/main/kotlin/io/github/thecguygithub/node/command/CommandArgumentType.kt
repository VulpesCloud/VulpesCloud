package io.github.thecguygithub.node.command

import io.github.thecguygithub.node.command.type.*
import org.jetbrains.annotations.Contract


object CommandArgumentType {

    // fun ModuleArgument(key: String?): CommandArgument<LoadedModule> {
    //     return ModuleArgument(key)
    // }

    // fun ClusterTask(key: String?): CommandArgument<ClusterTask> {
    //     return GroupArgument(key)
    // }

    // fun ClusterService(key: String?): ServiceArgument {
    //     return ServiceArgument(key)
    // }

    // fun PlatformVersion(key: String?): CommandArgument<PlatformVersion> {
    //     return PlatformBindVersionArgument(key)
    // }

    // fun Player(key: String?): CommandArgument<ClusterPlayer> {
    //     return PlayerArgument(key)
    // }

    // fun Platform(key: String?): CommandArgument<Platform> {
    //     return PlatformArgument(key)
    // }

    fun <E : Enum<E>> Enum(enumClass: Class<E>?, key: String): EnumArgument<out Enum<*>> {
        return EnumArgument(enumClass, key)
    }

    // fun NodeEndpoint(key: String?): CommandArgument<NodeEndpoint> {
    //     return NodeEndpointArgument(key)
    // }

    @Contract("_ -> new")
    fun Integer(key: String?): IntArgument {
        return IntArgument(key)
    }

    @Contract("_ -> new")
    fun StringArray(key: String?): StringArrayArgument {
        return StringArrayArgument(key)
    }

    @Contract("_ -> new")
    fun Boolean(key: String?): BooleanArgument {
        return BooleanArgument(key)
    }


    @Contract("_ -> new")
    fun Text(key: String?): TextArgument {
        return TextArgument(key)
    }

    @Contract("_ -> new")
    fun Keyword(key: String?): KeywordArgument {
        return KeywordArgument(key)
    }

}