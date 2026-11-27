package com.prateek.workflowengine.brand.b

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.prateek.workflowengine.CoreWorkFlowFactory
import com.prateek.workflowengine.DefaultGreetingTask
import com.prateek.workflowengine.WorkflowDemo
import com.prateek.workflowengine.library.FlowRegistry
import com.prateek.workflowengine.library.TaskResult
import com.prateek.workflowengine.library.WorkflowContext
import com.prateek.workflowengine.library.workflow

class BBrandActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val registry = remember { createBBrandRegistry() }
            WorkflowDemo(registry = registry, brandName = "B Brand")
        }
    }
}

private fun createBBrandRegistry(): FlowRegistry =
    CoreWorkFlowFactory.createRegistry()
        .overrideFlow(workflow("welcome-greeting") {
            task(nodeId = "make-greeting", taskId = DefaultGreetingTask.TASK_ID)
        })
        .overrideTask(BBrandGreetingTask())

private class BBrandGreetingTask : DefaultGreetingTask() {

    override suspend fun execute(context: WorkflowContext): TaskResult =
        TaskResult.Success(
            mapOf("greeting" to "B Brand says hello, ${context.get("name") ?: "friend"}!")
        )
}
