// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.settings.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.installer.dialog.DialogSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.installer.dialog.DialogSettingsViewModel
import com.rosan.installer.ui.page.tv.settings.TvChoiceOption
import com.rosan.installer.ui.page.tv.settings.TvSettingsPage
import org.koin.androidx.compose.koinViewModel

/** Install dialog behavior, matching the phone Dialog Settings page. */
@Composable
fun TvDialogSettingsPage(viewModel: DialogSettingsViewModel = koinViewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    TvSettingsPage(title = stringResource(R.string.dialog_settings)) {
        header(context.getString(R.string.installer_settings_dialog_mode_options))
        choice(
            key = "comparison_display",
            title = context.getString(R.string.install_comparison_display_behavior),
            icon = AppIcons.SingleLineSettingIcon,
            options = listOf(
                TvChoiceOption(
                    label = context.getString(R.string.install_comparison_show_all),
                    summary = context.getString(R.string.install_comparison_show_all_desc),
                ),
                TvChoiceOption(
                    label = context.getString(R.string.install_comparison_show_differences_only),
                    summary = context.getString(R.string.install_comparison_show_differences_only_desc),
                ),
            ),
            selectedIndex = if (uiState.hideIdenticalComparisons) 1 else 0,
            onSelect = { viewModel.dispatch(DialogSettingsAction.ChangeHideIdenticalComparisons(it == 1)) },
        )
        switch(
            key = "version_single_line",
            title = context.getString(R.string.version_compare_in_single_line),
            summary = context.getString(R.string.version_compare_in_single_line_desc),
            icon = AppIcons.SingleLineSettingIcon,
            checked = uiState.versionCompareInSingleLine,
            onCheckedChange = { viewModel.dispatch(DialogSettingsAction.ChangeVersionCompareInSingleLine(it)) },
        )
        switch(
            key = "sdk_multi_line",
            title = context.getString(R.string.sdk_compare_in_multi_line),
            summary = context.getString(R.string.sdk_compare_in_multi_line_desc),
            icon = AppIcons.MultiLineSettingIcon,
            checked = uiState.sdkCompareInMultiLine,
            onCheckedChange = { viewModel.dispatch(DialogSettingsAction.ChangeSdkCompareInMultiLine(it)) },
        )
        switch(
            key = "extended_menu",
            title = context.getString(R.string.show_dialog_install_extended_menu),
            summary = context.getString(R.string.show_dialog_install_extended_menu_desc),
            icon = AppIcons.MenuOpen,
            checked = uiState.showDialogInstallExtendedMenu,
            onCheckedChange = { viewModel.dispatch(DialogSettingsAction.ChangeShowDialogInstallExtendedMenu(it)) },
        )
        switch(
            key = "smart_suggestion",
            title = context.getString(R.string.show_intelligent_suggestion),
            summary = context.getString(R.string.show_intelligent_suggestion_desc),
            icon = AppIcons.Suggestion,
            checked = uiState.showSmartSuggestion,
            onCheckedChange = { viewModel.dispatch(DialogSettingsAction.ChangeShowSuggestion(it)) },
        )
        switch(
            key = "expand_temporary_settings",
            title = context.getString(R.string.expand_temporary_settings_by_default),
            summary = context.getString(R.string.expand_temporary_settings_by_default_desc),
            icon = AppIcons.MultiLineSettingIcon,
            checked = uiState.expandTemporarySettingsByDefault,
            onCheckedChange = { viewModel.dispatch(DialogSettingsAction.ChangeExpandTemporarySettingsByDefault(it)) },
        )
        switch(
            key = "disable_notification_on_dismiss",
            title = context.getString(R.string.disable_notification_on_dismiss),
            summary = context.getString(R.string.close_notification_immediately_on_dialog_dismiss),
            icon = AppIcons.NotificationDisabled,
            checked = uiState.disableNotificationForDialogInstall,
            onCheckedChange = { viewModel.dispatch(DialogSettingsAction.ChangeShowDisableNotification(it)) },
        )

        header(context.getString(R.string.installer_settings_dialog_automation_options))
        switch(
            key = "auto_background_install",
            title = context.getString(R.string.auto_background_install),
            summary = context.getString(R.string.auto_background_install_desc),
            icon = AppIcons.PressInstallBackground,
            checked = uiState.autoSilentInstall,
            onCheckedChange = { viewModel.dispatch(DialogSettingsAction.ChangeAutoSilentInstall(it)) },
        )
        switch(
            key = "long_click_background_install",
            title = context.getString(R.string.long_click_background_install),
            summary = context.getString(R.string.long_click_background_install_desc),
            icon = AppIcons.PressInstallBackground,
            checked = uiState.longClickBackgroundInstall,
            onCheckedChange = { viewModel.dispatch(DialogSettingsAction.ChangeLongClickBackgroundInstall(it)) },
        )

        header(context.getString(R.string.extras))
        switch(
            key = "tap_icon_to_share",
            title = context.getString(R.string.lab_tap_icon_to_share),
            summary = context.getString(R.string.lab_tap_icon_to_share_desc),
            icon = AppIcons.Share,
            checked = uiState.tapIconToShare,
            onCheckedChange = { viewModel.dispatch(DialogSettingsAction.ChangeTapIconToShare(it)) },
        )
    }
}
