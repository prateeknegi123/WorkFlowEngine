package com.prateek.workflowengine.library

class WorkflowRegistryBuilder {
    private val registry = FlowRegistry()

    fun task(task: WorkflowTask) = apply {
        registry.registerTask(task)
    }

    fun flow(
        id: String,
        block: FlowBuilder.() -> Unit
    ) = apply {
        val flow = FlowBuilder(id, registry::registerTask).apply(block).build()
        registry.registerFlow(flow)
    }

    fun flow(flow: WorkflowFlow) = apply {
        registry.registerFlow(flow)
    }

    fun build(): FlowRegistry = registry
}

fun workflowRegistry(
    block: WorkflowRegistryBuilder.() -> Unit
): FlowRegistry = WorkflowRegistryBuilder().apply(block).build()
