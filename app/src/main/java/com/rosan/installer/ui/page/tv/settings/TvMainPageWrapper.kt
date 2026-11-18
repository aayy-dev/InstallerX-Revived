// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.settings

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.tv.material3.Icon
import androidx.tv.material3.ModalNavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Text
import com.rosan.installer.domain.settings.repository.ConfigRepository
import com.rosan.installer.ui.navigation.LocalNavigator
import com.rosan.installer.ui.navigation.Route
import com.rosan.installer.ui.navigation.rememberSettingsNavigationTabs
import com.rosan.installer.ui.page.main.settings.SettingsSharedViewModel
import com.rosan.installer.ui.page.main.settings.config.all.AllPage
import com.rosan.installer.ui.page.main.settings.history.HistoryPage
import com.rosan.installer.ui.page.main.settings.home.HomePage
import com.rosan.installer.ui.page.main.settings.preferred.PreferredPage
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

/**
 * TV main settings screen: a TV navigation drawer for the four main tabs, hosting the same tab
 * pages as the phone layouts. Only the selected page is shown, so left/right on the remote moves
 * between the drawer and the page instead of swiping a pager.
 */
@Composable
fun TvMainPageWrapper(sharedViewModel: SettingsSharedViewModel) {
    val sharedState by sharedViewModel.state.collectAsStateWithLifecycle()
    val tabs = rememberSettingsNavigationTabs()

    val configRepo = koinInject<ConfigRepository>()
    val configCountFlow = remember { configRepo.flowAll().map { it.size } }
    val configCount by configCountFlow.collectAsStateWithLifecycle(initialValue = 0)

    var selectedPage by rememberSaveable { mutableIntStateOf(sharedState.lastMainPageIndex.coerceIn(tabs.indices)) }
    LaunchedEffect(selectedPage) {
        sharedViewModel.updateLastMainPageIndex(selectedPage)
    }
    // Back returns to Home first, then leaves the app. Same guard as MainScreenBackHandler, so it
    // never intercepts Back while a sub-page is on top of Main.
    val navigator = LocalNavigator.current
    val isBackToHomeEnabled by remember(navigator) {
        derivedStateOf { navigator.current() is Route.Main && navigator.backStackSize() == 1 && selectedPage != 0 }
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = isBackToHomeEnabled,
        onBackCompleted = { selectedPage = 0 },
    )
    val pageStateHolder = rememberSaveableStateHolder()

    val itemFocusRequesters = remember(tabs.size) { List(tabs.size) { FocusRequester() } }

    ModalNavigationDrawer(
        drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
                    // Entering the drawer lands on the current page's item, not the first one.
                    .focusProperties { onEnter = { itemFocusRequesters[selectedPage].requestFocus() } }
                    .focusGroup()
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationDrawerItem(
                        selected = selectedPage == index,
                        onClick = { selectedPage = index },
                        leadingContent = { Icon(imageVector = tab.icon, contentDescription = null) },
                        modifier = Modifier.focusRequester(itemFocusRequesters[index]),
                    ) {
                        Text(tab.label)
                    }
                }
            }
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // The modal drawer overlays content; keep the page clear of its collapsed rail.
                .padding(start = CollapsedDrawerInset),
        ) {
            val noPadding = PaddingValues(0.dp)
            // Keeps each tab's scroll position when switching, as the phone pager does.
            pageStateHolder.SaveableStateProvider(selectedPage) {
                when (selectedPage) {
                    0 -> HomePage(
                        useBlur = false,
                        title = tabs[0].label,
                        outerPadding = noPadding,
                        configCount = configCount,
                        onNavigateToProfiles = { selectedPage = 1 },
                    )

                    1 -> AllPage(useBlur = false, title = tabs[1].label, outerPadding = noPadding)

                    2 -> HistoryPage(useBlur = false, title = tabs[2].label, outerPadding = noPadding)

                    3 -> PreferredPage(useBlur = false, title = tabs[3].label, outerPadding = noPadding)
                }
            }
        }
    }
}

private val CollapsedDrawerInset = 80.dp
