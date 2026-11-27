package com.prateek.workflowengine.library

import androidx.compose.runtime.Composable

/** UI tasks complete asynchronously through [Content]'s result callback. */
interface UiWorkflowTask : WorkflowTask {
    @Composable
    fun Content(context: WorkflowContext, onResult: (TaskResult) -> Unit)

    override suspend fun execute(context: WorkflowContext): TaskResult =
        TaskResult.Failure("UI task '$taskId' must be run by WorkflowEngine.")
}