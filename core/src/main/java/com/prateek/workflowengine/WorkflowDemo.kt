package com.prateek.workflowengine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prateek.workflowengine.library.FlowRegistry
import com.prateek.workflowengine.library.FlowResult
import com.prateek.workflowengine.library.WorkflowEngine
import com.prateek.workflowengine.library.WorkflowEngineState
import com.prateek.workflowengine.library.WorkflowHost
import com.prateek.workflowengine.ui.theme.WorkFlowEngineTheme

@Composable
fun WorkflowDemo(registry: FlowRegistry, brandName: String) {
    WorkFlowEngineTheme {
        val engine = remember(registry) { WorkflowEngine(registry) }
        DisposableEffect(engine) {
            onDispose { engine.close() }
        }
        val state by engine.state.collectAsState()

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (state) {
                is WorkflowEngineState.Running, is WorkflowEngineState.AwaitingUserInput ->
                    WorkflowHost(engine = engine, modifier = Modifier.padding(innerPadding))

                else -> WorkflowLandingPage(
                    brandName = brandName,
                    state = state,
                    onStart = { engine.start("welcome") },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun WorkflowLandingPage(
    brandName: String,
    state: WorkflowEngineState,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val finished = state as? WorkflowEngineState.Finished

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$brandName welcome flow", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "This app uses the shared workflow and brand-specific task implementations.",
            style = MaterialTheme.typography.bodyMedium
        )

        if (finished != null) {
            Spacer(Modifier.height(24.dp))
            Card {
                Column(Modifier.padding(16.dp)) {
                    when (val result = finished.result) {
                        FlowResult.Success -> {
                            Text("Flow completed", style = MaterialTheme.typography.titleMedium)
                            Text(finished.context.get("greeting")?.toString() ?: "Welcome!")
                        }

                        FlowResult.Skipped -> Text("Flow was skipped.")
                        is FlowResult.Failure -> Text("Flow failed: ${result.reason}")
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = onStart) {
            Text(if (finished == null) "Start workflow" else "Run again")
        }
    }
}