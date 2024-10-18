package io.github.thecguygithub.node.service.utils

object ClusterDefaultArgs {
    val ARGUMENTS: List<String> = listOf(
        "-XX:+UseG1GC",
        "-XX:+ParallelRefProcEnabled",
        "-XX:MaxGCPauseMillis=200",
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+DisableExplicitGC",
        "-XX:+AlwaysPreTouch",
        "-XX:G1NewSizePercent=30",
        "-XX:G1MaxNewSizePercent=40",
        "-XX:G1HeapRegionSize=8M",
        "-XX:G1ReservePercent=20",
        "-XX:G1HeapWastePercent=5",
        "-XX:G1MixedGCCountTarget=4",
        "-XX:InitiatingHeapOccupancyPercent=15",
        "-XX:G1MixedGCLiveThresholdPercent=90",
        "-XX:G1RSetUpdatingPauseTimePercent=5",
        "-XX:SurvivorRatio=32",
        "-XX:+PerfDisableSharedMem",
        "-XX:MaxTenuringThreshold=1",
        "-Dusing.aikars.flags=https://mcflags.emc.gs",
        "-Daikars.new.flags=true",
        "-XX:-UseAdaptiveSizePolicy",
        "-XX:CompileThreshold=100",
        "-Dio.netty.recycler.maxCapacity=0",
        "-Dio.netty.recycler.maxCapacity.default=0",
        "-Djline.terminal=jline.UnsupportedTerminal",
        "-Dfile.encoding=UTF-8",
        "-Dclient.encoding.override=UTF-8",
        "-DIReallyKnowWhatIAmDoingISwear=true"
    )
}