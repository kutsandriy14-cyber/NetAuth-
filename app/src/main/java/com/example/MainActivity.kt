package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var onNewIntentCallback: ((android.content.Intent) -> Unit)? = null

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        onNewIntentCallback?.invoke(intent)
    }

    private fun handleConfigIntent(intent: android.content.Intent?, viewModel: AccountViewModel) {
        val uri = intent?.data
        if (uri != null && uri.scheme == "netauth" && uri.host == "config") {
            val serverUrl = uri.getQueryParameter("server_url")
            val emailSuffix = uri.getQueryParameter("email_suffix")
            val connectionMode = uri.getQueryParameter("connection_mode")

            if (serverUrl != null) {
                viewModel.setServerUrl(serverUrl)
            }
            if (emailSuffix != null) {
                viewModel.setEmailSuffix(emailSuffix)
            }
            if (connectionMode != null) {
                viewModel.setConnectionMode(connectionMode)
            }

            android.widget.Toast.makeText(
                this,
                "NetAuth Client: Server Configuration Updated",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val accountViewModel: AccountViewModel = viewModel()

                // Register broadcast receiver and intent callbacks
                val context = androidx.compose.ui.platform.LocalContext.current
                androidx.compose.runtime.DisposableEffect(Unit) {
                    val receiver = object : android.content.BroadcastReceiver() {
                        override fun onReceive(ctx: android.content.Context?, intent: android.content.Intent?) {
                            intent?.let {
                                val serverUrl = it.getStringExtra("server_url")
                                val emailSuffix = it.getStringExtra("email_suffix")
                                val connectionMode = it.getStringExtra("connection_mode")

                                if (serverUrl != null) accountViewModel.setServerUrl(serverUrl)
                                if (emailSuffix != null) accountViewModel.setEmailSuffix(emailSuffix)
                                if (connectionMode != null) accountViewModel.setConnectionMode(connectionMode)

                                android.widget.Toast.makeText(
                                    context,
                                    "NetAuth Client: Configured via Broadcast",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    val filter = android.content.IntentFilter("com.example.netauth.CONFIGURE")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        context.registerReceiver(receiver, filter, android.content.Context.RECEIVER_EXPORTED)
                    } else {
                        context.registerReceiver(receiver, filter)
                    }

                    // Initial check
                    handleConfigIntent(intent, accountViewModel)

                    onNewIntentCallback = { newIntent ->
                        handleConfigIntent(newIntent, accountViewModel)
                    }

                    onDispose {
                        context.unregisterReceiver(receiver)
                        onNewIntentCallback = null
                    }
                }

                val isAppLocked by accountViewModel.isAppLocked.collectAsState()

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.AccountChooser.route,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            composable(Screen.AccountChooser.route) {
                                AccountChooserScreen(
                                    viewModel = accountViewModel,
                                    onNavigate = { screen -> navController.navigate(screen.route) }
                                )
                            }

                            composable(Screen.SignIn.route) {
                                SignInScreen(
                                    viewModel = accountViewModel,
                                    onNavigate = { screen -> navController.navigate(screen.route) },
                                    onLoginSuccess = {
                                        navController.navigate(Screen.Dashboard.route) {
                                            popUpTo(Screen.AccountChooser.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(Screen.RegisterName.route) {
                                RegisterNameScreen(
                                    viewModel = accountViewModel,
                                    onNavigate = { screen -> navController.navigate(screen.route) },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.RegisterBirthGender.route) {
                                RegisterBirthGenderScreen(
                                    viewModel = accountViewModel,
                                    onNavigate = { screen -> navController.navigate(screen.route) },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.RegisterEmail.route) {
                                RegisterEmailScreen(
                                    viewModel = accountViewModel,
                                    onNavigate = { screen -> navController.navigate(screen.route) },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.RegisterPassword.route) {
                                RegisterPasswordScreen(
                                    viewModel = accountViewModel,
                                    onNavigate = { screen -> navController.navigate(screen.route) },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.RegisterContact.route) {
                                RegisterContactScreen(
                                    viewModel = accountViewModel,
                                    onNavigate = { screen -> navController.navigate(screen.route) },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.RegisterTerms.route) {
                                RegisterTermsScreen(
                                    viewModel = accountViewModel,
                                    onNavigate = { screen -> navController.navigate(screen.route) },
                                    onBack = { navController.popBackStack() },
                                    onRegisterSuccess = {
                                        navController.navigate(Screen.Dashboard.route) {
                                            popUpTo(Screen.AccountChooser.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(Screen.Dashboard.route) {
                                DashboardScreen(
                                    viewModel = accountViewModel,
                                    onSignOut = {
                                        navController.navigate(Screen.AccountChooser.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (isAppLocked) {
                        PasscodeLockScreen(
                            viewModel = accountViewModel,
                            onUnlock = { accountViewModel.unlockApp() }
                        )
                    }
                }
            }
        }
    }
}
