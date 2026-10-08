package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.api.RetrofitInstance
import com.example.myapplication.ui.screens.LoginScreen
import com.example.myapplication.ui.screens.MainScaffoldScreen
import com.example.myapplication.ui.screens.SignupScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.viewmodel.AuthViewModel
import com.example.myapplication.viewmodel.LedgerlyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Retrofit TokenStore & Unauthorized listener
        RetrofitInstance.init(applicationContext)

        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val ledgerlyViewModel: LedgerlyViewModel = viewModel()

            val isDarkMode by authViewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    LedgerlyApp(authViewModel, ledgerlyViewModel)
                }
            }
        }
    }
}

@Composable
fun LedgerlyApp(
    authViewModel: AuthViewModel,
    ledgerlyViewModel: LedgerlyViewModel
) {
    val navController = rememberNavController()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    val startDestination = if (isLoggedIn) "main" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToSignup = { navController.navigate("signup") },
                onLoginSuccess = {
                    ledgerlyViewModel.loadDataInParallel(isInitial = false)
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("signup") {
            SignupScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onSignupSuccess = {
                    ledgerlyViewModel.loadDataInParallel(isInitial = false)
                    navController.navigate("main") {
                        popUpTo("signup") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainScaffoldScreen(
                authViewModel = authViewModel,
                ledgerlyViewModel = ledgerlyViewModel,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
    }
}


