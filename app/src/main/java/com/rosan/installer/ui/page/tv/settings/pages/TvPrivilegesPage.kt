// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.settings.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.RadioButton
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.preferences.RootMode
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.home.HomePageViewAction
import com.rosan.installer.ui.page.main.settings.home.HomePageViewModel
import com.rosan.installer.ui.page.main.widget.util.OnLifecycleEvent
import com.rosan.installer.ui.page.tv.settings.TvSettingsPage
import org.koin.androidx.compose.koinViewModel

@Composable
fun TvPrivilegesPage(viewModel: HomePageViewModel = koinViewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val shizukuIcon = ImageVector.vectorResource(R.drawable.ic_shizuku)

    OnLifecycleEvent(event = Lifecycle.Event.ON_RESUME) {
        viewModel.dispatch(HomePageViewAction.RefreshActivateStatus)
    }

    val noneTitle = stringResource(
        if (uiState.isSystemApp) R.string.working_status_system_installer else R.string.config_authorizer_none,
    )
    val noneSummary = when {
        !uiState.isSessionInstallSupported -> stringResource(R.string.unavailable)
        uiState.isSystemApp -> stringResource(R.string.working_status_system_installer_desc)
        else -> stringResource(R.string.working_status_none_authorizer_desc)
    }
    val rootSummary = if (uiState.rootMode != RootMode.None) {
        stringResource(R.string.available) + " (${uiState.rootMode.name})"
    } else {
        stringResource(R.string.unavailable)
    }
    val shizukuSummary = when {
        uiState.shizukuAuthorized -> stringResource(R.string.activate) + " (${uiState.shizukuMode.desc})"
        uiState.shizukuAvailable -> stringResource(R.string.shizuku_not_authorized)
        else -> stringResource(R.string.shizuku_not_available)
    }
    val dhizukuSummary = when {
        uiState.dhizukuAuthorized -> stringResource(R.string.activate)
        uiState.dhizukuAvailable -> stringResource(R.string.dhizuku_not_authorized)
        else -> stringResource(R.string.dhizuku_not_available)
    }
    val customizeLabel = stringResource(R.string.config_authorizer_customize)
    val customizeCommand = uiState.customizeAuthorizer
    val editLabel = stringResource(R.string.edit)

    TvSettingsPage(
        title = stringResource(R.string.home_stat_authorizers),
        initialFocusKey = when (uiState.globalAuthorizer) {
            Authorizer.Root -> "root"
            Authorizer.Shizuku -> "shizuku"
            Authorizer.Dhizuku -> "dhizuku"
            Authorizer.Customize -> "customize"
            else -> "none"
        },
    ) {
        fun authorizerRow(
            key: String,
            authorizer: Authorizer,
            title: String,
            summary: String,
            icon: ImageVector,
            enabled: Boolean = true,
        ) {
            val selected = uiState.globalAuthorizer == authorizer
            item(
                key = key,
                title = title,
                summary = summary,
                icon = icon,
                enabled = enabled,
                trailing = { RadioButton(selected = selected, onClick = null) },
                onClick = { viewModel.dispatch(HomePageViewAction.ChangeAuthorizer(authorizer)) },
            )
        }

        authorizerRow("none", Authorizer.None, noneTitle, noneSummary, AppIcons.None, enabled = uiState.isSessionInstallSupported)
        authorizerRow("root", Authorizer.Root, context.getString(R.string.config_authorizer_root), rootSummary, AppIcons.Root)
        authorizerRow("shizuku", Authorizer.Shizuku, context.getString(R.string.config_authorizer_shizuku), shizukuSummary, shizukuIcon)
        authorizerRow(
            "dhizuku",
            Authorizer.Dhizuku,
            context.getString(R.string.config_authorizer_dhizuku),
            dhizukuSummary,
            AppIcons.InstallAllowRestrictedPermissions,
        )
        // A custom command can only be selected once one is set; the row below edits it.
        authorizerRow(
            "customize",
            Authorizer.Customize,
            customizeLabel,
            customizeCommand.ifBlank { customizeLabel },
            AppIcons.Terminal,
            enabled = customizeCommand.isNotBlank(),
        )
        textInput(
            key = "customize_command",
            title = editLabel,
            value = customizeCommand,
            icon = AppIcons.Edit,
            onValueChange = { input ->
                val command = input.trim()
                if (command.isNotBlank()) {
                    val selectOnSave = customizeCommand.isBlank() && uiState.globalAuthorizer != Authorizer.Customize
                    viewModel.dispatch(
                        if (selectOnSave) {
                            HomePageViewAction.EnableCustomizeAuthorizer(command)
                        } else {
                            HomePageViewAction.ChangeCustomizeAuthorizer(command)
                        },
                    )
                }
            },
        )

        info(
            key = "what_is_this",
            title = context.getString(R.string.priv_page_what_is_this_title),
            summary = context.getString(R.string.priv_page_what_is_this_desc),
            icon = AppIcons.Info,
        )
        info(
            key = "notice",
            title = context.getString(R.string.priv_page_notice_title),
            summary = context.getString(R.string.priv_page_notice_desc),
            icon = AppIcons.Tip,
        )
    }
}
