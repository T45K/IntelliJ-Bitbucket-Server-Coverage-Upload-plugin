package io.github.t45k.bitbucket_coverage.credential

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe

fun createCredentialAttributes(key: String) =
    // The first arg of generateServiceName must not be committed for security reason
    CredentialAttributes(generateServiceName("", key))

fun storePasswordInSafeWay(username: String, password: String) {
    val credentialAttributes = createCredentialAttributes(username)
    PasswordSafe.instance.setPassword(credentialAttributes, password)
}

fun retrievePasswordInSafeWay(username: String): String {
    val credentialAttributes = createCredentialAttributes(username)
    return PasswordSafe.instance.getPassword(credentialAttributes) ?: ""
}