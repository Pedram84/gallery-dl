package com.example.gallerydl.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gallerydl.ui.theme.*

enum class AppTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DOWNLOADER("Download", Icons.Filled.Download, Icons.Outlined.Download, "tab_downloader"),
    JOBS("History", Icons.Filled.History, Icons.Outlined.History, "tab_jobs"),
    EXTRACTORS("Sites", Icons.Filled.Public, Icons.Outlined.Public, "tab_extractors"),
    CONFIG("Config", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_config"),
    TERMINAL("Terminal", Icons.Filled.Terminal, Icons.Outlined.Terminal, "tab_terminal")
}

@Composable
fun AppBottomBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = DarkSurface,
        contentColor = TextPrimary,
        tonalElevation = 8.dp,
        modifier = modifier.navigationBarsPadding()
    ) {
        AppTab.values().forEach { tab ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.title,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        fontSize = 11.sp,
                        color = if (isSelected) AmberPrimary else TextMuted
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DarkBackground,
                    selectedTextColor = AmberPrimary,
                    indicatorColor = AmberPrimary,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                ),
                modifier = Modifier.testTag(tab.testTag)
            )
        }
    }
}
