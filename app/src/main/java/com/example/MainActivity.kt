package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminLoginScreen
import com.example.ui.screens.EmployeeHomeScreen
import com.example.ui.screens.RoleSelectionScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          val viewModel: MainViewModel = viewModel()
          var currentScreen by remember { mutableStateOf<Screen>(Screen.RoleSelection) }

          AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
              fadeIn() togetherWith fadeOut()
            },
            label = "screen_navigation"
          ) { screen ->
            when (screen) {
              is Screen.RoleSelection -> {
                RoleSelectionScreen(
                  onSelectAdmin = { currentScreen = Screen.AdminLogin },
                  onSelectUser = { currentScreen = Screen.EmployeeHome }
                )
              }
              is Screen.AdminLogin -> {
                AdminLoginScreen(
                  viewModel = viewModel,
                  onLoginSuccess = { currentScreen = Screen.AdminDashboard },
                  onBack = { currentScreen = Screen.RoleSelection }
                )
              }
              is Screen.AdminDashboard -> {
                AdminDashboardScreen(
                  viewModel = viewModel,
                  onLogout = {
                    viewModel.logoutAdmin()
                    currentScreen = Screen.RoleSelection
                  }
                )
              }
              is Screen.EmployeeHome -> {
                EmployeeHomeScreen(
                  viewModel = viewModel,
                  onBack = { currentScreen = Screen.RoleSelection }
                )
              }
            }
          }
        }
      }
    }
  }
}

sealed class Screen {
  object RoleSelection : Screen()
  object AdminLogin : Screen()
  object AdminDashboard : Screen()
  object EmployeeHome : Screen()
}
