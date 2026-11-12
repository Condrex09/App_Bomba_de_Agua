package com.example.app_bomba_de_agua.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.app_bomba_de_agua.network.IoTNetworkClient
import com.example.app_bomba_de_agua.network.NetworkObserver
import com.example.app_bomba_de_agua.security.SecureStorage

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterPumpApp() {
    val context = LocalContext.current
    val secureStorage = remember { SecureStorage(context) }
    val networkClient = remember { IoTNetworkClient() }
    val networkObserver = remember { NetworkObserver(context) }

    val isWifiConnected by networkObserver.isConnected.collectAsState(initial = networkObserver.isWifiOrNetworkAvailable())
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.LOGIN

    val startDestination = if (secureStorage.isLoggedIn()) Routes.DASHBOARD else Routes.LOGIN

    Scaffold(
        topBar = {
            if (currentRoute != Routes.LOGIN) {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentRoute) {
                                Routes.DASHBOARD -> "Dashboard IoT Bomba"
                                Routes.SETTINGS -> "Configuración IP"
                                else -> "Bomba de Agua IoT"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    navigationIcon = {
                        if (currentRoute == Routes.SETTINGS) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Volver"
                                )
                            }
                        }
                    },
                    actions = {
                        if (currentRoute == Routes.DASHBOARD) {
                            IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Configuración"
                                )
                            }
                            IconButton(onClick = {
                                secureStorage.setLoggedIn(false)
                                navController.navigate(Routes.LOGIN) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = "Cerrar Sesión"
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    secureStorage = secureStorage,
                    onLoginSuccess = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    secureStorage = secureStorage,
                    networkClient = networkClient,
                    isWifiConnected = isWifiConnected,
                    onNavigateToSettings = {
                        navController.navigate(Routes.SETTINGS)
                    }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    secureStorage = secureStorage,
                    networkClient = networkClient,
                    onSavedAndBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
