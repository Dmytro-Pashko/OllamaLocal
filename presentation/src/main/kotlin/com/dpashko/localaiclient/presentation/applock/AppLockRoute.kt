package com.dpashko.localaiclient.presentation.applock

import android.content.Context
import android.content.ContextWrapper
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * Renders private content only after optional Android device unlock succeeds.
 */
@Composable
fun AppLockRoute(
    viewModel: AppLockViewModel,
    content: @Composable () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    LaunchedEffect(state.unlockRequestNonce, state.isLockRequired, state.isUnlocked, activity) {
        if (state.unlockRequestNonce > 0 && state.isLockRequired && !state.isUnlocked) {
            if (activity == null) {
                viewModel.onUnlockFailed("Device unlock is not available.")
            } else {
                activity.showDeviceUnlockPrompt(
                    onSucceeded = viewModel::onUnlockSucceeded,
                    onFailed = viewModel::onUnlockFailed,
                )
            }
        }
    }

    when {
        state.canShowContent -> content()
        state.isLoading -> LoadingLockScreen()
        else -> LockedScreen(
            errorMessage = state.errorMessage,
            onUnlockClick = viewModel::requestUnlock,
        )
    }
}

@Composable
private fun LoadingLockScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LockedScreen(
    errorMessage: String?,
    onUnlockClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = "Local AI Client is locked",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onUnlockClick) {
            Text("Unlock")
        }
        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun ComponentActivity.showDeviceUnlockPrompt(
    onSucceeded: () -> Unit,
    onFailed: (String) -> Unit,
) {
    val prompt = BiometricPrompt.Builder(this)
        .setTitle("Unlock Local AI Client")
        .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        .build()

    prompt.authenticate(
        CancellationSignal(),
        ContextCompat.getMainExecutor(this),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSucceeded()
            }

            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence,
            ) {
                onFailed(errString.toString())
            }

            override fun onAuthenticationFailed() {
                onFailed("Unlock failed.")
            }
        },
    )
}

private tailrec fun Context.findActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
