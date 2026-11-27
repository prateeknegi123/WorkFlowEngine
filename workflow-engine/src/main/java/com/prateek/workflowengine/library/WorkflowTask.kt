package com.prateek.workflowengine.library

interface WorkflowTask {
    val taskId: String
    suspend fun execute(context: WorkflowContext): TaskResult
}