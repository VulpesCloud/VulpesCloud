package de.vulpescloud.node.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object SysUtils {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    fun getPublicIps(): List<String> {
        val services = listOf(
            "https://api64.ipify.org",    // IPv4 + IPv6
            "https://ipv4.icanhazip.com", // IPv4 only
            "https://ipv6.icanhazip.com"  // IPv6 only
        )

        val results = mutableListOf<String>()

        for (url in services) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val ip = response.body?.string()?.trim()
                        if (!ip.isNullOrBlank() && !results.contains(ip)) {
                            results.add(ip)
                        }
                    }
                }
            } catch (_: Exception) {
                // ignore failed services
            }
        }

        return results
    }
}
