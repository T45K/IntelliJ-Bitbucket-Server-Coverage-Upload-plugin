package io.github.t45k.bitbucket_coverage.model

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.rt.coverage.data.ClassData
import com.intellij.rt.coverage.data.LineData
import git4idea.repo.GitRepository

data class IntermediateFileCoverage(
    val file: VirtualFile,
    val lines: List<LineData>
) {

    companion object {
        fun fromClassData(classData: ClassData, findPsiClassAction: (String) -> PsiClass?): IntermediateFileCoverage? {
            val virtualFile = findPsiClassAction(classData.name)
                ?.containingFile
                ?.takeUnless { it.fileType.isBinary }
                ?.virtualFile
                ?: return null
            val lines = classData.lines.filterNotNull().map { it as LineData }
            return IntermediateFileCoverage(virtualFile, lines)
        }
    }

    fun toFileCoverage(repo: GitRepository): FileCoverage {
        val repoRootPath = repo.root.toNioPath().toRealPath()
        val filePath = file.toNioPath().toRealPath()
        return FileCoverage(repoRootPath.relativize(filePath).toString(), lines)
    }

}

fun List<IntermediateFileCoverage>.mergeLines(): IntermediateFileCoverage {
    val files = this.map { it.file }.distinct()
    assert(files.size == 1)

    return IntermediateFileCoverage(files[0], this.map { it.lines }.flatten())
}
