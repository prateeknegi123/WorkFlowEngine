package com.prateek.workflowengine.library

typealias FlowDefinition = WorkflowFlow

class FlowBuilder(
    private val id: String,
    private val registerTask: ((WorkflowTask) -> Unit)? = null
) {
    private val nodes = mutableListOf<WorkflowNode>()

    fun task(
        nodeId: String,
        task: WorkflowTask,
        onSuccess: WorkflowRoute = WorkflowRoute.Continue,
        onSkipped: WorkflowRoute = WorkflowRoute.Continue,
        onFailure: WorkflowRoute = WorkflowRoute.Propagate
    ) = apply {
        val register = requireNotNull(registerTask) {
            "Registering task implementations is only supported inside workflowRegistry."
        }
        register(task)
        nodes += WorkflowNode.Task(nodeId, task.taskId, onSuccess, onSkipped, onFailure)
    }

    fun task(
        nodeId: String,
        taskId: String,
        onSuccess: WorkflowRoute = WorkflowRoute.Continue,
        onSkipped: WorkflowRoute = WorkflowRoute.Continue,
        onFailure: WorkflowRoute = WorkflowRoute.Propagate
    ) = apply {
        nodes += WorkflowNode.Task(nodeId, taskId, onSuccess, onSkipped, onFailure)
    }

    fun subFlow(
        nodeId: String,
        flowId: String,
        onSuccess: WorkflowRoute = WorkflowRoute.Continue,
        onSkipped: WorkflowRoute = WorkflowRoute.Continue,
        onFailure: WorkflowRoute = WorkflowRoute.Propagate
    ) = apply {
        nodes += WorkflowNode.SubFlow(nodeId, flowId, onSuccess, onSkipped, onFailure)
    }

    fun build(): FlowDefinition = WorkflowFlow(id, nodes.toList())
}

fun workflow(
    id: String,
    block: FlowBuilder.() -> Unit
): FlowDefinition = FlowBuilder(id).apply(block).build()
