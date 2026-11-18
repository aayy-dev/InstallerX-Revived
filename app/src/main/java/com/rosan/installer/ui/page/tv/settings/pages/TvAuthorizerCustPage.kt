// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.settings.pages

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.installer.authorizer.AuthorizerCustAction
import com.rosan.installer.ui.page.main.settings.preferred.installer.authorizer.AuthorizerCustViewModel
import com.rosan.installer.ui.page.tv.settings.TvChoiceOption
import com.rosan.installer.ui.page.tv.settings.TvSettingsPage
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private val CloseSessionCountDownOptions = (1..10).toList()

/** Authorizer tweaks, matching the phone Authorizer Customization page. */
@Composable
fun TvAuthorizerCustPage(viewModel: AuthorizerCustViewModel = koinViewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()
    val isSystemApp = capabilityProvider.isSystemApp

    TvSettingsPage(title = stringResource(R.string.authorizer_customization)) {
        header(
            context.getString(if (isSystemApp) R.string.working_status_system_installer else R.string.config_authorizer_none),
        )
        if (isSystemApp) {
            switch(
                key = "always_root_in_system",
                title = context.getString(R.string.config_always_use_root_in_system),
                summary = context.getString(R.string.config_always_use_root_in_system_desc),
                icon = AppIcons.FlashPreferRoot,
                checked = uiState.alwaysUseRootInSystem,
                onCheckedChange = { viewModel.dispatch(AuthorizerCustAction.ChangeAlwaysUseRootInSystem(it)) },
            )
        }
        if (!isSystemApp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            switch(
                key = "install_without_user_action",
                title = context.getString(R.string.lab_install_without_user_action),
                summary = context.getString(R.string.lab_install_without_user_action_desc),
                icon = AppIcons.InstallWithOutAction,
                checked = uiState.allowInstallWithoutUserAction,
                onCheckedChange = { viewModel.dispatch(AuthorizerCustAction.ChangeAllowInstallWithoutUserAction(it)) },
            )
        }

        if (!isSystemApp) {
            header(context.getString(R.string.extras))
            choice(
                key = "close_session_countdown",
                title = context.getString(R.string.close_session_countdown),
                icon = AppIcons.Working,
                description = context.getString(R.string.close_session_countdown_desc),
                options = CloseSessionCountDownOptions.map { TvChoiceOption(it.toString()) },
                selectedIndex = CloseSessionCountDownOptions.indexOf(uiState.closeSessionCountDown),
                onSelect = { index ->
                    viewModel.dispatch(AuthorizerCustAction.ChangeCloseSessionCountDown(CloseSessionCountDownOptions[index]))
                },
            )
        }
    }
}
