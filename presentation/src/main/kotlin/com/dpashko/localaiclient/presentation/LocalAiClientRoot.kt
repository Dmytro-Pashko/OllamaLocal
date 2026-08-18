package com.dpashko.localaiclient.presentation

import android.net.Uri
import androidx.compose.runtime.Composable
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
        startDestination = Routes.Connection,
    ) {
        composable(Routes.Connection) {
            ConnectionRoute(
                viewModel = hiltViewModel(),
                onOpenConversations = { provider, host, port, modelName ->
                    navController.navigate(Routes.conversationList(provider, host, port, modelName)) {
                        popUpTo(Routes.Connection) {
                            inclusive = true
                        }
                    }
                },
            )
        }

        composable(
            route = Routes.ConversationList,
            arguments = listOf(
                navArgument(Routes.ArgProvider) { type = NavType.StringType },
                navArgument(Routes.ArgHost) { type = NavType.StringType },
                navArgument(Routes.ArgPort) { type = NavType.IntType },
                navArgument(Routes.ArgModelName) { type = NavType.StringType },
            ),
        ) {
            ConversationListRoute(
                viewModel = hiltViewModel(),
                onOpenSettings = {
                    navController.navigate(Routes.Settings)
                },
                onDisconnect = {
                    navController.navigate(Routes.Connection) {
                        popUpTo(Routes.ConversationList) {
                            inclusive = true
                        }
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

/**
 * Compose Navigation route definitions and route builders.
 */
object Routes {
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
    /** Conversation list route including provider connection arguments. */
    const val ConversationList = "conversations/{$ArgProvider}/{$ArgHost}/{$ArgPort}/{$ArgModelName}"
    /** Chat route including provider connection and conversation arguments. */
    const val Conversation = "conversation/{$ArgProvider}/{$ArgHost}/{$ArgPort}/{$ArgModelName}/{$ArgConversationId}"

    /** Builds a conversation list route from provider connection details. */
    fun conversationList(
        provider: AiProvider,
        host: String,
        port: Int,
        modelName: String,
    ): String = "conversations/${provider.routeValue}/${Uri.encode(host)}/$port/${Uri.encode(modelName)}"

    /** Builds a chat route for the selected conversation. */
    fun conversation(
        provider: AiProvider,
        host: String,
        port: Int,
        modelName: String,
        conversationId: Long,
    ): String = "conversation/${provider.routeValue}/${Uri.encode(host)}/$port/${Uri.encode(modelName)}/$conversationId"
}
