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
import com.prateek.workflowengine.library.WorkflowRoute
import com.prateek.workflowengine.library.workflowRegistry
import com.prateek.workflowengine.ui.theme.WorkFlowEngineTheme

/** Provides a new registry so each brand can safely apply its own overrides. */
object CoreWorkFlowFactory {
    fun createRegistry(): FlowRegistry = workflowRegistry {
        flow("welcome") {
            task(
                nodeId = "enter-name",
                task = NameEntryTask(),
                onSuccess = WorkflowRoute.GoTo("greeting")
            )
            subFlow(nodeId = "greeting", flowId = "welcome-greeting")
        }
        flow("welcome-greeting") {
            subFlow(nodeId = "greeting-message", flowId = "welcome-greeting-message")
        }
        flow("welcome-greeting-message") {
            task(nodeId = "make-greeting", task = DefaultGreetingTask())
        }
    }
}





