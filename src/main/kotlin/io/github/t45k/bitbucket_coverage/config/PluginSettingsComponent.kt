package io.github.t45k.bitbucket_coverage.config

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.COLUMNS_SHORT
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import io.github.t45k.bitbucket_coverage.credential.retrievePasswordInSafeWay
import io.github.t45k.bitbucket_coverage.credential.storePasswordInSafeWay
import javax.swing.JPasswordField

class PluginSettingsComponent {

    val panel: DialogPanel

    var bitbucketServerUrlText: String = ""
    var usernameText: String = ""

    init {
        panel = panel {
            row("Bitbucket Server URL: ") { textField().bindText(::bitbucketServerUrlText) }
            row("Username: ") { textField().bindText(::usernameText) }
            row("Password: ") {
                cell(JPasswordField()).columns(COLUMNS_SHORT).bindText(
                    { retrievePasswordInSafeWay(usernameText) },
                    { storePasswordInSafeWay(usernameText, it) }
                ).comment("You can use personal access token instead of password.")
            }
        }
    }
}
