// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.settings.pages

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.config.NetworkSourceMode
import com.rosan.installer.domain.settings.model.preferences.GithubUpdateChannel
import com.rosan.installer.domain.settings.model.preferences.HttpProfile
import com.rosan.installer.domain.settings.model.preferences.NetworkAppNameSource
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.network.NetworkSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.network.NetworkSettingsViewModel
import com.rosan.installer.ui.page.tv.components.TvAction
import com.rosan.installer.ui.page.tv.components.TvActionRow
import com.rosan.installer.ui.page.tv.components.TvConfirmScreen
import com.rosan.installer.ui.page.tv.settings.LocalTvSettingsNavigator
import com.rosan.installer.ui.page.tv.settings.TvChoiceOption
import com.rosan.installer.ui.page.tv.settings.TvSettingsPage
import org.koin.androidx.compose.koinViewModel

@Composable
fun TvNetworkPage(viewModel: NetworkSettingsViewModel = koinViewModel()) {
    val navigator = LocalTvSettingsNavigator.current
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var pendingNetworkSourceMode by rememberSaveable { mutableStateOf<NetworkSourceMode?>(null) }
    var exitAfterInternetDisable by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(exitAfterInternetDisable, uiState.allowInternetAccess) {
        if (exitAfterInternetDisable && !uiState.allowInternetAccess) {
            navigator.pop()
        }
    }

    pendingNetworkSourceMode?.let { mode ->
        BackHandler { pendingNetworkSourceMode = null }
        TvConfirmScreen(
            title = stringResource(R.string.network_source_mode_warning_title),
            description = stringResource(R.string.network_source_mode_warning_desc),
            actions = {
                TvActionRow(
                    actions = listOf(
                        TvAction(stringResource(R.string.confirm), onClick = {
                            pendingNetworkSourceMode = null
                            viewModel.dispatch(NetworkSettingsAction.ConfirmNetworkSourceMode(mode))
                        }),
                        TvAction(stringResource(R.string.cancel), onClick = { pendingNetworkSourceMode = null }, isDefault = true),
                    ),
                )
            },
        )
        return
    }

    val sourceModes = listOf(NetworkSourceMode.Cache, NetworkSourceMode.Automatic, NetworkSourceMode.StreamOnly)
    val sourceModeOptions = listOf(
        TvChoiceOption(stringResource(R.string.config_network_source_cache), stringResource(R.string.config_network_source_cache_desc)),
        TvChoiceOption(stringResource(R.string.config_network_source_smart), stringResource(R.string.config_network_source_smart_desc)),
        TvChoiceOption(
            stringResource(R.string.config_network_source_low_storage),
            stringResource(R.string.config_network_source_low_storage_desc),
        ),
    )
    val appNameSources = NetworkAppNameSource.entries
    val appNameOptions = listOf(
        TvChoiceOption(stringResource(R.string.network_app_name_server), stringResource(R.string.network_app_name_server_desc)),
        TvChoiceOption(stringResource(R.string.network_app_name_resources), stringResource(R.string.network_app_name_resources_desc)),
    )
    val httpProfiles = listOf(HttpProfile.ALLOW_SECURE, HttpProfile.ALLOW_LOCAL, HttpProfile.ALLOW_ALL)
    val httpProfileOptions = listOf(
        TvChoiceOption(stringResource(R.string.lab_http_profile_secure)),
        TvChoiceOption(stringResource(R.string.lab_http_profile_local)),
        TvChoiceOption(stringResource(R.string.lab_http_profile_all)),
    )
    val channels = listOf(GithubUpdateChannel.OFFICIAL, GithubUpdateChannel.PROXY_7ED, GithubUpdateChannel.CUSTOM)
    val channelOptions = listOf(
        TvChoiceOption(stringResource(R.string.lab_update_github_proxy_official)),
        TvChoiceOption(stringResource(R.string.lab_update_github_proxy_7ed)),
        TvChoiceOption(stringResource(R.string.lab_update_github_proxy_custom)),
    )

    TvSettingsPage(title = stringResource(R.string.network_settings)) {
        switch(
            key = "network_access",
            title = context.getString(R.string.network_access),
            icon = AppIcons.Network,
            checked = uiState.allowInternetAccess,
            onCheckedChange = { enabled ->
                if (!enabled) exitAfterInternetDisable = true
                viewModel.dispatch(NetworkSettingsAction.ChangeInternetAccess(enabled))
            },
        )
        if (!uiState.allowInternetAccess) return@TvSettingsPage

        header(context.getString(R.string.internet_access_enabled))
        choice(
            key = "source_mode",
            title = context.getString(R.string.config_network_source_mode),
            icon = AppIcons.NetworkSource,
            options = sourceModeOptions,
            selectedIndex = sourceModes.indexOf(uiState.networkSourceMode).coerceAtLeast(0),
            onSelect = { index ->
                val mode = sourceModes[index]
                if (mode != NetworkSourceMode.Cache && !uiState.networkSourceModeWarningAcknowledged) {
                    pendingNetworkSourceMode = mode
                } else {
                    viewModel.dispatch(NetworkSettingsAction.ChangeNetworkSourceMode(mode))
                }
            },
        )
        if (uiState.networkSourceMode == NetworkSourceMode.Automatic) {
            choice(
                key = "app_name_source",
                title = context.getString(R.string.network_app_name_source),
                icon = AppIcons.Info,
                options = appNameOptions,
                selectedIndex = appNameSources.indexOf(uiState.networkAppNameSource),
                onSelect = { index ->
                    appNameSources.getOrNull(index)?.let { viewModel.dispatch(NetworkSettingsAction.ChangeAppNameSource(it)) }
                },
            )
            switch(
                key = "load_app_icon",
                title = context.getString(R.string.network_load_app_icon),
                summary = context.getString(R.string.network_load_app_icon_desc),
                icon = AppIcons.LoadAppIcon,
                checked = uiState.networkLoadAppIcon,
                onCheckedChange = { viewModel.dispatch(NetworkSettingsAction.ChangeLoadAppIcon(it)) },
            )
        }
        choice(
            key = "http_profile",
            title = context.getString(R.string.lab_http_profile),
            icon = AppIcons.Bypass,
            options = httpProfileOptions,
            selectedIndex = httpProfiles.indexOf(uiState.httpProfile).coerceAtLeast(0),
            onSelect = { index -> viewModel.dispatch(NetworkSettingsAction.ChangeHttpProfile(httpProfiles[index])) },
        )
        choice(
            key = "github_channel",
            title = context.getString(R.string.lab_update_github_proxy),
            icon = AppIcons.UpdateChannel,
            options = channelOptions,
            selectedIndex = channels.indexOf(uiState.githubUpdateChannel).coerceAtLeast(0),
            onSelect = { index -> viewModel.dispatch(NetworkSettingsAction.ChangeGithubUpdateChannel(channels[index])) },
        )
        if (uiState.githubUpdateChannel == GithubUpdateChannel.CUSTOM) {
            textInput(
                key = "custom_proxy_url",
                title = context.getString(R.string.lab_update_github_proxy_url),
                value = uiState.customGithubProxyUrl,
                placeholder = context.getString(R.string.lab_update_github_proxy_custom),
                icon = AppIcons.Edit,
                onValueChange = { url ->
                    val trimmed = url.trim()
                    viewModel.dispatch(NetworkSettingsAction.ChangeCustomGithubProxyUrl(trimmed))
                    // An empty custom proxy falls back to the official channel, as on the phone.
                    if (trimmed.isEmpty()) {
                        viewModel.dispatch(NetworkSettingsAction.ChangeGithubUpdateChannel(GithubUpdateChannel.OFFICIAL))
                    }
                },
            )
        }
    }
}
