# Workflow Engine

An Android workflow runtime with a shared core and brand-specific Android apps.
`:workflow-engine` provides the flow runtime, `:core` is the shared Android library
with the default welcome workflow, and `:a-brand-app` / `:b-brand-app` are example
apps that customize it independently.

## Core concepts

- A `WorkflowFlow` contains ordered task or subflow nodes. Routes can continue, jump
  to another node, finish with an explicit result, or propagate a task/subflow result.
- `FlowResult` reports success, skipped, or failure. A failed task propagates to the
  flow by default; applications can configure alternative routes.
- `WorkflowTask` performs non-UI work. `UiWorkflowTask` renders with Compose and
  returns user input through `TaskResult`.
- A single `WorkflowContext` is shared through nested flows. Task result values are
  merged into it and can be retrieved by later tasks or the host application.
- `WorkflowRegistry` lets an application register new flows/tasks, override a base
  flow/task, or skip a task without changing the library's base definitions.

## Example

The shared core returns a fresh registry for each brand:

```kotlin
val registry = CoreWorkflows.createRegistry()
```

The shared `welcome` flow collects a name and enters the `welcome-greeting` subflow.
That flow enters `welcome-greeting-message`, where the greeting is created. All three
flows share the same context, so the deepest flow can read the name collected by the
parent. B Brand overrides `welcome-greeting` to run the greeting task directly,
bypassing `welcome-greeting-message`.

Each brand app depends on the shared `:core` library and overrides tasks by their
registered task ID. A Brand replaces the Compose UI task; B Brand replaces the
greeting logic:

```kotlin
val aBrandRegistry = CoreWorkflows.createRegistry()
    .overrideTask(ABrandNameEntryTask())
val bBrandRegistry = CoreWorkflows.createRegistry()
    .overrideFlow(workflow("welcome-greeting") {
        task(nodeId = "make-greeting", taskId = DefaultGreetingTask.TASK_ID)
    })
    .overrideTask(BBrandGreetingTask())
```

The same registry can override the flow itself with `overrideFlow(workflow("welcome")
{ ... })`. Every brand must create its own registry instance rather than mutating a
shared singleton. The demo screens use `WorkflowDemo`; production apps can instead
create a `WorkflowEngine`, render `WorkflowHost(engine)`, and close the engine with
their owning lifecycle.
