package io.github.t45k.bitbucket_coverage

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.coverage.CoverageDataManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.rt.coverage.data.LineData
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import io.github.t45k.bitbucket_coverage.model.FileCoverage
import io.github.t45k.bitbucket_coverage.model.Inner
import io.github.t45k.bitbucket_coverage.model.IntermediateFileCoverage
import io.github.t45k.bitbucket_coverage.model.RequestBody
import kotlin.io.path.Path
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.StringJoiner

class CoveragePostAction : AnAction() {

    private val okHttpClient = OkHttpClient()
    private val objectMapper = jacksonObjectMapper()

    companion object {
        private val logger = Logger.getInstance(this::class.java)
        private const val BITBUCKET_URL = "https://foo"
        private const val USERNAME = "bar"
        private const val PASSWORD = "baz"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return // TODO: show message
        ChangeListManagerImpl.getInstanceImpl(project).executeOnUpdaterThread {
            val gitRepositoryManager = GitRepositoryManager.getInstance(project)
            val coverageDataManager = CoverageDataManager.getInstance(project)
            val currentSuite = coverageDataManager.currentSuitesBundle
            val projectData = currentSuite.suites[0].getCoverageData(coverageDataManager)
                ?: return@executeOnUpdaterThread // TODO: show message
            val classes = projectData.classesCollection
            val scope = GlobalSearchScope.everythingScope(project)
            val javaPsiFacade = JavaPsiFacade.getInstance(project)
            val repositoryFileCoverageMap: Map<GitRepository, List<FileCoverage>> =
                classes.mapNotNull { classDate ->
                    val psiFile =
                        javaPsiFacade.findClass(classDate.name, scope)?.containingFile ?: return@mapNotNull null
                    if (psiFile.fileType.isBinary) {
                        return@mapNotNull null
                    }
                    val lines = classDate.lines.filterNotNull().map { it as LineData }
                    IntermediateFileCoverage(psiFile.virtualFile, lines)
                }.mapNotNull {
                    val repository = gitRepositoryManager.getRepositoryForFile(it.file) ?: return@mapNotNull null
                    repository to it
                }.groupBy({ it.first }, {
                    val repoRoot = Path(it.first.root.path).toRealPath()
                    val filePath = it.second.file.toNioPath().toRealPath()
                    FileCoverage(repoRoot.relativize(filePath).toString(), it.second.lines)
                })
            if (repositoryFileCoverageMap.isEmpty()) {
                return@executeOnUpdaterThread // TODO: show message
            }

            for ((repo, fileCoverages) in repositoryFileCoverageMap) {
                val url = BITBUCKET_URL.toHttpUrl().newBuilder()
                    .addPathSegment("rest")
                    .addPathSegment("code-coverage")
                    .addPathSegment("1.0")
                    .addPathSegment("commits")
                    .addPathSegment(repo.currentRevision!!)
                    .build()
                val body = fileCoverages.filter { it.lines.isNotEmpty() }
                    .map { fileCoverage ->
                        val coveredLines = fileCoverage.lines.groupBy({ it.hits > 0 }, { it.lineNumber })
                        val joiner = StringJoiner(";")
                        coveredLines[true]?.also { joiner.add("C:${it.joinToString(",")}") }
                        coveredLines[false]?.also { joiner.add("U:${it.joinToString(",")}") }
                        Inner(fileCoverage.relativePath, joiner.toString())
                    }.let(::RequestBody)
                val request = Request.Builder()
                    .url(url)
                    .addHeader("authorization", Credentials.basic(USERNAME, PASSWORD))
                    .addHeader("X-Atlassian-Token", "no-check")
                    .post(objectMapper.writeValueAsString(body).toRequestBody("application/json".toMediaType()))
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        logger.error("Communication failure for the following reason.\n${response.body.string()}")
                    }
                }
                // TODO: show dialog
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
