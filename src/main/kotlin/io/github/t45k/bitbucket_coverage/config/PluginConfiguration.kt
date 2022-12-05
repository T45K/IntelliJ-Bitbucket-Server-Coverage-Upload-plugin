package io.github.t45k.bitbucket_coverage.config

import com.intellij.openapi.options.Configurable
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class PluginConfiguration : Configurable {

    override fun createComponent(): JComponent? {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }

        panel.add(JLabel("foo").apply {
            alignmentX = JComponent.LEFT_ALIGNMENT
            alignmentY = JComponent.TOP_ALIGNMENT
        })

        panel.add(JLabel("bar").apply {
            alignmentX = JComponent.LEFT_ALIGNMENT
            alignmentY = JComponent.TOP_ALIGNMENT
        })

        panel.add(JLabel("baz").apply {
            alignmentX = JComponent.LEFT_ALIGNMENT
            alignmentY = JComponent.TOP_ALIGNMENT
        })

        return panel
    }

    override fun isModified(): Boolean {
        return true
    }

    override fun apply() {
    }

    override fun getDisplayName(): String {
        return "Bitbucket Server Coverage Uploader plugin"
    }
}
