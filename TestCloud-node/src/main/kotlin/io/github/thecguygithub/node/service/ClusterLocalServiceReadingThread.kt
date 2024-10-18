package io.github.thecguygithub.node.service

import io.github.thecguygithub.node.Node
import lombok.SneakyThrows
import java.nio.charset.StandardCharsets
import java.util.*


class ClusterLocalServiceReadingThread : Thread() {
    @SneakyThrows
    override fun run() {
        while (!isInterrupted) {
            for (service in Node.serviceProvider?.services()!!) {
                if (service is ClusterLocalServiceImpl) {
                    appendServiceLog(service)
                }
            }
            try {
                sleep(LOG_UPDATE_CYCLE)
            } catch (ignore: InterruptedException) {
            }
        }
    }

    @SneakyThrows
    private fun appendServiceLog(service: ClusterLocalServiceImpl) {
        if (service.process != null) {
            val inputStream = service.process!!.inputStream
            val bytes = ByteArray(2048)
            var length: Int

            val logs = ArrayList<String>()

            while (inputStream.read(bytes).also { length = it } != -1) {
                // Convert the read bytes to a String and split by newlines
                val logChunk = String(bytes, 0, length, StandardCharsets.UTF_8)
                logs.addAll(
                    listOf(
                        *String(bytes, 0, length, StandardCharsets.UTF_8).split("\n".toRegex())
                            .dropLastWhile { it.isEmpty() }.toTypedArray()
                    )
                )
            }

            if (logs.isEmpty()) return
            Node.instance?.getRC()?.sendMessage("SERVICE;${service.name()};EVENTS;LOG;$logs", "testcloud-service-events")
        }
    }

    companion object {
        private const val LOG_UPDATE_CYCLE: Long = 100
    }
}