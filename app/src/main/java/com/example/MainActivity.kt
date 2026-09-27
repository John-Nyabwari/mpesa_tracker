package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.local.entities.TransactionEntity
import com.example.ui.screens.*
import com.example.ui.theme.MpesaTrackerTheme
import com.example.ui.viewmodels.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MpesaTrackerApp
        val factory = AppViewModelFactory(app.repository, applicationContext)

        setContent {
            MpesaTrackerTheme {
                val navController = rememberNavController()
                val hasReadPermission = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED
                val hasReceivePermission = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECEIVE_SMS
                ) == PackageManager.PERMISSION_GRANTED
                val startDestination = if (hasReadPermission && hasReceivePermission) "dashboard" else "permissions"

                val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
                val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
                val rulesViewModel: RulesViewModel = viewModel(factory = factory)
                val categoriesViewModel: CategoriesViewModel = viewModel(factory = factory)

                val dashboardState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
                val userPrefs by settingsViewModel.preferences.collectAsStateWithLifecycle()
                val settingsFeedback by settingsViewModel.feedbackMessage.collectAsStateWithLifecycle()
                val keywordRules by rulesViewModel.keywordRules.collectAsStateWithLifecycle()
                val merchantRules by rulesViewModel.merchantRules.collectAsStateWithLifecycle()
                val pendingUnparsed by rulesViewModel.pendingUnparsed.collectAsStateWithLifecycle()
                val ruleStatusMessage by rulesViewModel.ruleStatusMessage.collectAsStateWithLifecycle()
                val categories by categoriesViewModel.categories.collectAsStateWithLifecycle()

                var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("permissions") {
                        PermissionsScreen(
                            onComplete = {
                                dashboardViewModel.scanDeviceInboxNow()
                                navController.navigate("dashboard") {
                                    popUpTo("permissions") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("dashboard") {
                        DashboardScreen(
                            state = dashboardState,
                            unparsedCount = pendingUnparsed.size,
                            onPeriodSelected = { dashboardViewModel.setPeriod(it) },
                            onCategoryFilterSelected = { dashboardViewModel.setCategoryFilter(it) },
                            onDirectionFilterSelected = { dashboardViewModel.setDirectionFilter(it) },
                            onSearchQueryChange = { dashboardViewModel.setSearchQuery(it) },
                            onTransactionClick = { tx -> editingTx = tx },
                            onScanInboxNow = { dashboardViewModel.scanDeviceInboxNow() },
                            onIngestSingleSms = { sender, body -> dashboardViewModel.ingestSingleSms(sender, body) },
                            onIngestAllSampleSubtypes = { dashboardViewModel.ingestAllSampleSubtypes() },
                            onDismissBanner = { dashboardViewModel.clearStatusBanner() },
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToRules = { navController.navigate("rules") },
                            onNavigateToCategories = { navController.navigate("categories") }
                        )

                        editingTx?.let { tx ->
                            TransactionEditDialog(
                                transaction = tx,
                                categories = categories,
                                currency = dashboardState.currencySymbol,
                                onDismiss = { editingTx = null },
                                onSaveCategory = { newCat, rememberMerchant ->
                                    dashboardViewModel.updateTransactionCategory(tx, newCat, rememberMerchant)
                                    editingTx = null
                                },
                                onDeleteTransaction = {
                                    dashboardViewModel.deleteTransaction(tx)
                                    editingTx = null
                                }
                            )
                        }
                    }

                    composable("settings") {
                        SettingsScreen(
                            prefs = userPrefs,
                            feedbackMessage = settingsFeedback,
                            onClearFeedback = { settingsViewModel.clearFeedback() },
                            onBack = { navController.popBackStack() },
                            onSaveProfile = { name, cur, senders ->
                                settingsViewModel.updateProfile(name, cur, senders)
                            },
                            onUpdateTrackingStartDate = { newDate ->
                                settingsViewModel.updateTrackingStartDate(newDate)
                            }
                        )
                    }

                    composable("rules") {
                        RulesAndReviewScreen(
                            rules = keywordRules,
                            merchantRules = merchantRules,
                            unparsed = pendingUnparsed,
                            statusMessage = ruleStatusMessage,
                            onClearStatus = { rulesViewModel.clearStatusMessage() },
                            onBack = { navController.popBackStack() },
                            onAddRule = { kw, dir, cat, rgx ->
                                rulesViewModel.addRule(kw, dir, cat, rgx)
                            },
                            onDeleteRule = { rule ->
                                rulesViewModel.deleteRule(rule)
                            },
                            onDeleteMerchantRule = { mRule ->
                                rulesViewModel.deleteMerchantRule(mRule)
                            },
                            onDismissUnparsed = { item ->
                                rulesViewModel.dismissUnparsed(item)
                            }
                        )
                    }

                    composable("categories") {
                        CategoriesScreen(
                            categories = categories,
                            onBack = { navController.popBackStack() },
                            onAddCategory = { name, hex ->
                                categoriesViewModel.createCategory(name, hex)
                            },
                            onUpdateCategory = { oldName, newName, hex ->
                                categoriesViewModel.updateCategory(oldName, newName, hex)
                            },
                            onDeleteCategory = { cat ->
                                categoriesViewModel.deleteCategory(cat)
                            }
                        )
                    }
                }
            }
        }
    }
}
