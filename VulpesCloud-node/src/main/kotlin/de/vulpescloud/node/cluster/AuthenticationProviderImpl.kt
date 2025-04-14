package de.vulpescloud.node.cluster

import de.vulpescloud.api.cluster.AuthenticationProvider
import de.vulpescloud.node.utils.StringUtils
import kotlin.io.path.Path

class AuthenticationProviderImpl : AuthenticationProvider {
    private var token = ""

    override fun getAuthenticationToken(): String {
        return token
    }

    fun initializeToken() {
        val authFile = Path("node/auth.secret")
        Path("node/").toFile().mkdirs()
        if (authFile.toFile().exists()) {
            token = authFile.toFile().readText()
        } else {
            authFile.toFile().writeText(StringUtils.generateRandomString(16))
            token = authFile.toFile().readText()
        }
    }

}
