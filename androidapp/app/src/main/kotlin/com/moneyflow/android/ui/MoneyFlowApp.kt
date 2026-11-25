@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.moneyflow.android.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.moneyflow.android.R
import com.moneyflow.android.domain.Kind

private enum class Destination(val route: String, val label: String, val icon: Int) {
    Dashboard("dashboard", "Dashboard", R.drawable.ic_dashboard),
    Expenses("expenses", "Expenses", R.drawable.ic_expense),
    Income("income", "Income", R.drawable.ic_income),
    Categories("categories", "Categories", R.drawable.ic_categories),
}

@Composable
internal fun MoneyFlowApp(model: MoneyViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    MoneyTheme(state.settings) { dark ->
        val activity = LocalActivity.current
        SideEffect {
            activity?.window?.let { window ->
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
        Surface(Modifier.fillMaxSize()) {
            when {
                state.restoring -> LoadingScreen(null, model::refresh)
                state.accountName == null -> AuthScreen(
                    state.registration, state.authBusy, state.authError, state.authNotice,
                    model::showRegistration, model::login, model::register,
                )
                else -> key(state.accountKey) { Workspace(state, model) }
            }
        }
    }
}

@Composable
private fun Workspace(state: MoneyState, model: MoneyViewModel) {
    val nav = rememberNavController()
    val destinationState = rememberUpdatedState(state)
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Destination.Dashboard.route
    val selected = Destination.entries.firstOrNull { it.route == route }
    val snackbar = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            if (message in listOf("Transaction added", "Changes saved", "Transaction deleted", "Category added", "Category deleted")) {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            }
            snackbar.showSnackbar(message)
            model.consumeMessage(message)
        }
    }

    val select: (Destination) -> Unit = { destination ->
        nav.navigate(destination.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val rail = maxWidth >= 600.dp
        val compactHeight = maxHeight < 520.dp
        val addTransaction = selected == Destination.Dashboard ||
            selected == Destination.Expenses || selected == Destination.Income
        Scaffold(
            modifier = Modifier.imePadding(),
            topBar = {
                TopAppBar(
                    title = { Text(selected?.label ?: "Settings") },
                    navigationIcon = {
                        if (selected == null) IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Go back")
                        }
                    },
                    actions = {
                        if (selected != null) {
                            IconButton(
                                onClick = model::refresh,
                                enabled = !state.refreshing && !state.writing,
                            ) {
                                Icon(Icons.Default.Refresh, "Refresh data")
                            }
                            IconButton(onClick = { nav.navigate("settings") { launchSingleTop = true } }) {
                                Icon(Icons.Default.AccountCircle, "Account and settings")
                            }
                        }
                    },
                )
            },
            bottomBar = {
                if (selected != null && !rail) NavigationBar {
                    Destination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = selected == destination,
                            onClick = { select(destination) },
                            icon = { Icon(painterResource(destination.icon), null) },
                            label = { Text(destination.label, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = {
                if (addTransaction && !state.writing) {
                    val kind = if (selected == Destination.Income) Kind.Income else Kind.Expense
                    val action = { model.openEntry(kind) }
                    val label = "Add ${kind.singular}"
                    if (compactHeight) {
                        FloatingActionButton(onClick = action) { Icon(Icons.Default.Add, label) }
                    } else {
                        ExtendedFloatingActionButton(
                            text = { Text(label) },
                            icon = { Icon(Icons.Default.Add, null) },
                            onClick = action,
                        )
                    }
                }
            },
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                if (selected != null && rail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight(), windowInsets = WindowInsets(0)) {
                        Destination.entries.forEach { destination ->
                            NavigationRailItem(
                                selected = selected == destination,
                                onClick = { select(destination) },
                                icon = { Icon(painterResource(destination.icon), null) },
                                label = { Text(destination.label, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                            )
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    NavHost(
                        navController = nav,
                        startDestination = Destination.Dashboard.route,
                        modifier = Modifier.widthIn(max = 1000.dp).fillMaxSize(),
                    ) {
                        composable(Destination.Dashboard.route) { DashboardScreen(destinationState.value, model) }
                        composable(Destination.Expenses.route) { LedgerScreen(Kind.Expense, destinationState.value, model) }
                        composable(Destination.Income.route) { LedgerScreen(Kind.Income, destinationState.value, model) }
                        composable(Destination.Categories.route) { CategoriesScreen(destinationState.value, model) }
                        composable("settings") { SettingsScreen(destinationState.value, model) }
                    }
                }
            }
        }
        WorkspaceOverlay(state, model)
    }
}
