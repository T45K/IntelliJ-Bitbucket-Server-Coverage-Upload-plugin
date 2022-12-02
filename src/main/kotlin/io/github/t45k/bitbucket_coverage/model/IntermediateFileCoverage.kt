package io.github.t45k.bitbucket_coverage.model

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.rt.coverage.data.LineData

data class IntermediateFileCoverage(
    val file: VirtualFile,
    val lines: List<LineData>
)
