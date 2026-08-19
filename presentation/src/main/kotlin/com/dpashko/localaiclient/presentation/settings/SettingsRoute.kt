package com.dpashko.localaiclient.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dpashko.localaiclient.presentation.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                SettingsEvent.Applied -> onBack()
            }
        }
    }

    SettingsScreen(
        state = state,
        onBack = onBack,
        onTimeoutMinutesChanged = viewModel::onTimeoutMinutesChanged,
        onAppLockEnabledChanged = viewModel::onAppLockEnabledChanged,
        onResetClick = viewModel::resetDraftToDefault,
        onApplyClick = viewModel::apply,
        onDeleteAllSessionDataClick = viewModel::deleteAllSessionData,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onTimeoutMinutesChanged: (String) -> Unit,
    onAppLockEnabledChanged: (Boolean) -> Unit,
    onResetClick: () -> Unit,
    onApplyClick: () -> Unit,
    onDeleteAllSessionDataClick: () -> Unit,
) {
    var isDeleteAllDialogVisible by remember { mutableStateOf(false) }
    var isResetDialogVisible by remember { mutableStateOf(false) }
    val isBusy = state.isApplying || state.isDeletingSessionData

    if (isResetDialogVisible) {
        AlertDialog(
            onDismissRequest = { isResetDialogVisible = false },
            title = { Text("Reset settings?") },
            text = { Text("This resets the draft settings to default values.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        isResetDialogVisible = false
                        onResetClick()
                    },
                    enabled = !isBusy,
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { isResetDialogVisible = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (isDeleteAllDialogVisible) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isDeletingSessionData) {
                    isDeleteAllDialogVisible = false
                }
            },
            title = { Text("Delete all conversations?") },
            text = { Text("This permanently deletes all conversations and messages on this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleteAllDialogVisible = false
                        onDeleteAllSessionDataClick()
                    },
                    enabled = !state.isDeletingSessionData,
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { isDeleteAllDialogVisible = false },
                    enabled = !state.isDeletingSessionData,
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { isResetDialogVisible = true },
                        enabled = !isBusy,
                    ) {
                        Text("Reset")
                    }
                },
            )
        },
        bottomBar = {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp),
                onClick = onApplyClick,
                enabled = !isBusy,
            ) {
                if (state.isApplying) {
                    CircularProgressIndicator()
                } else {
                    Text("Apply")
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.timeoutMinutesText,
                onValueChange = onTimeoutMinutesChanged,
                enabled = !isBusy,
                label = { Text("Generation timeout") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_timer),
                        contentDescription = null,
                    )
                },
                suffix = { Text("minutes") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = state.errorMessage != null,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_key),
                        contentDescription = null,
                    )
                    Text(
                        text = "Require device unlock",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Switch(
                    checked = state.appLockEnabled,
                    onCheckedChange = onAppLockEnabledChanged,
                    enabled = !isBusy,
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { isDeleteAllDialogVisible = true },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                if (state.isDeletingSessionData) {
                    CircularProgressIndicator()
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = null,
                        )
                        Text("Delete all conversations")
                    }
                }
            }

            state.sessionDeleteMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            state.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
