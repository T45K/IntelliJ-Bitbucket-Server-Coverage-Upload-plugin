package io.github.t45k.bitbucket_coverage.model

import com.intellij.rt.coverage.data.LineData

data class FileCoverage(
    val relativePath: String,
    private val lines: List<LineData>,
) {
    fun getCoveredLines(): List<Int> = this.lines.filter { it.isCovered() }.map { it.lineNumber }
    fun getUncoveredLines(): List<Int> = this.lines.filterNot { it.isCovered() }.map { it.lineNumber }
}

private fun LineData.isCovered() = this.hits > 0
