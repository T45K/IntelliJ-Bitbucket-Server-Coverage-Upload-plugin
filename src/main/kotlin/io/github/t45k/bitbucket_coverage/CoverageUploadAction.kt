package io.github.t45k.bitbucket_coverage

import com.intellij.coverage.CoverageDataManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
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

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Managers
        val gitRepositoryManager = GitRepositoryManager.getInstance(project)
        val coverageDataManager = CoverageDataManager.getInstance(project)
        val psiManager = PsiManager.getInstance(project)
        ApplicationManager.getApplication().executeOnPooledThread {
            ApplicationManager.getApplication().runReadAction {

                // Coverage data
                val projectData = coverageDataManager.currentSuitesBundle
                    ?.let { it.suites[0].getCoverageData(coverageDataManager) }
                    ?: run {
                        notifyWarning("Please run tests with coverage before uploading", project)
                        return@runReadAction
                    }

                val repositoryFileCoverageMap: Map<GitRepository, List<FileCoverage>> = projectData.classes.values
                    .asSequence()
                    .mapNotNull { classData ->
                        IntermediateFileCoverage.fromClassData(classData) { ClassUtil.findPsiClass(psiManager, it) }
                    }
                    .groupBy { it.file }
                    .values
                    .map { it.mergeLines() }
                    .mapNotNull { gitRepositoryManager.getRepositoryForFile(it.file)?.to(it) }
                    .groupBy({ it.first }, { it.second.toFileCoverage(it.first) })
                    .takeIf { it.isNotEmpty() }
                    ?: run {
                        notifyWarning("Coverage data or Git repository was not found", project)
                        return@runReadAction
                    }

                val (bitbucketServerUrl, username) = PluginSettingsState.getInstance()
                val password = retrievePasswordInSafeWay(username)
                val bitbucketServerApiClient = BitbucketServerApiClient(bitbucketServerUrl, username, password)
                for ((repo, fileCoverages) in repositoryFileCoverageMap) {
                    when (val result = bitbucketServerApiClient.postCoverage(repo, fileCoverages)) {
                        CoverageApiResult.Success -> notifyInfo("Succeeded to upload coverage", project)
                        is CoverageApiResult.Failure -> notifyError(
                            "Failed to upload coverage due to the following reason\n${result.cause}", project
                        )
                    }
                }
            }
        }
    }

    private fun notifyInfo(message: String, project: Project) {
        Notifications.Bus.notify(
            Notification(
                "Coverage Uploader message",
                message,
                NotificationType.INFORMATION
            ), project
        )
    }

    private fun notifyWarning(message: String, project: Project) {
        Notifications.Bus.notify(
            Notification(
                "Coverage Uploader message",
                message,
                NotificationType.WARNING
            ), project
        )
    }

    private fun notifyError(message: String, project: Project) {
        Notifications.Bus.notify(
            Notification(
                "Coverage Uploader message",
                message,
                NotificationType.ERROR
            ), project
        )
    }
}
