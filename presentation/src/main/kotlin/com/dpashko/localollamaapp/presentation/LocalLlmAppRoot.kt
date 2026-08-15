package com.dpashko.localollamaapp.presentation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dpashko.localollamaapp.domain.models.connection.AiProvider
import com.dpashko.localollamaapp.presentation.chat.ChatRoute
import com.dpashko.localollamaapp.presentation.conversationlist.ConversationListRoute
import com.dpashko.localollamaapp.presentation.connection.ConnectionRoute

@Composable
fun LocalLlmAppRoot() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Connection,
    ) {
        composable(Routes.Connection) {
            ConnectionRoute(
                viewModel = hiltViewModel(),
                onOpenConversations = { provider, host, port, modelName ->
                    navController.navigate(Routes.conversationList(provider, host, port, modelName))
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
                onBack = navController::popBackStack,
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

object Routes {
    const val Connection = "connection"
    const val ArgProvider = "provider"
    const val ArgHost = "host"
    const val ArgPort = "port"
    const val ArgModelName = "modelName"
    const val ArgConversationId = "conversationId"

    const val ConversationList = "conversations/{$ArgProvider}/{$ArgHost}/{$ArgPort}/{$ArgModelName}"
    const val Conversation = "conversation/{$ArgProvider}/{$ArgHost}/{$ArgPort}/{$ArgModelName}/{$ArgConversationId}"

    fun conversationList(
        provider: AiProvider,
        host: String,
        port: Int,
        modelName: String,
    ): String = "conversations/${provider.routeValue}/${Uri.encode(host)}/$port/${Uri.encode(modelName)}"

    fun conversation(
        provider: AiProvider,
        host: String,
        port: Int,
        modelName: String,
        conversationId: Long,
    ): String = "conversation/${provider.routeValue}/${Uri.encode(host)}/$port/${Uri.encode(modelName)}/$conversationId"
}
