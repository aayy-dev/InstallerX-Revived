// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.settings.pages

import android.content.ClipData
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.history.model.InstallMethod
import com.rosan.installer.domain.history.model.OperationHistoryModel
import com.rosan.installer.domain.history.model.OperationStatus
import com.rosan.installer.domain.history.model.OperationType
import com.rosan.installer.domain.history.model.VersionChange
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.history.HistoryViewAction
import com.rosan.installer.ui.page.main.settings.history.HistoryViewModel
import com.rosan.installer.ui.page.main.settings.history.formatHistoryTime
import com.rosan.installer.ui.page.main.settings.history.historyAuthorizerText
import com.rosan.installer.ui.page.main.settings.history.labelRes
import com.rosan.installer.ui.page.tv.components.TvAction
import com.rosan.installer.ui.page.tv.components.TvActionRow
import com.rosan.installer.ui.page.tv.components.TvConfirmScreen
import com.rosan.installer.ui.page.tv.settings.TvPreview
import com.rosan.installer.ui.page.tv.settings.TvSettingsPage
import com.rosan.installer.util.toast
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun TvHistoryPage(viewModel: HistoryViewModel = koinViewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var selectedRecord by remember { mutableStateOf<OperationHistoryModel?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showDisableConfirm by remember { mutableStateOf(false) }

    selectedRecord?.let { record ->
        BackHandler { selectedRecord = null }
        TvHistoryRecordPage(record = record, isSystemApp = state.isSystemApp)
        return
    }

    if (showClearConfirm) {
        val dismiss = { showClearConfirm = false }
        BackHandler(onBack = dismiss)
        TvConfirmScreen(
            title = stringResource(R.string.history_clear_confirm_title),
            description = stringResource(R.string.history_clear_confirm_desc),
            actions = {
                TvActionRow(
                    actions = listOf(
                        TvAction(stringResource(R.string.clear), onClick = {
                            showClearConfirm = false
                            viewModel.dispatch(HistoryViewAction.ClearHistory)
                        }),
                        TvAction(stringResource(R.string.cancel), onClick = dismiss, isDefault = true),
                    ),
                )
            },
        )
        return
    }

    if (showDisableConfirm) {
        val dismiss = { showDisableConfirm = false }
        BackHandler(onBack = dismiss)
        TvConfirmScreen(
            title = stringResource(R.string.history_disable_confirm_title),
            description = stringResource(R.string.history_disable_confirm_desc),
            actions = {
                TvActionRow(
                    actions = listOf(
                        TvAction(stringResource(R.string.history_disable_and_clear), onClick = {
                            showDisableConfirm = false
                            viewModel.dispatch(HistoryViewAction.SetHistoryEnabled(enabled = false, clearHistory = true))
                        }),
                        TvAction(stringResource(R.string.history_disable_and_keep), onClick = {
                            showDisableConfirm = false
                            viewModel.dispatch(HistoryViewAction.SetHistoryEnabled(enabled = false))
                        }),
                        TvAction(stringResource(R.string.cancel), onClick = dismiss, isDefault = true),
                    ),
                )
            },
        )
        return
    }

    TvSettingsPage(title = stringResource(R.string.history)) {
        header(context.getString(R.string.history_recording))
        switch(
            key = "enabled",
            title = context.getString(R.string.history_enable),
            icon = AppIcons.History,
            checked = state.isHistoryEnabled,
            onCheckedChange = { enabled ->
                if (enabled) {
                    viewModel.dispatch(HistoryViewAction.SetHistoryEnabled(enabled = true))
                } else {
                    showDisableConfirm = true
                }
            },
        )
        switch(
            key = "indicators",
            title = context.getString(R.string.history_show_indicators),
            icon = AppIcons.Suggestion,
            checked = state.areIndicatorsEnabled,
            onCheckedChange = { viewModel.dispatch(HistoryViewAction.SetIndicatorsEnabled(it)) },
        )
        item(
            key = "clear",
            title = context.getString(R.string.history_clear),
            icon = AppIcons.Delete,
            enabled = state.isHistoryEnabled && state.records.isNotEmpty(),
            onClick = { showClearConfirm = true },
        )

        header(context.getString(R.string.history))
        when {
            state.isLoading && state.records.isEmpty() -> info(key = "loading", title = context.getString(R.string.loading))

            !state.isHistoryEnabled || state.records.isEmpty() -> info(
                key = "empty",
                title = context.getString(
                    if (state.isHistoryEnabled) R.string.history_empty_title else R.string.history_disabled_title,
                ),
                summary = context.getString(
                    if (state.isHistoryEnabled) R.string.history_empty_desc else R.string.history_disabled_desc,
                ),
            )

            else -> state.records.forEach { record ->
                val badge = if (state.areIndicatorsEnabled) record.badgeText(context) else null
                val summary = listOfNotNull(
                    context.getString(record.status.labelRes()),
                    badge,
                    record.timestamp.formatHistoryTime(),
                ).joinToString("  •  ")
                item(
                    key = "record_${record.id}",
                    title = record.appLabel ?: record.packageName,
                    summary = summary,
                    icon = if (record.status == OperationStatus.FAILED) AppIcons.BugReport else AppIcons.Check,
                    preview = TvPreview(
                        title = record.appLabel ?: record.packageName,
                        description = listOf(
                            record.packageName,
                            context.getString(record.operationType.labelRes()),
                            summary,
                        ).joinToString("\n"),
                    ),
                    drillIn = true,
                    onClick = { selectedRecord = record },
                )
            }
        }
    }
}

/** The record's install badge text, matching the phone list's indicator. */
private fun OperationHistoryModel.badgeText(context: Context): String? {
    if (operationType != OperationType.INSTALL) return null
    val res = when {
        packageName == context.packageName -> R.string.history_version_self_update
        versionChange == VersionChange.FRESH_INSTALL -> R.string.history_version_fresh_install
        versionChange == VersionChange.UPDATE -> R.string.history_version_update
        versionChange == VersionChange.DOWNGRADE -> R.string.history_version_downgrade
        else -> return null
    }
    return context.getString(res)
}

/**
 * Every detail line is a focusable row, so the remote can scroll through the record; OK copies the value.
 */
@Composable
private fun TvHistoryRecordPage(record: OperationHistoryModel, isSystemApp: Boolean) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val copy: (String, String) -> Unit = { title, value ->
        scope.launch { clipboard.setClipEntry(ClipData.newPlainText(title, value).toClipEntry()) }
        context.toast(R.string.copied_format, value)
    }
    val none = stringResource(R.string.history_none)
    val unknown = stringResource(R.string.history_unknown)
    val authorizer = historyAuthorizerText(record.authorizer, isSystemApp)

    val lines = buildList {
        add(R.string.history_package_name to record.packageName)
        add(R.string.history_operation_type to context.getString(record.operationType.labelRes()))
        add(R.string.history_time to record.timestamp.formatHistoryTime())
        if (record.installMethod != InstallMethod.SESSION) {
            add(R.string.history_version_change to context.getString(record.versionChange.labelRes()))
            add(
                R.string.history_version_name to context.getString(
                    R.string.history_version_pair,
                    record.oldVersionName?.takeIf { it.isNotBlank() } ?: none,
                    record.newVersionName?.takeIf { it.isNotBlank() } ?: none,
                ),
            )
            add(
                R.string.history_version_code to context.getString(
                    R.string.history_version_pair,
                    record.oldVersionCode?.toString() ?: none,
                    record.newVersionCode?.toString() ?: none,
                ),
            )
        }
        add(R.string.history_initiator to (record.initiatorPackageName ?: unknown))
        add(R.string.history_installer_package to (record.installerPackageName ?: unknown))
        if (record.installMethod != InstallMethod.SESSION) {
            add(R.string.history_apk_path to (record.sourcePaths.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: none))
        }
        add(R.string.history_method to context.getString(record.installMethod.labelRes()))
        add(R.string.history_authorizer to authorizer)
        if (record.status == OperationStatus.FAILED) {
            add(
                R.string.history_error to listOfNotNull(record.errorType, record.errorSummary)
                    .joinToString(": ")
                    .ifBlank { unknown },
            )
        }
    }

    TvSettingsPage(
        title = record.appLabel ?: record.packageName,
        subtitle = context.getString(record.status.labelRes()),
    ) {
        lines.forEach { (titleRes, value) ->
            val title = context.getString(titleRes)
            item(
                key = "line_$titleRes",
                title = title,
                summary = value,
                description = value,
                icon = if (titleRes == R.string.history_error) AppIcons.BugReport else null,
                onClick = { copy(title, value) },
            )
        }
    }
}
