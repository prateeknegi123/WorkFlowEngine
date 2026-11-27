package com.prateek.workflowengine.library

/**
 * Holds the base workflow configuration. Brand applications can register new items,
 * replace an existing flow/task, or mark a task as skipped before starting the engine.
 */
class FlowRegistry {
    private val flows = linkedMapOf<String, WorkflowFlow>()
    private val tasks = linkedMapOf<String, WorkflowTask>()
    private val skippedTasks = mutableSetOf<String>()

    fun registerFlow(flow: WorkflowFlow) = apply {
        require(flows.putIfAbsent(flow.flowId, flow) == null) {
            "Flow '${flow.flowId}' is already registered; use overrideFlow to replace it."
        }
    }

    fun overrideFlow(flow: WorkflowFlow) = apply {
        require(flows.containsKey(flow.flowId)) { "Cannot override unknown flow '${flow.flowId}'." }
        flows[flow.flowId] = flow
    }

    fun registerTask(task: WorkflowTask) = apply {
        require(task.taskId.isNotBlank()) { "Task id cannot be blank." }
        require(tasks.putIfAbsent(task.taskId, task) == null) {
            "Task '${task.taskId}' is already registered; use overrideTask to replace it."
        }
        skippedTasks.remove(task.taskId)
    }

    fun overrideTask(task: WorkflowTask) = apply {
        require(task.taskId.isNotBlank()) { "Task id cannot be blank." }
        require(tasks.containsKey(task.taskId)) { "Cannot override unknown task '${task.taskId}'." }
        tasks[task.taskId] = task
        skippedTasks.remove(task.taskId)
    }

    fun skipTask(taskId: String) = apply {
        require(tasks.containsKey(taskId)) { "Cannot skip unknown task '$taskId'." }
        skippedTasks.add(taskId)
    }

    internal fun task(taskId: String): WorkflowTask? = tasks[taskId]
    internal fun isSkipped(taskId: String): Boolean = taskId in skippedTasks
    internal fun flow(flowId: String): WorkflowFlow? = flows[flowId]

    internal fun validate(startFlowId: String): String? {
        val visited = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()

        fun visit(flowId: String): String? {
            if (flowId in visiting) return "Subflow cycle detected at '$flowId'."
            if (flowId in visited) return null
            val flow = flows[flowId] ?: return "Flow '$flowId' is not registered."
            visiting.add(flowId)

            val nodeIds = flow.nodes.mapTo(mutableSetOf()) { it.nodeId }
            for (node in flow.nodes) {
                val routes = when (node) {
                    is WorkflowNode.Task -> {
                        if (!isSkipped(node.taskId) && task(node.taskId) == null) {
                            return "Task '${node.taskId}' in flow '$flowId' is not registered."
                        }
                        listOf(node.onSuccess, node.onSkipped, node.onFailure)
                    }
                    is WorkflowNode.SubFlow -> {
                        val error = visit(node.flowId)
                        if (error != null) return error
                        listOf(node.onSuccess, node.onSkipped, node.onFailure)
                    }
                }
                for (route in routes) {
                    if (route is WorkflowRoute.GoTo && route.nodeId !in nodeIds) {
                        return "Route in flow '$flowId' targets unknown node '${route.nodeId}'."
                    }
                }
            }
            visiting.remove(flowId)
            visited.add(flowId)
            return null
        }

        return visit(startFlowId)
    }
}