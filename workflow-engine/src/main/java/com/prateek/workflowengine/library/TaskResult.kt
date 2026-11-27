package com.prateek.workflowengine.library

sealed interface TaskResult {
    data class Success(val values: Map<String, Any?> = emptyMap()) : TaskResult
    data class Skipped(val values: Map<String, Any?> = emptyMap()) : TaskResult
    data class Failure(val reason: String) : TaskResult
}