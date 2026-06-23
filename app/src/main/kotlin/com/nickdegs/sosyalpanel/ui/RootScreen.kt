package com.nickdegs.sosyalpanel.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nickdegs.sosyalpanel.AppViewModel
import com.nickdegs.sosyalpanel.R
import com.nickdegs.sosyalpanel.ui.components.AnimatedMeshBackground
import com.nickdegs.sosyalpanel.ui.screens.*

private enum class Tab(val labelRes: Int, val sel: ImageVector, val unsel: ImageVector) {
    DASHBOARD(R.string.tab_home, Icons.Filled.GridView, Icons.Outlined.GridView),
    ANALYTICS(R.string.tab_analytics, Icons.Filled.BarChart, Icons.Outlined.BarChart),
    COMPOSE(R.string.tab_share, Icons.Filled.Edit, Icons.Outlined.Edit),
    TIPS(R.string.tab_ai, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    SETTINGS(R.string.tab_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
}

@Composable
fun RootScreen(vm: AppViewModel = viewModel()) {
    val loggedIn by vm.isLoggedIn.collectAsState()
    if (!loggedIn) {
        PhoneLoginScreen(onLoggedIn = { vm.restoreData() })
        return
    }

    var tab by rememberSaveable { mutableStateOf(Tab.DASHBOARD) }

    Box(Modifier.fillMaxSize()) {
        AnimatedMeshBackground()
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(if (tab == t) t.sel else t.unsel, null) },
                            label = { Text(stringResource(t.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                when (tab) {
                    Tab.DASHBOARD -> DashboardScreen(vm)
                    Tab.ANALYTICS -> AnalyticsScreen(vm)
                    Tab.COMPOSE -> ComposerScreen(vm)
                    Tab.TIPS -> AIChatScreen(vm)
                    Tab.SETTINGS -> SettingsScreen(vm)
                }
            }
        }
    }
}
