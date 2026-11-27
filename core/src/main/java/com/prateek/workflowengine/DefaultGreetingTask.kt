package com.prateek.workflowengine

import com.prateek.workflowengine.library.TaskResult
import com.prateek.workflowengine.library.WorkflowContext
import com.prateek.workflowengine.library.WorkflowTask

open class DefaultGreetingTask : WorkflowTask {
    override val taskId = TASK_ID

    override suspend fun execute(context: WorkflowContext): TaskResult =
        TaskResult.Success(
            mapOf("greeting" to "Welcome, ${context.get("name") ?: "friend"}!")
        )

    companion object {
        const val TASK_ID = "make-greeting"
    }
}