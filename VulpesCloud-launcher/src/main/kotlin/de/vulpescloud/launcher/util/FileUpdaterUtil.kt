package de.vulpescloud.launcher.util

import java.io.IOException
import java.io.UncheckedIOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandler
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration


object FileUpdaterUtil {

    fun <T> get(uri: URI, body: BodyHandler<T>): HttpResponse<T> {
        val request = HttpRequest.newBuilder()
            .GET()
            .uri(uri)
            .timeout(Duration.ofMinutes(1))
            .header("user-agent", ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/97.0.4692.56 Safari/537.36"))
            .build()

        return HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build()
            .send(request, body)
    }

    fun filePathHandler(filePath: Path): BodyHandler<Path> {
        return HttpResponse.BodyHandlers.ofFile(
            filePath,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
    }

}