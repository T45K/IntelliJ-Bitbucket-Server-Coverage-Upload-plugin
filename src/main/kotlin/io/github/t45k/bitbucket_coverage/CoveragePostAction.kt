package io.github.t45k.bitbucket_coverage

import com.intellij.coverage.CoverageDataManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiManager
import com.intellij.psi.util.ClassUtil
import com.intellij.rt.coverage.data.LineData
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import io.github.t45k.bitbucket_coverage.model.FileCoverage
import io.github.t45k.bitbucket_coverage.model.IntermediateFileCoverage
import kotlin.io.path.Path

class CoveragePostAction : AnAction() {

    companion object {
        private val logger = Logger.getInstance(this::class.java)
        private const val BITBUCKET_URL = "https://foo"
        private const val USERNAME = "bar"
        private const val PASSWORD = "baz"
    }

    override fun actionPerformed(e: AnActionEvent) {
        ApplicationManager.getApplication().executeOnPooledThread {
            ApplicationManager.getApplication().runReadAction {
                val project = e.project ?: return@runReadAction // TODO: show message
                val gitRepositoryManager = GitRepositoryManager.getInstance(project)
                val coverageDataManager = CoverageDataManager.getInstance(project)
                val currentSuite = coverageDataManager.currentSuitesBundle
                val projectData =
                    currentSuite.suites[0].getCoverageData(coverageDataManager)
                        ?: return@runReadAction // TODO: show message
                val classes = projectData.classesCollection
                val psiManager = PsiManager.getInstance(project)
                val repositoryFileCoverageMap: Map<GitRepository, List<FileCoverage>> = classes.asSequence()
                    .mapNotNull { classData ->
                        val psiFile = ClassUtil.findPsiClass(psiManager, classData.name)?.containingFile
                            ?: return@mapNotNull null
                        if (psiFile.fileType.isBinary) {
                            return@mapNotNull null
                        }
                        val lines = classData.lines.filterNotNull().map { it as LineData }
                        IntermediateFileCoverage(psiFile.virtualFile, lines)
                    }
                    .groupBy { it.file }
                    .map { (virtualFile, list) ->
                        IntermediateFileCoverage(virtualFile, list.map { it.lines }.flatten())
                    }
                    .mapNotNull {
                        val repository =
                            gitRepositoryManager.getRepositoryForFile(it.file) ?: return@mapNotNull null
                        repository to it
                    }.groupBy({ it.first }, {
                        val repoRoot = Path(it.first.root.path).toRealPath()
                        val filePath = it.second.file.toNioPath().toRealPath()
                        FileCoverage(repoRoot.relativize(filePath).toString(), it.second.lines)
                    })
                if (repositoryFileCoverageMap.isEmpty()) {
                    return@runReadAction // TODO: show message
                }

                val bitbucketServerApiClient = BitbucketServerApiClient(BITBUCKET_URL, USERNAME, PASSWORD)
                for ((repo, fileCoverages) in repositoryFileCoverageMap) {
                    when (val result = bitbucketServerApiClient.postCoverage(repo, fileCoverages)) {
                        CoverageApiResult.Success -> logger.info("Succeeded to post coverage data") // TODO: show successful dialog
                        is CoverageApiResult.Failure -> logger.error("Failed to post coverage data due to the following reason.\n${result.cause}")
                    }
                    // TODO: show dialog
                }
            }
        }
    }

//    override fun update(e: AnActionEvent) {
//        val dataContext = e.dataContext
//        val presentation = e.presentation
//        presentation.isEnabled = false
//        presentation.isVisible = false
//        val project = e.project ?: return
//        val currentSuite = CoverageDataManager.getInstance(project).currentSuitesBundle ?: return
//        val coverageEngine = currentSuite.coverageEngine
//        if (coverageEngine.isReportGenerationAvailable(project, dataContext, currentSuite)) {
//            presentation.isEnabled = true
//            presentation.isVisible = true
//        }
//    }
}
