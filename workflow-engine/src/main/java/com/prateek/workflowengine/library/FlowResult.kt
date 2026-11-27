package com.prateek.workflowengine.library

sealed interface FlowResult {
    data object Success : FlowResult
    data object Skipped : FlowResult
    data class Failure(val reason: String) : FlowResult
}