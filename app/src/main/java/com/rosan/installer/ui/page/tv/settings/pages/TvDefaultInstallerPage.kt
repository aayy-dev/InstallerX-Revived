// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.settings.pages

import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.home.HomePageViewAction
import com.rosan.installer.ui.page.main.settings.home.HomePageViewEvent
import com.rosan.installer.ui.page.main.settings.home.HomePageViewModel
import com.rosan.installer.ui.page.main.settings.home.delayDefaultInstallerProgressIfNeeded
import com.rosan.installer.ui.page.tv.components.TvAction
import com.rosan.installer.ui.page.tv.components.TvActionRow
import com.rosan.installer.ui.page.tv.components.TvConfirmScreen
import com.rosan.installer.ui.page.tv.components.TvProgressBar
import com.rosan.installer.ui.page.tv.settings.TvSettingsPage
import com.rosan.installer.util.help
import org.koin.androidx.compose.koinViewModel

@Composable
fun TvDefaultInstallerPage(viewModel: HomePageViewModel = koinViewModel()) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    var errorInfo by remember { mutableStateOf<HomePageViewEvent.ShowDefaultInstallerErrorDetail?>(null) }
    var showProgress by remember { mutableStateOf(false) }
    var progressStartedAt by remember { mutableLongStateOf(0L) }
    var progressTextResId by remember { mutableStateOf(R.string.locking_default_installer) }

    fun dispatchSetDefaultInstaller(lock: Boolean) {
        progressStartedAt = SystemClock.elapsedRealtime()
        progressTextResId = if (lock) R.string.locking_default_installer else R.string.unlocking_default_installer
        showProgress = true
        viewModel.dispatch(HomePageViewAction.SetDefaultInstaller(lock))
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            if (showProgress) {
                delayDefaultInstallerProgressIfNeeded(progressStartedAt)
                showProgress = false
            }
            when (event) {
                is HomePageViewEvent.ShowDefaultInstallerResult ->
                    Toast.makeText(context, context.getString(event.messageResId), Toast.LENGTH_SHORT).show()

                is HomePageViewEvent.ShowDefaultInstallerErrorDetail -> errorInfo = event
            }
        }
    }

    if (showProgress) {
        // Stay on the page until the result arrives; leaving would cancel the event collector and
        // drop the result or error (with its Retry).
        BackHandler { }
        TvConfirmScreen(
            title = stringResource(progressTextResId),
            extra = { TvProgressBar(progress = -1f, modifier = Modifier.widthIn(max = 520.dp)) },
            actions = {},
        )
        return
    }

    errorInfo?.let { info ->
        val dismiss = { errorInfo = null }
        BackHandler(onBack = dismiss)
        TvConfirmScreen(
            title = stringResource(info.titleResId),
            description = listOfNotNull(info.exception.help(), info.exception.message).joinToString("\n\n"),
            actions = {
                TvActionRow(
                    actions = listOf(
                        TvAction(stringResource(R.string.retry), onClick = {
                            errorInfo = null
                            val action = info.retryAction
                            if (action is HomePageViewAction.SetDefaultInstaller) {
                                dispatchSetDefaultInstaller(action.lock)
                            } else {
                                viewModel.dispatch(action)
                            }
                        }),
                        TvAction(stringResource(R.string.close), onClick = dismiss, isDefault = true),
                    ),
                )
            },
        )
        return
    }

    val actionsEnabled = uiState.globalAuthorizer != Authorizer.None && !uiState.userSetLSPosedActive
    val tip = when {
        uiState.isSystemApp -> null to stringResource(R.string.config_authorizer_none_system_app_tips)
        uiState.globalAuthorizer == Authorizer.None -> null to stringResource(R.string.config_authorizer_none_tips)
        else -> stringResource(R.string.priv_page_what_is_this_title) to stringResource(R.string.working_status_default_installer_tip)
    }

    TvSettingsPage(title = stringResource(R.string.default_installer)) {
        info(
            key = "tip",
            title = tip.first ?: context.getString(R.string.default_installer),
            summary = tip.second,
            icon = AppIcons.Tip,
        )

        header(context.getString(R.string.basic))
        item(
            key = "lock",
            title = context.getString(R.string.lock_default_installer),
            summary = context.getString(R.string.lock_default_installer_desc),
            icon = AppIcons.LockDefault,
            enabled = actionsEnabled,
            onClick = { dispatchSetDefaultInstaller(true) },
        )
        item(
            key = "unlock",
            title = context.getString(R.string.unlock_default_installer),
            summary = context.getString(R.string.unlock_default_installer_desc),
            icon = AppIcons.UnlockDefault,
            enabled = actionsEnabled,
            onClick = { dispatchSetDefaultInstaller(false) },
        )

        if (!uiState.isSystemApp) {
            header(context.getString(R.string.advanced))
            switch(
                key = "lsposed",
                title = context.getString(R.string.setting_lsposed_module_title),
                summary = context.getString(R.string.setting_lsposed_module_desc),
                icon = AppIcons.LSPosed,
                checked = uiState.userSetLSPosedActive,
                onCheckedChange = { viewModel.dispatch(HomePageViewAction.ChangeUserSetLSPosedActive(it)) },
            )
            item(
                key = "inxlocker",
                title = context.getString(R.string.home_lsp_inxlocker_title),
                summary = context.getString(R.string.home_label_lsp_link),
                icon = AppIcons.Info,
                // TVs may have no browser installed; opening the link is best effort.
                onClick = { runCatching { uriHandler.openUri("https://github.com/Chimioo/InxLocker") } },
            )
        }
    }
}
