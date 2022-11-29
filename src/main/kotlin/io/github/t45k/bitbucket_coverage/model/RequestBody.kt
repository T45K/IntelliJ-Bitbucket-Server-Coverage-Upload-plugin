package io.github.t45k.bitbucket_coverage.model

data class RequestBody(val files: List<Inner>)

data class Inner(val path: String, val coverage: String)
