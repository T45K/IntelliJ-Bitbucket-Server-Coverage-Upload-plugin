package io.github.t45k.bitbucket_coverage

import com.intellij.coverage.CoverageDataManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiManager
import com.intellij.psi.util.ClassUtil
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import io.github.t45k.bitbucket_coverage.config.PluginSettingsState
import io.github.t45k.bitbucket_coverage.credential.retrievePasswordInSafeWay
import io.github.t45k.bitbucket_coverage.model.FileCoverage
import io.github.t45k.bitbucket_coverage.model.IntermediateFileCoverage
import io.github.t45k.bitbucket_coverage.model.mergeLines

class CoverageUploadAction : AnAction() {

    companion object {
        private val logger = Logger.getInstance(this::class.java)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Managers
        val gitRepositoryManager = GitRepositoryManager.getInstance(project)
        val coverageDataManager = CoverageDataManager.getInstance(project)
        val psiManager = PsiManager.getInstance(project)

        ApplicationManager.getApplication().executeOnPooledThread {
            ApplicationManager.getApplication().runReadAction {

                // Coverage data
                val currentSuite = coverageDataManager.currentSuitesBundle
                val projectData = currentSuite.suites[0].getCoverageData(coverageDataManager)
                    ?: return@runReadAction // TODO: show message
                val classes = projectData.classesCollection

                val repositoryFileCoverageMap: Map<GitRepository, List<FileCoverage>> = classes.asSequence()
                    .mapNotNull { classData ->
                        IntermediateFileCoverage.fromClassData(classData) { ClassUtil.findPsiClass(psiManager, it) }
                    }
                    .groupBy { it.file }
                    .values
                    .map { it.mergeLines() }
                    .mapNotNull { gitRepositoryManager.getRepositoryForFile(it.file)?.to(it) }
                    .groupBy({ it.first }, { it.second.toFileCoverage(it.first) })
                    .takeIf { it.isNotEmpty() }
                    ?: return@runReadAction

                val (bitbucketServerUrl, username) = PluginSettingsState.getInstance()
                val password = retrievePasswordInSafeWay(username)
                val bitbucketServerApiClient = BitbucketServerApiClient(bitbucketServerUrl, username, password)
                for ((repo, fileCoverages) in repositoryFileCoverageMap) {
                    when (val result = bitbucketServerApiClient.postCoverage(repo, fileCoverages)) {
                        CoverageApiResult.Success -> logger.info("Succeeded to post coverage data") // TODO: show successful dialog
                        is CoverageApiResult.Failure -> logger.error("Failed to post coverage data due to the following reason\n${result.cause}")
                    }
                    // TODO: show dialog
                }
            }
        }
    }
}
