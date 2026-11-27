package com.prateek.workflowengine.library

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ContextKey<T : Any>(val name: String)

/** The single shared data store passed to every task and subflow in a run. */
class WorkflowContext internal constructor(initialValues: Map<String, Any?> = emptyMap()) {
    private val values = initialValues.toMutableMap()

    @Synchronized
    fun put(name: String, value: Any?) {
        values[name] = value
    }

    @Synchronized
    fun putAll(newValues: Map<String, Any?>) {
        values.putAll(newValues)
    }

    @Synchronized
    fun get(name: String): Any? = values[name]

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun <T : Any> get(key: ContextKey<T>): T? = values[key.name] as? T

    @Synchronized
    fun snapshot(): Map<String, Any?> = values.toMap()
}


sealed interface WorkflowRoute {
    data object Continue : WorkflowRoute
    data class GoTo(val nodeId: String) : WorkflowRoute
    data class Finish(val result: FlowResult) : WorkflowRoute

    /** Return the task or subflow outcome as the enclosing flow's result. */
    data object Propagate : WorkflowRoute
}

sealed interface WorkflowNode {
    val nodeId: String

    data class Task(
        override val nodeId: String,
        val taskId: String,
        val onSuccess: WorkflowRoute = WorkflowRoute.Continue,
        val onSkipped: WorkflowRoute = WorkflowRoute.Continue,
        val onFailure: WorkflowRoute = WorkflowRoute.Propagate
    ) : WorkflowNode

    data class SubFlow(
        override val nodeId: String,
        val flowId: String,
        val onSuccess: WorkflowRoute = WorkflowRoute.Continue,
        val onSkipped: WorkflowRoute = WorkflowRoute.Continue,
        val onFailure: WorkflowRoute = WorkflowRoute.Propagate
    ) : WorkflowNode
}

data class WorkflowFlow(val flowId: String, val nodes: List<WorkflowNode>) {
    init {
        require(flowId.isNotBlank()) { "Flow id cannot be blank." }
        require(nodes.map { it.nodeId }.toSet().size == nodes.size) {
            "Node ids must be unique within flow '$flowId'."
        }
        require(nodes.all { it.nodeId.isNotBlank() }) { "Node ids cannot be blank." }
    }
}



sealed interface WorkflowEngineState {
    data object Idle : WorkflowEngineState
    data class Running(val flowId: String, val context: WorkflowContext) : WorkflowEngineState
    data class AwaitingUserInput(
        val flowId: String,
        val taskId: String,
        val requestId: Long,
        val task: UiWorkflowTask,
        val context: WorkflowContext
    ) : WorkflowEngineState
    data class Finished(
        val flowId: String,
        val result: FlowResult,
        val context: WorkflowContext
    ) : WorkflowEngineState
}

