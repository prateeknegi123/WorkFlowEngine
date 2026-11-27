package com.prateek.workflowengine.brand.a

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prateek.workflowengine.CoreWorkFlowFactory
import com.prateek.workflowengine.WorkflowDemo
import com.prateek.workflowengine.NameEntryTask
import com.prateek.workflowengine.library.FlowRegistry
import com.prateek.workflowengine.library.TaskResult
import com.prateek.workflowengine.library.UiWorkflowTask
import com.prateek.workflowengine.library.WorkflowContext

class ABrandActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val registry = remember { createABrandRegistry() }
            WorkflowDemo(registry = registry, brandName = "A Brand")
        }
    }
}

private fun createABrandRegistry(): FlowRegistry =
    CoreWorkFlowFactory.createRegistry().overrideTask(ABrandNameEntryTask())

private class ABrandNameEntryTask : UiWorkflowTask {
    override val taskId = NameEntryTask.TASK_ID

    @androidx.compose.runtime.Composable
    override fun Content(context: WorkflowContext, onResult: (TaskResult) -> Unit) {
        var name by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Welcome to A Brand",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onResult(TaskResult.Success(mapOf("name" to name.trim()))) },
                enabled = name.isNotBlank()
            ) {
                Text("Continue")
            }
        }
    }
}
