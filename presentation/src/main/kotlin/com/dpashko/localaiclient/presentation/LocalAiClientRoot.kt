package com.dpashko.localaiclient.presentation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.presentation.applock.AppLockRoute
import com.dpashko.localaiclient.presentation.chat.ChatRoute
import com.dpashko.localaiclient.presentation.conversationlist.ConversationListRoute
import com.dpashko.localaiclient.presentation.connection.ConnectionRoute
import com.dpashko.localaiclient.presentation.dashboard.DashboardRoute
import com.dpashko.localaiclient.presentation.R
import com.dpashko.localaiclient.presentation.serverselection.ServerSelectionRoute
import com.dpashko.localaiclient.presentation.settings.SettingsRoute

/**
 * Root navigation graph for connection, conversation list, settings, and chat screens.
 */
@Composable
fun LocalAiClientRoot() {
    AppLockRoute(viewModel = hiltViewModel()) {
        LocalAiClientNavHost()
    }
}

@Composable
private fun LocalAiClientNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.ServerSelection,
    ) {
        composable(Routes.ServerSelection) {
            ServerSelectionRoute(
                viewModel = hiltViewModel(),
                onAddServer = { navController.navigate(Routes.Connection) },
                onOpenConnected = { provider, host, port, modelName ->
                    navController.navigate(Routes.connected(provider, host, port, modelName)) {
                        popUpTo(Routes.ServerSelection) {
                            inclusive = false
                        }
                    }
                },
            )
        }

        composable(Routes.Connection) {
            ConnectionRoute(
                viewModel = hiltViewModel(),
                onBack = navController::popBackStack,
            )
        }

        composable(
            route = Routes.Connected,
            arguments = listOf(
                navArgument(Routes.ArgProvider) { type = NavType.StringType },
                navArgument(Routes.ArgHost) { type = NavType.StringType },
                navArgument(Routes.ArgPort) { type = NavType.IntType },
                navArgument(Routes.ArgModelName) { type = NavType.StringType },
            ),
        ) {
            ConnectedShell(
                onOpenSettings = { navController.navigate(Routes.Settings) },
                onDisconnect = {
                    navController.navigate(Routes.ServerSelection) {
                        popUpTo(Routes.Connected) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onOpenConversation = { provider, host, port, modelName, conversationId ->
                    navController.navigate(
                        Routes.conversation(
                            provider = provider,
                            host = host,
                            port = port,
                            modelName = modelName,
                            conversationId = conversationId,
                        ),
                    )
                },
            )
        }

        composable(Routes.Settings) {
            SettingsRoute(
                viewModel = hiltViewModel(),
                onBack = navController::popBackStack,
            )
        }

        composable(
            route = Routes.Conversation,
            arguments = listOf(
                navArgument(Routes.ArgProvider) { type = NavType.StringType },
                navArgument(Routes.ArgHost) { type = NavType.StringType },
                navArgument(Routes.ArgPort) { type = NavType.IntType },
                navArgument(Routes.ArgModelName) { type = NavType.StringType },
                navArgument(Routes.ArgConversationId) { type = NavType.LongType },
            ),
        ) {
            ChatRoute(
                viewModel = hiltViewModel(),
                onBack = navController::popBackStack,
            )
        }
    }
}

@Composable
private fun ConnectedShell(
    onOpenSettings: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenConversation: (AiProvider, String, Int, String, Long) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(ConnectedTab.CONVERSATIONS) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                ConnectedTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                painter = painterResource(tab.iconResId),
                                contentDescription = null,
                            )
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            ConnectedTab.CONVERSATIONS -> ConversationListRoute(
                modifier = Modifier.padding(innerPadding),
                viewModel = hiltViewModel(),
                isArchive = false,
                onOpenSettings = onOpenSettings,
                onDisconnect = onDisconnect,
                onOpenConversation = onOpenConversation,
            )

            ConnectedTab.ARCHIVE -> ConversationListRoute(
                modifier = Modifier.padding(innerPadding),
                viewModel = hiltViewModel(),
                isArchive = true,
                onOpenSettings = onOpenSettings,
                onDisconnect = onDisconnect,
                onOpenConversation = onOpenConversation,
            )

            ConnectedTab.DASHBOARD -> DashboardRoute(
                modifier = Modifier.padding(innerPadding),
                viewModel = hiltViewModel(),
                onOpenConversation = onOpenConversation,
            )
        }
    }
}

private enum class ConnectedTab(
    val label: String,
    val iconResId: Int,
) {
    CONVERSATIONS("Conversations", R.drawable.ic_chat),
    ARCHIVE("Archive", R.drawable.ic_archive),
    DASHBOARD("Dashboard", R.drawable.ic_monitoring),
}

/**
 * Compose Navigation route definitions and route builders.
 */
object Routes {
    /** Saved server selection route. */
    const val ServerSelection = "server-selection"
    /** Connection screen route. */
    const val Connection = "connection"
    /** Provider route argument name. */
    const val ArgProvider = "provider"
    /** Provider host route argument name. */
    const val ArgHost = "host"
    /** Provider port route argument name. */
    const val ArgPort = "port"
    /** Selected model route argument name. */
    const val ArgModelName = "modelName"
    /** Conversation id route argument name. */
    const val ArgConversationId = "conversationId"

    /** Settings screen route. */
    const val Settings = "settings"
    /** Connected app shell route including provider connection arguments. */
    const val Connected = "connected/{$ArgProvider}/{$ArgHost}/{$ArgPort}/{$ArgModelName}"
    /** Chat route including provider connection and conversation arguments. */
    const val Conversation = "conversation/{$ArgProvider}/{$ArgHost}/{$ArgPort}/{$ArgModelName}/{$ArgConversationId}"

    /** Builds a connected app shell route from provider connection details. */
    fun connected(
        provider: AiProvider,
        host: String,
        port: Int,
        modelName: String,
    ): String = "connected/${provider.routeValue}/${Uri.encode(host)}/$port/${Uri.encode(modelName)}"

    /** Builds a chat route for the selected conversation. */
    fun conversation(
        provider: AiProvider,
        host: String,
        port: Int,
        modelName: String,
        conversationId: Long,
    ): String = "conversation/${provider.routeValue}/${Uri.encode(host)}/$port/${Uri.encode(modelName)}/$conversationId"
}
