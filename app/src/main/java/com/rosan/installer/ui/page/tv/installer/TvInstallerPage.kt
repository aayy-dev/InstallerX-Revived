// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.installer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.domain.session.repository.InstallerSessionRepository
import com.rosan.installer.ui.page.main.installer.InstallerStage
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.dialog.LocalDialogButtonsRenderer
import com.rosan.installer.ui.page.main.installer.dialog.dialogGenerateParams
import com.rosan.installer.ui.page.main.installer.dialog.dismissInstallerDialog
import com.rosan.installer.ui.page.main.installer.dialog.inner.ModuleInstallSheetContent
import com.rosan.installer.ui.page.main.widget.util.InstallerEventCollector
import com.rosan.installer.ui.page.tv.TvFocusEnvironment
import com.rosan.installer.ui.page.tv.TvSafeArea
import com.rosan.installer.ui.theme.InstallerMaterialExpressiveTheme
import com.rosan.installer.ui.theme.InstallerTheme
import com.rosan.installer.ui.theme.LocalInstallerColorScheme
import com.rosan.installer.ui.theme.material.dynamicColorScheme
import com.rosan.installer.ui.theme.tv.InstallerTvTheme
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Full-screen, remote-driven installer for TV devices. Stage content and actions come from the same
 * [dialogGenerateParams] as the Material 3 dialog; only the layout and the buttons differ.
 */
@Composable
fun TvInstallerPage(session: InstallerSessionRepository, viewModel: InstallerViewModel = koinViewModel { parametersOf(session) }) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stage = uiState.stage

    val globalColorScheme = InstallerTheme.colorScheme
    val isDark = InstallerTheme.isDark
    val paletteStyle = InstallerTheme.paletteStyle
    val colorSpec = InstallerTheme.colorSpec
    val temporarySeedColor = uiState.seedColor
    val activeColorScheme = remember(temporarySeedColor, globalColorScheme, isDark, paletteStyle) {
        temporarySeedColor?.let {
            dynamicColorScheme(
                keyColor = it,
                isDark = isDark,
                style = paletteStyle,
                colorSpec = colorSpec,
            )
        } ?: globalColorScheme
    }

    LaunchedEffect(session.id) {
        viewModel.dispatch(InstallerViewAction.CollectSession(session))
    }

    InstallerEventCollector(viewModel)

    if (stage is InstallerStage.Ready) return

    CompositionLocalProvider(LocalInstallerColorScheme provides activeColorScheme) {
        InstallerMaterialExpressiveTheme(
            colorScheme = activeColorScheme,
            darkTheme = isDark,
            compatStatusBarColor = false,
        ) {
            InstallerTvTheme(colorScheme = activeColorScheme, isDark = isDark) {
                BackHandler { dismissTvInstaller(viewModel) }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    TvFocusEnvironment {
                        if (stage is InstallerStage.InstallingModule) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(TvSafeArea),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(modifier = Modifier.widthIn(max = 760.dp)) {
                                    ModuleInstallSheetContent(
                                        rootMode = uiState.rootMode,
                                        outputLines = stage.output,
                                        isFinished = stage.isFinished,
                                        onReboot = { viewModel.dispatch(InstallerViewAction.Reboot("")) },
                                        onSoftReboot = { viewModel.dispatch(InstallerViewAction.Reboot("ksud_soft_reboot")) },
                                        onClose = { viewModel.dispatch(InstallerViewAction.Close) },
                                        colorScheme = activeColorScheme,
                                    )
                                }
                            }
                        } else {
                            TvStageLayout(params = dialogGenerateParams(viewModel))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Back on TV closes the installer instead of parking it as a notification, which TV launchers may
 * not surface. In-progress stages keep the phone behavior so a running install is not cancelled.
 */
private fun dismissTvInstaller(viewModel: InstallerViewModel) {
    val uiState = viewModel.uiState.value
    if (!uiState.isDismissible) return
    when (uiState.stage) {
        is InstallerStage.Preparing,
        is InstallerStage.Installing,
        is InstallerStage.InstallWaitingUnknownSource,
        -> dismissInstallerDialog(viewModel)

        else -> viewModel.dispatch(InstallerViewAction.Close)
    }
}

@Composable
private fun TvStageLayout(params: DialogParams) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(TvSafeArea),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Identity pane: rendered larger so the app icon and name read from across the room.
        Column(
            modifier = Modifier.weight(0.42f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density * IdentityScale, density.fontScale),
                LocalContentColor provides AlertDialogDefaults.titleContentColor,
            ) {
                params.icon.content?.let { icon ->
                    CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.iconContentColor) {
                        Box(modifier = Modifier.padding(bottom = 12.dp)) { icon() }
                    }
                }
                params.title.content?.let { title ->
                    ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                        Box(modifier = Modifier.padding(bottom = 8.dp)) { title() }
                    }
                }
                params.subtitle.content?.let { subtitle ->
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) { subtitle() }
                }
            }
        }

        Spacer(modifier = Modifier.width(48.dp))

        // Detail and action pane: content replaces text, matching the phone dialog.
        Column(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            val body = params.content.content ?: params.text.content
            if (body != null) {
                // Some reused phone components hide their indication; outline the pane instead.
                var bodyHasFocus by remember { mutableStateOf(false) }
                val focusOutline = if (bodyHasFocus) MaterialTheme.colorScheme.onSurface else Color.Transparent
                CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.textContentColor) {
                    ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth()
                                .onFocusChanged { bodyHasFocus = it.hasFocus }
                                .border(3.dp, focusOutline, BodyShape)
                                .padding(8.dp),
                        ) { body() }
                    }
                }
            }
            params.buttons.content?.let { buttons ->
                CompositionLocalProvider(
                    LocalDialogButtonsRenderer provides { list -> TvDialogButtons(list, Modifier.padding(top = 24.dp)) },
                ) { buttons() }
            }
        }
    }
}

private const val IdentityScale = 1.3f
private val BodyShape = RoundedCornerShape(20.dp)
