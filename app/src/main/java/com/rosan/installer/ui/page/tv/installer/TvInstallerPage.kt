// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.installer

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.ui.page.main.installer.InstallerStage
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.LocalDialogButtonsRenderer
import com.rosan.installer.ui.page.main.installer.dialog.dialogGenerateParams
import com.rosan.installer.ui.page.main.installer.dialog.dismissInstallerDialog
import com.rosan.installer.ui.page.main.widget.util.InstallerEventCollector
import com.rosan.installer.ui.page.tv.components.TvAction
import com.rosan.installer.ui.page.tv.components.TvActionRow
import com.rosan.installer.ui.theme.InstallerMaterialExpressiveTheme
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.LocalInstallerColorScheme
import com.rosan.installer.ui.theme.material.dynamicColorScheme
import com.rosan.installer.ui.theme.tv.InstallerTvTheme
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Full-screen, remote-driven installer for TV devices, laid out like Google TV's own store and
 * system screens. Each stage's actions come from the same per-stage button definitions as the phone
 * dialog (rendered here as TV pill buttons), so install, cancel and safety defaults stay consistent.
 */
@Composable
fun TvInstallerPage(session: InstallerSessionRepository, viewModel: InstallerViewModel = koinViewModel { parametersOf(session) }) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stage = uiState.stage

    val globalColorScheme = InstallerTheme.colorScheme
    val paletteStyle = InstallerTheme.paletteStyle
    val colorSpec = InstallerTheme.colorSpec
    val temporarySeedColor = uiState.seedColor
    val activeColorScheme = remember(temporarySeedColor, globalColorScheme, paletteStyle) {
        temporarySeedColor?.let {
            dynamicColorScheme(keyColor = it, isDark = true, style = paletteStyle, colorSpec = colorSpec)
        } ?: globalColorScheme
    }

    LaunchedEffect(session.id) {
        viewModel.dispatch(InstallerViewAction.CollectSession(session))
    }

    InstallerEventCollector(viewModel)

    if (stage is InstallerStage.Ready) return

    CompositionLocalProvider(LocalInstallerColorScheme provides activeColorScheme) {
        InstallerMaterialExpressiveTheme(colorScheme = activeColorScheme, darkTheme = true, compatStatusBarColor = false) {
            InstallerTvTheme(colorScheme = activeColorScheme, isDark = true) {
                BackHandler { dismissTvInstaller(viewModel) }
                val params = dialogGenerateParams(viewModel)
                TvInstallerStage(viewModel = viewModel, stage = stage, params = params)
            }
        }
    }
}

@Composable
private fun TvInstallerStage(viewModel: InstallerViewModel, stage: InstallerStage, params: DialogParams) {
    val actions: @Composable () -> Unit = { StageActions(params) }
    when (stage) {
        is InstallerStage.Resolving -> TvLoadingScreen(stringResource(R.string.installer_resolving), null, -1f, actions)

        is InstallerStage.Analysing -> TvLoadingScreen(stringResource(R.string.installer_analysing), null, -1f, actions)

        is InstallerStage.Preparing -> TvLoadingScreen(
            title = stringResource(R.string.installer_preparing),
            description = stringResource(R.string.installer_preparing_desc),
            progress = stage.progress,
            actions = actions,
        )

        is InstallerStage.ResolveFailed -> TvErrorScreen(viewModel, stringResource(R.string.installer_resolve_failed), actions)

        is InstallerStage.AnalyseFailed -> TvErrorScreen(viewModel, stringResource(R.string.installer_analyse_failed), actions)

        is InstallerStage.InstallPrepare,
        is InstallerStage.Installing,
        is InstallerStage.InstallSuccess,
        is InstallerStage.InstallFailed,
        -> TvAppHeroScreen(viewModel = viewModel, stage = stage, actions = actions)

        is InstallerStage.InstallChoice -> TvInstallChoiceScreen(viewModel)

        // Option lists open on their first row; the buttons stay one Left/Back away.
        is InstallerStage.InstallExtendedMenu -> TvInstallOptionsScreen(viewModel) { StageActions(params, requestInitialFocus = false) }

        is InstallerStage.InstallExtendedSubMenu -> TvPermissionListScreen(viewModel) { StageActions(params, requestInitialFocus = false) }

        is InstallerStage.InstallingModule -> TvModuleInstallScreen(viewModel, stage)

        is InstallerStage.InstallConfirm -> TvSessionConfirmScreen(viewModel, stage, actions)

        is InstallerStage.InstallWaitingUnknownSource -> TvUnknownSourceScreen(viewModel, actions)

        is InstallerStage.InstallCompleted -> TvInstallCompletedScreen(stage, actions)

        is InstallerStage.UninstallReady -> TvUninstallScreen(viewModel, actions)

        is InstallerStage.Uninstalling -> TvLoadingScreen(stringResource(R.string.uninstall_progress_text), null, -1f, actions)

        is InstallerStage.UninstallSuccess -> TvUninstallResultScreen(viewModel, success = true, actions = actions)

        is InstallerStage.UninstallFailed,
        is InstallerStage.UninstallResolveFailed,
        -> TvUninstallResultScreen(viewModel, success = false, actions = actions)

        is InstallerStage.UnarchiveReady -> TvUnarchiveScreen(stage, actions)

        is InstallerStage.Unarchiving -> TvLoadingScreen(stringResource(R.string.unarchive_restoring), null, -1f, actions)

        // Rare stages keep the phone content inside a TV panel until they get dedicated layouts.
        else -> TvGenericStageScreen(params, actions)
    }
}

/** Renders the stage's buttons (from the shared per-stage definitions) as a TV action row. */
@Composable
private fun StageActions(params: DialogParams, requestInitialFocus: Boolean = true) {
    val buttons = params.buttons.content ?: return
    CompositionLocalProvider(
        LocalDialogButtonsRenderer provides { list ->
            TvActionRow(
                actions = list.map {
                    TvAction(
                        text = it.text,
                        onClick = it.onClick,
                        enabled = it.enabled,
                        isDefault = it.isDefaultFocus,
                        onLongClick = it.onLongClick,
                    )
                },
                requestInitialFocus = requestInitialFocus,
            )
        },
    ) { buttons() }
}

/**
 * Back on TV closes the installer instead of parking it as a notification, which TV launchers may
 * not surface. In-progress stages keep the phone behavior so a running install is not cancelled.
 * The option menus, which the phone dialog keeps open on outside taps, step back like their own
 * Next/Previous buttons (options are applied as they change), since TV has no other way back.
 */
private fun dismissTvInstaller(viewModel: InstallerViewModel) {
    val uiState = viewModel.uiState.value
    when (uiState.stage) {
        is InstallerStage.InstallExtendedMenu -> return viewModel.dispatch(InstallerViewAction.InstallPrepare)
        is InstallerStage.InstallExtendedSubMenu -> return viewModel.dispatch(InstallerViewAction.InstallExtendedMenu)
        else -> Unit
    }
    if (!uiState.isDismissible) return
    when (uiState.stage) {
        is InstallerStage.Preparing,
        is InstallerStage.Installing,
        is InstallerStage.InstallWaitingUnknownSource,
        -> dismissInstallerDialog(viewModel)

        else -> viewModel.dispatch(InstallerViewAction.Close)
    }
}
