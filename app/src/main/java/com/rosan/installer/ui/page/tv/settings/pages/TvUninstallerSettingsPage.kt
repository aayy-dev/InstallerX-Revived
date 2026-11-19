// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.settings.pages

import android.content.Intent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.core.bitmask.hasFlag
import com.rosan.installer.domain.engine.model.install.UninstallFlags
import com.rosan.installer.ui.activity.UninstallerActivity
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.uninstaller.UninstallerSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.uninstaller.UninstallerSettingsEvent
import com.rosan.installer.ui.page.main.settings.preferred.uninstaller.UninstallerSettingsViewModel
import com.rosan.installer.ui.page.tv.settings.TvSettingsPage
import com.rosan.installer.util.toast
import org.koin.androidx.compose.koinViewModel

@Composable
fun TvUninstallerSettingsPage(viewModel: UninstallerSettingsViewModel = koinViewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UninstallerSettingsEvent.ShowMessage -> context.toast(event.resId)
            }
        }
    }

    val biometricAvailable = remember(context) {
        BiometricManager.from(context)
            .canAuthenticate(BIOMETRIC_WEAK or BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    }

    TvSettingsPage(title = stringResource(R.string.uninstaller_settings)) {
        info(
            key = "tip",
            title = context.getString(R.string.uninstall_authorizer_tip),
            icon = AppIcons.Tip,
        )

        header(context.getString(R.string.global))
        listOf(
            Triple(UninstallFlags.DELETE_KEEP_DATA, R.string.uninstall_keep_data to R.string.uninstall_keep_data_desc, AppIcons.Save),
            Triple(UninstallFlags.DELETE_ALL_USERS, R.string.uninstall_all_users to R.string.uninstall_all_users_desc, AppIcons.InstallForAllUsers),
            Triple(
                UninstallFlags.DELETE_SYSTEM_APP,
                R.string.uninstall_delete_system_app to R.string.uninstall_delete_system_app_desc,
                AppIcons.BugReport,
            ),
        ).forEach { (flag, labels, icon) ->
            switch(
                key = "flag_$flag",
                title = context.getString(labels.first),
                summary = context.getString(labels.second),
                icon = icon,
                checked = uiState.uninstallFlags.hasFlag(flag),
                onCheckedChange = { viewModel.dispatch(UninstallerSettingsAction.ToggleGlobalUninstallFlag(flag, it)) },
            )
        }
        if (biometricAvailable) {
            switch(
                key = "biometric",
                title = context.getString(R.string.uninstaller_settings_require_biometric_auth),
                summary = context.getString(R.string.uninstaller_settings_require_biometric_auth_desc),
                icon = AppIcons.BiometricAuth,
                checked = uiState.uninstallerRequireBiometricAuth,
                onCheckedChange = { viewModel.dispatch(UninstallerSettingsAction.ChangeBiometricAuth(it)) },
            )
        }

        header(context.getString(R.string.uninstall_call_uninstaller))
        textInput(
            key = "manual_uninstall",
            title = context.getString(R.string.uninstall_call_uninstaller_manually),
            value = "",
            placeholder = context.getString(R.string.uninstall_call_uninstaller_manually_desc),
            icon = AppIcons.Delete,
            description = context.getString(R.string.uninstall_enter_package_name),
            onValueChange = { input ->
                val packageName = input.trim()
                if (packageName.isNotEmpty()) {
                    context.startActivity(
                        Intent(context, UninstallerActivity::class.java).putExtra("package_name", packageName),
                    )
                }
            },
        )
    }
}
