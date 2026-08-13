package com.dpashko.localollamaapp.presentation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dpashko.localollamaapp.presentation.conversationlist.ConversationListRoute
import com.dpashko.localollamaapp.presentation.connection.ConnectionRoute

@Composable
fun LocalOllamaAppRoot() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Connection,
    ) {
        composable(Routes.Connection) {
            ConnectionRoute(
                viewModel = hiltViewModel(),
                onOpenConversations = { host, port, modelName ->
                    navController.navigate(Routes.conversationList(host, port, modelName))
                },
            )
        }

        composable(
            route = Routes.ConversationList,
            arguments = listOf(
                navArgument(Routes.ArgHost) { type = NavType.StringType },
                navArgument(Routes.ArgPort) { type = NavType.IntType },
                navArgument(Routes.ArgModelName) { type = NavType.StringType },
            ),
        ) {
            ConversationListRoute(
                viewModel = hiltViewModel(),
                onBack = navController::popBackStack,
                onOpenConversation = { host, port, modelName, conversationId ->
                    navController.navigate(
                        Routes.conversation(
                            host = host,
                            port = port,
                            modelName = modelName,
                            conversationId = conversationId,
                        ),
                    )
                },
            )
        }
    }
}

object Routes {
    const val Connection = "connection"
    const val ArgHost = "host"
    const val ArgPort = "port"
    const val ArgModelName = "modelName"
    const val ArgConversationId = "conversationId"

    const val ConversationList = "conversations/{$ArgHost}/{$ArgPort}/{$ArgModelName}"
    const val Conversation = "conversation/{$ArgHost}/{$ArgPort}/{$ArgModelName}/{$ArgConversationId}"

    fun conversationList(
        host: String,
        port: Int,
        modelName: String,
    ): String = "conversations/${Uri.encode(host)}/$port/${Uri.encode(modelName)}"

    fun conversation(
        host: String,
        port: Int,
        modelName: String,
        conversationId: Long,
    ): String = "conversation/${Uri.encode(host)}/$port/${Uri.encode(modelName)}/$conversationId"
}
