package io.github.t45k.bitbucket_coverage.config

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel

class PluginSettingsConfiguration : BoundConfigurable("Coverage Upload", null) {

    private lateinit var component: PluginSettingsComponent

    override fun createPanel(): DialogPanel {
        component = PluginSettingsComponent()
        return component.panel
    }

    override fun apply() {
        super.apply()

        val state = PluginSettingsState.getInstance()
        state.bitbucketServerUrl = component.bitbucketServerUrlText
        state.username = component.usernameText
    }

    override fun reset() {
        val (bitbucketServerUrl, username) = PluginSettingsState.getInstance()
        component.bitbucketServerUrlText = bitbucketServerUrl
        component.usernameText = username

        super.reset()
    }
}
