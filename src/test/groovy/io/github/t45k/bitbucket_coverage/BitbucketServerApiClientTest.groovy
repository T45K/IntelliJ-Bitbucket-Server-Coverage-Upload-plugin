package io.github.t45k.bitbucket_coverage

import com.intellij.rt.coverage.data.LineData
import git4idea.repo.GitRepository
import groovy.json.JsonSlurper
import io.github.t45k.bitbucket_coverage.model.FileCoverage
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import spock.lang.Specification

class BitbucketServerApiClientTest extends Specification {

    def jsonSlurper = new JsonSlurper()
    def mockWebServer = new MockWebServer()

    def 'postCoverage returns Success object when API call is successful'() {
        given:
        def url = mockWebServer.url('').toString()
        def bitbucketServerApiClient = new BitbucketServerApiClient(url, 'user', 'pass')

        mockWebServer.enqueue(new MockResponse().setResponseCode(200))

        def gitRepository = Stub(GitRepository) { getCurrentRevision() >> '0123456789' }
        def fileCoverages = [
            new FileCoverage('foo/bar/baz', [
                new LineData(1, '').tap { touch() },
                new LineData(2, '')
            ]),
            new FileCoverage('hoge/fuga/piyo', [new LineData(1, '')]),
            new FileCoverage('no-coverage', [])
        ]

        when:
        def result = bitbucketServerApiClient.postCoverage(gitRepository, fileCoverages)

        then:
        result instanceof CoverageApiResult.Success
        jsonSlurper.parse(mockWebServer.takeRequest().body.readByteArray()) == [
            files: [
                [
                    path    : 'foo/bar/baz',
                    coverage: 'C:1;U:2',
                ],
                [
                    path    : 'hoge/fuga/piyo',
                    coverage: 'U:1',
                ]
            ]
        ]
    }

    def 'postCoverage returns Failure when GitRepository does not return current revision commit sha'() {
        given:
        def bitbucketServerApiClient = new BitbucketServerApiClient('url', 'user', 'pass')

        def gitRepository = Stub(GitRepository) { getCurrentRevision() >> null }

        when:
        def result = bitbucketServerApiClient.postCoverage(gitRepository, _ as List<FileCoverage>)

        then:
        result instanceof CoverageApiResult.Failure
        (result as CoverageApiResult.Failure).cause == 'Failed to find head commit. This module may not be git repository.'
    }

    def 'postCoverage returns Failure when API call is failure'() {
        given:
        def url = mockWebServer.url('').toString()
        mockWebServer.enqueue(new MockResponse().setResponseCode(responseCode).setBody("{\"message\": \"$message\"}"))
        def bitbucketServerApiClient = new BitbucketServerApiClient(url, 'user', 'pass')

        def gitRepository = Stub(GitRepository) { getCurrentRevision() >> '0123456789' }

        when:
        def result = bitbucketServerApiClient.postCoverage(gitRepository, [])

        then:
        result instanceof CoverageApiResult.Failure
        (result as CoverageApiResult.Failure).cause == "{\"message\": \"$message\"}"

        where:
        responseCode | message
        400          | 'Bad Request'
        404          | 'Not Found'
        500          | 'Internal Server Error'
    }

    def 'postCoverage returns Failure when Server does not respond'() {
        given:
        def url = mockWebServer.url('').toString()
        def bitbucketServerApiClient = new BitbucketServerApiClient(url, 'user', 'pass')

        def gitRepository = Stub(GitRepository) { getCurrentRevision() >> '0123456789' }

        when:
        def result = bitbucketServerApiClient.postCoverage(gitRepository, [])

        then:
        result instanceof CoverageApiResult.Failure
        (result as CoverageApiResult.Failure).cause == 'request timed out'
    }

    def cleanup() {
        mockWebServer.shutdown()
    }
}
