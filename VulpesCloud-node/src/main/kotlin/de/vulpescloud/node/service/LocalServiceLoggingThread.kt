package de.vulpescloud.node.service

import de.vulpescloud.api.network.redis.RedisPubSubChannels
import de.vulpescloud.api.services.ClusterService
import de.vulpescloud.api.services.builder.ServiceEventMessageBuilder
import de.vulpescloud.node.Node
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.function.Supplier


class LocalServiceLoggingThread {

//    val processSupplier: Supplier<Process>? = null
//
//    private val buffer: ByteArray = ByteArray(2048)
//    private val stringBuffer: StringBuffer = StringBuffer()
//
//    val loggingThread = Thread {
//        while (true) {
//            Node.serviceProvider.services()!!.forEach {
//                val ser = it as LocalService
//                if (ser.process != null) {
//                    this.readStream(ser.process!!.inputStream, ser)
//                }
//            }
//            Thread.sleep(1000)
//        }
//    }
//
//    @Throws(IOException::class)
//    fun readStream(stream: InputStream, service: ClusterService) {
//        var len: Int = 0
//        while (stream.available() > 0 && (stream.read(this.buffer, 0, this.buffer.size).also { len = it }) != -1) {
//            this.stringBuffer.append(String(this.buffer, 0, len, StandardCharsets.UTF_8))
//        }
//
//        val content = this.stringBuffer.toString()
//        if (content.contains("\n") || content.contains("\r")) {
//            for (input in content.split("\r")) {
//                for (text in input.split("\n")) {
//                    if (text.trim().isNotEmpty()) {
//                        val msg = ServiceEventMessageBuilder.consoleEventBuilder()
//                            .setService(service)
//                            .setLine(text)
//                            .build()
//                        Node.instance!!.getRC()?.sendMessage(msg, RedisPubSubChannels.VULPESCLOUD_SERVICE_EVENT.name)
//                    }
//                }
//            }
//        }
//
//        this.stringBuffer.setLength(0)
//    }


}