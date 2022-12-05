package io.github.t45k.bitbucket_coverage.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "io.github.t45k.bitbucket_coverage.config.PluginSettingsState",
    storages = [Storage("bitbucket-coverage.xml")]
)
data class PluginSettingsState(
    var bitbucketServerUrl: String = "",
    var username: String = "",
) : PersistentStateComponent<PluginSettingsState> {

    companion object {
        fun getInstance(): PluginSettingsState =
            ApplicationManager.getApplication().getService(PluginSettingsState::class.java)
    }

    override fun getState(): PluginSettingsState = this

    override fun loadState(state: PluginSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