/** Executes registered flows and exposes state for a Compose host or a custom UI. */
class WorkflowEngine(
    private val registry: FlowRegistry,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) : AutoCloseable {
    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutableState = MutableStateFlow<WorkflowEngineState>(WorkflowEngineState.Idle)
    private var executionJob: Job? = null
    private var pendingUiResult: CompletableDeferred<TaskResult>? = null
    private var nextRequestId = 0L

    val state: StateFlow<WorkflowEngineState> = mutableState.asStateFlow()

    fun start(flowId: String, initialValues: Map<String, Any?> = emptyMap()) {
        synchronized(lock) {
            executionJob?.cancel()
            pendingUiResult?.cancel()
            pendingUiResult = null

            val context = WorkflowContext(initialValues)
            val validationError = registry.validate(flowId)
            if (validationError != null) {
                mutableState.value = WorkflowEngineState.Finished(
                    flowId,
                    FlowResult.Failure(validationError),
                    context
                )
                executionJob = null
                return
            }

            mutableState.value = WorkflowEngineState.Running(flowId, context)
            executionJob = scope.launch {
                val result = executeFlow(flowId, context, emptySet())
                mutableState.value = WorkflowEngineState.Finished(flowId, result, context)
            }
        }
    }

    /** Completes the UI task currently displayed by [WorkflowHost]. */
    fun completeUiTask(requestId: Long, result: TaskResult): Boolean = synchronized(lock) {
        val awaiting = mutableState.value as? WorkflowEngineState.AwaitingUserInput
        if (awaiting?.requestId != requestId) return@synchronized false
        pendingUiResult?.complete(result) ?: false
    }

    override fun close() {
        synchronized(lock) {
            pendingUiResult?.cancel()
            pendingUiResult = null
            executionJob?.cancel()
            executionJob = null
            scope.cancel()
        }
    }

    private suspend fun executeFlow(
        flowId: String,
        context: WorkflowContext,
        ancestors: Set<String>
    ): FlowResult {
        if (flowId in ancestors) return FlowResult.Failure("Subflow cycle detected at '$flowId'.")
        val flow = registry.flow(flowId)
            ?: return FlowResult.Failure("Flow '$flowId' is not registered.")
        val nextAncestors = ancestors + flowId
        val nodeIndices = flow.nodes.mapIndexed { index, node -> node.nodeId to index }.toMap()
        var index = 0

        while (index < flow.nodes.size) {
            val node = flow.nodes[index]
            synchronized(lock) {
                mutableState.value = WorkflowEngineState.Running(flowId, context)
            }

            val outcome: Any = when (node) {
                is WorkflowNode.Task -> {
                    val taskResult = if (registry.isSkipped(node.taskId)) {
                        TaskResult.Skipped()
                    } else {
                        val task = registry.task(node.taskId)
                            ?: return FlowResult.Failure("Task '${node.taskId}' is not registered.")
                        val result = try {
                            if (task is UiWorkflowTask) awaitUiTask(flowId, task, context)
                            else task.execute(context)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            TaskResult.Failure(
                                error.message ?: "Task '${node.taskId}' failed unexpectedly."
                            )
                        }
                        result
                    }
                    when (taskResult) {
                        is TaskResult.Success -> context.putAll(taskResult.values)
                        is TaskResult.Skipped -> context.putAll(taskResult.values)
                        is TaskResult.Failure -> Unit
                    }
                    taskResult
                }
                is WorkflowNode.SubFlow -> executeFlow(node.flowId, context, nextAncestors)
            }

            val route = when (node) {
                is WorkflowNode.Task -> when (val taskResult = outcome as TaskResult) {
                    is TaskResult.Success -> node.onSuccess
                    is TaskResult.Skipped -> node.onSkipped
                    is TaskResult.Failure -> node.onFailure
                }
                is WorkflowNode.SubFlow -> when (outcome as FlowResult) {
                    FlowResult.Success -> node.onSuccess
                    FlowResult.Skipped -> node.onSkipped
                    is FlowResult.Failure -> node.onFailure
                }
            }

            when (route) {
                WorkflowRoute.Continue -> index += 1
                is WorkflowRoute.GoTo -> {
                    index = nodeIndices[route.nodeId]
                        ?: return FlowResult.Failure("Unknown node '${route.nodeId}' in flow '$flowId'.")
                }
                is WorkflowRoute.Finish -> return route.result
                WorkflowRoute.Propagate -> return when (val value = outcome) {
                    is TaskResult.Success -> FlowResult.Success
                    is TaskResult.Skipped -> FlowResult.Skipped
                    is TaskResult.Failure -> FlowResult.Failure(value.reason)
                    is FlowResult -> value
                    else -> FlowResult.Failure("Unsupported workflow outcome.")
                }
            }
        }

        return FlowResult.Success
    }

    private suspend fun awaitUiTask(
        flowId: String,
        task: UiWorkflowTask,
        context: WorkflowContext
    ): TaskResult {
        val deferred = CompletableDeferred<TaskResult>()
        synchronized(lock) {
            pendingUiResult = deferred
            nextRequestId += 1
            mutableState.value = WorkflowEngineState.AwaitingUserInput(
                flowId,
                task.taskId,
                nextRequestId,
                task,
                context
            )
        }
        return try {
            deferred.await()
        } finally {
            synchronized(lock) {
                if (pendingUiResult === deferred) pendingUiResult = null
            }
        }
    }
}
