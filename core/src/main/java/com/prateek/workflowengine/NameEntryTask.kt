package com.prateek.workflowengine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prateek.workflowengine.library.TaskResult
import com.prateek.workflowengine.library.UiWorkflowTask
import com.prateek.workflowengine.library.WorkflowContext

class NameEntryTask : UiWorkflowTask {
    override val taskId = TASK_ID

    @Composable
    override fun Content(context: WorkflowContext, onResult: (TaskResult) -> Unit) {
        var name by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("What should we call you?", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your name") },
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onResult(TaskResult.Success(mapOf("name" to name.trim()))) },
                enabled = name.isNotBlank()
            ) {
                Text("Continue")
            }
            TextButton(onClick = { onResult(TaskResult.Skipped()) }) {
                Text("Skip")
            }
        }
    }

    companion object {
        const val TASK_ID = "enter-name"
    }
}