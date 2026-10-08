package com.example.myapplication.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.viewmodel.AuthViewModel
import com.example.myapplication.viewmodel.LedgerlyViewModel

enum class MainTab(val label: String) {
    Dashboard("Dashboard"),
    Transactions("Transactions"),
    Salary("Salary"),
    Budget("Budget"),
    Reports("Reports"),
    Settings("Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffoldScreen(
    authViewModel: AuthViewModel,
    ledgerlyViewModel: LedgerlyViewModel,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(MainTab.Dashboard) }
    val userName by authViewModel.userName.collectAsState()
    val snackbarMsg by ledgerlyViewModel.snackbarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            ledgerlyViewModel.snackbarMessage.value = null
        }
    }

    val initials = userName.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifBlank { "U" }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ledgerly",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { selectedTab = MainTab.Settings },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = (selectedTab == MainTab.Dashboard),
                    onClick = { selectedTab = MainTab.Dashboard },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = (selectedTab == MainTab.Transactions),
                    onClick = { selectedTab = MainTab.Transactions },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Transactions") },
                    label = { Text("Txns") }
                )
                NavigationBarItem(
                    selected = (selectedTab == MainTab.Salary),
                    onClick = { selectedTab = MainTab.Salary },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Salary") },
                    label = { Text("Salary") }
                )
                NavigationBarItem(
                    selected = (selectedTab == MainTab.Budget),
                    onClick = { selectedTab = MainTab.Budget },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Budget") },
                    label = { Text("Budget") }
                )
                NavigationBarItem(
                    selected = (selectedTab == MainTab.Reports),
                    onClick = { selectedTab = MainTab.Reports },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Reports") },
                    label = { Text("Reports") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Crossfade(targetState = selectedTab, label = "tabCrossfade") { tab ->
                when (tab) {
                    MainTab.Dashboard -> DashboardScreen(authViewModel, ledgerlyViewModel)
                    MainTab.Transactions -> TransactionsScreen(authViewModel, ledgerlyViewModel)
                    MainTab.Salary -> SalaryScreen(authViewModel, ledgerlyViewModel)
                    MainTab.Budget -> BudgetScreen(authViewModel, ledgerlyViewModel)
                    MainTab.Reports -> ReportsScreen(authViewModel, ledgerlyViewModel)
                    MainTab.Settings -> SettingsScreen(authViewModel, onLogout)
                }
            }
        }
    }
}
