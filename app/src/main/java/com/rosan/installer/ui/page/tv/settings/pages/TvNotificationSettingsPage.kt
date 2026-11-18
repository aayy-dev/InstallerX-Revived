// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.settings.pages

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.installer.notification.NotificationSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.installer.notification.NotificationSettingsViewModel
import com.rosan.installer.ui.page.main.settings.preferred.installer.notification.NotificationStyle
import com.rosan.installer.ui.page.tv.settings.TvChoiceOption
import com.rosan.installer.ui.page.tv.settings.TvSettingsPage
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private val AutoClearSecondsOptions = listOf(0, 3, 5, 10, 15, 20, 30)
private val MiIslandIntervalOptions = (50..350 step 50).toList()

/** Install progress notification settings, matching the phone Notification Settings page. */
@Composable
fun TvNotificationSettingsPage(viewModel: NotificationSettingsViewModel = koinViewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()

    val isModernEligible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
    val isMiIslandSupported = capabilityProvider.isSupportMiIsland
    val styleOptions = remember(isModernEligible, isMiIslandSupported) {
        buildList {
            add(NotificationStyle.STANDARD)
            if (isModernEligible) add(NotificationStyle.LIVE_ACTIVITY)
            if (isMiIslandSupported) add(NotificationStyle.MI_ISLAND)
        }
    }
    val activeStyle = uiState.currentStyle.takeIf { it in styleOptions } ?: NotificationStyle.STANDARD
    val isStyleSelectionEnabled = isModernEligible || isMiIslandSupported

    // Keep the current value selectable even when it is not one of the preset steps.
    val intervalOptions = (MiIslandIntervalOptions + uiState.miIslandBlockingInterval).distinct().sorted()

    TvSettingsPage(title = stringResource(R.string.notification_settings)) {
        header(context.getString(R.string.notification_style))
        choice(
            key = "style",
            title = context.getString(R.string.notification_style),
            icon = AppIcons.Palette,
            enabled = isStyleSelectionEnabled,
            description = if (isStyleSelectionEnabled) null else context.getString(R.string.notification_style_unsupported_desc),
            options = styleOptions.map { style ->
                TvChoiceOption(
                    when (style) {
                        NotificationStyle.STANDARD -> context.getString(R.string.notification_style_standard)
                        NotificationStyle.LIVE_ACTIVITY -> context.getString(R.string.notification_style_live_activity)
                        NotificationStyle.MI_ISLAND -> context.getString(R.string.notification_style_mi_island)
                    },
                )
            },
            selectedIndex = styleOptions.indexOf(activeStyle).coerceAtLeast(0),
            onSelect = { index -> viewModel.dispatch(NotificationSettingsAction.ChangeStyle(styleOptions[index])) },
        )
        if (activeStyle == NotificationStyle.MI_ISLAND) {
            switch(
                key = "mi_island_bypass",
                title = context.getString(R.string.lab_mi_island_bypass_restriction),
                summary = context.getString(R.string.lab_mi_island_bypass_restriction_desc),
                icon = AppIcons.Bypass,
                checked = uiState.miIslandBypassRestriction,
                onCheckedChange = { viewModel.dispatch(NotificationSettingsAction.ChangeMiIslandBypassRestriction(it)) },
            )
            if (uiState.miIslandBypassRestriction) {
                choice(
                    key = "mi_island_interval",
                    title = context.getString(R.string.lab_mi_island_countdown),
                    icon = AppIcons.StopWatch,
                    description = context.getString(R.string.lab_mi_island_countdown_desc),
                    options = intervalOptions.map { TvChoiceOption("$it ms") },
                    selectedIndex = intervalOptions.indexOf(uiState.miIslandBlockingInterval),
                    onSelect = { index ->
                        viewModel.dispatch(NotificationSettingsAction.ChangeMiIslandBlockingInterval(intervalOptions[index]))
                    },
                )
            }
            switch(
                key = "mi_island_glow",
                title = context.getString(R.string.lab_mi_island_outer_glow),
                summary = context.getString(R.string.lab_mi_island_outer_glow_desc),
                icon = AppIcons.Glow,
                checked = uiState.miIslandOuterGlow,
                onCheckedChange = { viewModel.dispatch(NotificationSettingsAction.ChangeMiIslandOuterGlow(it)) },
            )
        }

        header(context.getString(R.string.config_label_preferences))
        switch(
            key = "dialog_on_press",
            title = context.getString(R.string.show_dialog_when_pressing_notification),
            summary = context.getString(R.string.change_notification_touch_behavior),
            icon = AppIcons.Dialog,
            checked = uiState.showDialogOnPress,
            onCheckedChange = { viewModel.dispatch(NotificationSettingsAction.ChangeShowDialogOnPress(it)) },
        )
        val clearIndex = AutoClearSecondsOptions.indexOf(uiState.successAutoClearSeconds).coerceAtLeast(0)
        choice(
            key = "auto_clear",
            title = context.getString(R.string.installer_settings_auto_clear_success_notification),
            icon = AppIcons.Timer,
            description = if (AutoClearSecondsOptions[clearIndex] == 0) {
                context.getString(R.string.installer_settings_auto_clear_time_never_desc)
            } else {
                context.getString(R.string.installer_settings_auto_clear_time_seconds_format_desc, AutoClearSecondsOptions[clearIndex])
            },
            options = AutoClearSecondsOptions.map { seconds ->
                TvChoiceOption(
                    if (seconds == 0) {
                        context.getString(R.string.installer_settings_auto_clear_time_never)
                    } else {
                        context.getString(R.string.installer_settings_auto_clear_time_seconds_format, seconds)
                    },
                )
            },
            selectedIndex = clearIndex,
            onSelect = { index ->
                val seconds = AutoClearSecondsOptions[index]
                if (seconds != uiState.successAutoClearSeconds) {
                    viewModel.dispatch(NotificationSettingsAction.ChangeAutoClearSeconds(seconds))
                }
            },
        )
    }
}
