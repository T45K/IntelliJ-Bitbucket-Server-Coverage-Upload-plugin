package io.github.t45k.bitbucket_coverage

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import git4idea.repo.GitRepository
import io.github.t45k.bitbucket_coverage.model.FileCoverage
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.HttpTimeoutException
import java.time.Duration
import java.util.Base64
import java.util.StringJoiner

class BitbucketServerApiClient(
    private val bitbucketServerUrl: String,
    private val username: String,
    private val password: String,
) {

    private val httpClient = HttpClient.newHttpClient()
    private val objectMapper = jacksonObjectMapper()

    fun postCoverage(repo: GitRepository, fileCoverages: List<FileCoverage>): CoverageApiResult {
        val head: String = repo.currentRevision
            ?: return CoverageApiResult.Failure("Failed to find head commit. This module may not be git repository.")
        val uri = URI.create("$bitbucketServerUrl/rest/code-coverage/1.0/commits/$head")
        val requestBody: CoverageApiRequestBody = fileCoverages.filter { it.lines.isNotEmpty() }
            .map { fileCoverage ->
                val coveredLines: List<Int> = fileCoverage.getCoveredLines()
                val uncoveredLines: List<Int> = fileCoverage.getUncoveredLines()
                val joiner = StringJoiner(";")
                coveredLines.takeIf { it.isNotEmpty() }?.also { joiner.add("C:${it.joinToString(",")}") }
                uncoveredLines.takeIf { it.isNotEmpty() }?.also { joiner.add("U:${it.joinToString(",")}") }
                FileRequestBody(fileCoverage.relativePath, joiner.toString())
            }.let(::CoverageApiRequestBody)
        val request = HttpRequest.newBuilder()
            .uri(uri)
            .header(
                "Authorization",
                "Basic ${Base64.getEncoder().encodeToString("${username}:${password}".toByteArray())}"
            ).header("X-Atlassian-Token", "no-check")
            .header("content-type", "application/json; charset=UTF-8")
            .timeout(Duration.ofSeconds(5))
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
            .build()
        return try {
            val response = httpClient.send(request, BodyHandlers.ofString())
            if (response.statusCode() < 400) CoverageApiResult.Success else CoverageApiResult.Failure(response.body())
        } catch (e: HttpTimeoutException) {
            CoverageApiResult.Failure(e.message!!)
        }
    }
}

private data class CoverageApiRequestBody(val files: List<FileRequestBody>)

private data class FileRequestBody(val path: String, val coverage: String)

sealed interface CoverageApiResult {
    object Success : CoverageApiResult
    data class Failure(val cause: String) : CoverageApiResult
}


