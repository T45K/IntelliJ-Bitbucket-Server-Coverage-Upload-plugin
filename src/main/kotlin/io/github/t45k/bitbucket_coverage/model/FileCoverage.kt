package io.github.t45k.bitbucket_coverage.model

import com.intellij.rt.coverage.data.LineData

data class FileCoverage(
    val relativePath: String,
    val lines: List<LineData>,
)
