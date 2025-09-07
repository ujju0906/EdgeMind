package com.ml.EdgeBrain.docqa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ml.EdgeBrain.docqa.ui.screens.advanced_options.AdvancedOptionsScreen
import com.ml.EdgeBrain.docqa.ui.screens.chat.ChatScreen
import com.ml.EdgeBrain.docqa.ui.screens.docs.DocsScreen
import com.ml.EdgeBrain.docqa.ui.screens.edit_api_key.EditAPIKeyScreen
import com.ml.EdgeBrain.docqa.ui.screens.model_download.ModelDownloadScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navHostController = rememberNavController()
            NavHost(
                navController = navHostController,
                startDestination = "chat",
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
            ) {
                composable("docs") { DocsScreen(onBackClick = { navHostController.navigateUp() }) }
                composable("edit-api-key") { EditAPIKeyScreen(onBackClick = { navHostController.navigateUp() }) }
                composable("chat") {
                    ChatScreen(
                        onOpenDocsClick = { navHostController.navigate("docs") },
                        onEditAPIKeyClick = { navHostController.navigate("edit-api-key") },
                        onModelDownloadClick = { navHostController.navigate("model_download") },
                        onAdvancedOptionsClick = { navHostController.navigate("advanced_options") }
                    )
                }
                composable("model_download") {
                    ModelDownloadScreen(onBackClick = { navHostController.navigateUp() })
                }
                composable("advanced_options") { AdvancedOptionsScreen(onBackClick = { navHostController.navigateUp() }) }
            }
        }
    }
}
