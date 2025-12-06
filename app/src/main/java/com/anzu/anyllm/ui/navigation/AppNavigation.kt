package com.anzu.anyllm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.anzu.anyllm.ui.screen.chat.ChatScreen
import com.anzu.anyllm.ui.screen.profile.ProfileEditorScreen
import com.anzu.anyllm.ui.screen.profile.ProfileListScreen
import com.anzu.anyllm.ui.screen.welcome.WelcomeScreen

/**
 * 导航路由定义
 */
sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object ProfileList : Screen("profile_list")
    data object ProfileEditor : Screen("profile_editor/{profileId}") {
        fun createRoute(profileId: String? = null) = "profile_editor/${profileId ?: "new"}"
    }
    data object Chat : Screen("chat/{profileId}") {
        fun createRoute(profileId: String) = "chat/$profileId"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 欢迎页
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToProfileList = {
                    navController.navigate(Screen.ProfileList.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        // 配置列表
        composable(Screen.ProfileList.route) {
            ProfileListScreen(
                onNavigateToEditor = { profileId ->
                    navController.navigate(Screen.ProfileEditor.createRoute(profileId))
                },
                onNavigateToChat = { profileId ->
                    navController.navigate(Screen.Chat.createRoute(profileId))
                }
            )
        }

        // 配置编辑器
        composable(
            route = Screen.ProfileEditor.route,
            arguments = listOf(
                navArgument("profileId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId")
            ProfileEditorScreen(
                profileId = if (profileId == "new") null else profileId,
                onNavigateBack = { navController.popBackStack() },
                onSaveComplete = { navController.popBackStack() }
            )
        }

        // 聊天界面
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("profileId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: return@composable
            ChatScreen(
                profileId = profileId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = {
                    navController.navigate(Screen.ProfileEditor.createRoute(profileId))
                }
            )
        }
    }
}

