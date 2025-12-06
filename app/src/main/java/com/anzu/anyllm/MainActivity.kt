package com.anzu.anyllm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.anzu.anyllm.ui.navigation.AppNavigation
import com.anzu.anyllm.ui.navigation.Screen
import com.anzu.anyllm.ui.theme.AnyLLMTheme
import com.anzu.anyllm.viewmodel.AppViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnyLLMTheme {
                val appViewModel: AppViewModel = hiltViewModel()
                val isLoading by appViewModel.isLoading.collectAsState()
                val isFirstLaunch by appViewModel.isFirstLaunch.collectAsState()

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    val navController = rememberNavController()
                    val startDestination = if (isFirstLaunch == true) {
                        Screen.Welcome.route
                    } else {
                        Screen.ProfileList.route
                    }

                    AppNavigation(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
