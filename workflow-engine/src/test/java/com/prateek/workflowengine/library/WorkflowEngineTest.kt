package com.prateek.workflowengine.library

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowEngineTest {
    @Test
    fun registryDslRegistersTasksAndFlows() {
        val registry = workflowRegistry {
            flow("flow") {
                task("work-node", task("work") { TaskResult.Success() })
            }
        }

        assertEquals("flow", registry.flow("flow")?.flowId)
        assertTrue(registry.task("work") != null)
    }

    @Test
    fun flowDslBuildsOrderedTaskNodesAndRoutes() {
        val flow = workflow("welcome") {
            task(
                nodeId = "enter-name",
                taskId = "enter-name",
                onSuccess = WorkflowRoute.GoTo("greet"),
                onSkipped = WorkflowRoute.GoTo("cancel")
            )
            task(nodeId = "greet", taskId = "make-greeting")
            subFlow(nodeId = "child", flowId = "child-flow")
        }

        assertEquals("welcome", flow.flowId)
        assertEquals(
            listOf(
                WorkflowNode.Task(
                    "enter-name",
                    "enter-name",
                    WorkflowRoute.GoTo("greet"),
                    WorkflowRoute.GoTo("cancel")
                ),
                WorkflowNode.Task("greet", "make-greeting"),
                WorkflowNode.SubFlow("child", "child-flow")
            ),
            flow.nodes
        )
    }

    @Test
    fun uiTaskInputIsAddedToTheSharedContext() = runBlocking {
        val registry = FlowRegistry()
            .registerTask(object : UiWorkflowTask {
                override val taskId = "input"

                @Composable
                override fun Content(
                    context: WorkflowContext,
                    onResult: (TaskResult) -> Unit
                ) = Unit
            })
            .registerTask(task("consume") {
                TaskResult.Success(mapOf("received" to it.get("answer")))
            })
            .registerFlow(
                WorkflowFlow(
                    "flow",
                    listOf(
                        WorkflowNode.Task("input-node", "input"),
                        WorkflowNode.Task("consume-node", "consume")
                    )
                )
            )
        val engine = WorkflowEngine(registry)

        try {
            engine.start("flow")
            val awaiting = withTimeout(5_000) {
                engine.state.first { it is WorkflowEngineState.AwaitingUserInput }
            } as WorkflowEngineState.AwaitingUserInput

            assertTrue(engine.completeUiTask(awaiting.requestId, TaskResult.Success(mapOf("answer" to 42))))
            assertTrue(!engine.completeUiTask(awaiting.requestId, TaskResult.Success()))

            val finished = awaitFinished(engine)
            assertEquals(42, finished.context.get("received"))
        } finally {
            engine.close()
        }
    }

    @Test
    fun nestedSubflowsShareContextAndReturnTheirResultToParent() = runBlocking {
        val registry = FlowRegistry()
            .registerTask(task("set-name") { TaskResult.Success(mapOf("name" to "Ada")) })
            .registerTask(task("set-greeting") {
                TaskResult.Success(mapOf("greeting" to "Hello ${it.get("name")}"))
            })
            .registerFlow(
                WorkflowFlow(
                    "child",
                    listOf(WorkflowNode.SubFlow("grandchild-flow", "grandchild"))
                )
            )
            .registerFlow(
                WorkflowFlow(
                    "grandchild",
                    listOf(WorkflowNode.Task("greeting", "set-greeting"))
                )
            )
            .registerFlow(
                WorkflowFlow(
                    "parent",
                    listOf(
                        WorkflowNode.Task("name", "set-name"),
                        WorkflowNode.SubFlow("child-flow", "child")
                    )
                )
            )
        val engine = WorkflowEngine(registry)

        try {
            engine.start("parent")
            val finished = awaitFinished(engine)

            assertEquals(FlowResult.Success, finished.result)
            assertEquals("Ada", finished.context.get("name"))
            assertEquals("Hello Ada", finished.context.get("greeting"))
        } finally {
            engine.close()
        }
    }

    @Test
    fun taskOverrideAndConfiguredSkipAreApplied() = runBlocking {
        val registry = FlowRegistry()
            .registerTask(task("first") { TaskResult.Success(mapOf("brand" to "base")) })
            .registerTask(task("second") { error("Skipped task must not execute") })
            .registerFlow(
                WorkflowFlow(
                    "flow",
                    listOf(
                        WorkflowNode.Task("first-node", "first"),
                        WorkflowNode.Task(
                            "second-node",
                            "second",
                            onSkipped = WorkflowRoute.Finish(FlowResult.Skipped)
                        )
                    )
                )
            )
            .overrideTask(task("first") { TaskResult.Success(mapOf("brand" to "custom")) })
            .skipTask("second")
        val engine = WorkflowEngine(registry)

        try {
            engine.start("flow")
            val finished = awaitFinished(engine)

            assertEquals(FlowResult.Skipped, finished.result)
            assertEquals("custom", finished.context.get("brand"))
        } finally {
            engine.close()
        }
    }

    @Test
    fun taskFailurePropagatesToFlowResult() = runBlocking {
        val registry = FlowRegistry()
            .registerTask(task("broken") { TaskResult.Failure("No connection") })
            .registerFlow(
                WorkflowFlow("flow", listOf(WorkflowNode.Task("work", "broken")))
            )
        val engine = WorkflowEngine(registry)

        try {
            engine.start("flow")
            val finished = awaitFinished(engine)

            assertTrue(finished.result is FlowResult.Failure)
            assertEquals("No connection", (finished.result as FlowResult.Failure).reason)
        } finally {
            engine.close()
        }
    }

    private suspend fun awaitFinished(engine: WorkflowEngine): WorkflowEngineState.Finished =
        withTimeout(5_000) {
            engine.state.first { it is WorkflowEngineState.Finished } as WorkflowEngineState.Finished
        }

    private fun task(id: String, action: suspend (WorkflowContext) -> TaskResult) =
        object : WorkflowTask {
            override val taskId = id
            override suspend fun execute(context: WorkflowContext) = action(context)
        }
}
