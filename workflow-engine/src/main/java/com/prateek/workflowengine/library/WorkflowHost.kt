package com.prateek.workflowengine.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Displays the active UI task and reports terminal results to the host application. */
@Composable
fun WorkflowHost(
    engine: WorkflowEngine,
    modifier: Modifier = Modifier,
    onFinished: (FlowResult) -> Unit = {}
) {
    val state by engine.state.collectAsState()

    LaunchedEffect(state) {
        if (state is WorkflowEngineState.Finished) {
            onFinished((state as WorkflowEngineState.Finished).result)
        }
    }

    when (val current = state) {
        WorkflowEngineState.Idle -> Unit
        is WorkflowEngineState.Running -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        is WorkflowEngineState.AwaitingUserInput -> current.task.Content(current.context) {
            engine.completeUiTask(current.requestId, it)
        }
        is WorkflowEngineState.Finished -> Text(
            text = when (val result = current.result) {
                FlowResult.Success -> "Workflow complete"
                FlowResult.Skipped -> "Workflow skipped"
                is FlowResult.Failure -> "Workflow failed: ${result.reason}"
            },
            modifier = modifier
        )
    }
}
