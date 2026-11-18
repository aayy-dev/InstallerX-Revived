// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.settings.pages

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.rosan.installer.BuildConfig
import com.rosan.installer.R
import com.rosan.installer.core.device.model.Level
import com.rosan.installer.core.env.AppConfig
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutAction
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutEvent
import com.rosan.installer.ui.page.main.settings.preferred.about.AboutViewModel
import com.rosan.installer.ui.page.tv.components.TvAction
import com.rosan.installer.ui.page.tv.components.TvActionRow
import com.rosan.installer.ui.page.tv.components.TvAppIcon
import com.rosan.installer.ui.page.tv.components.TvConfirmScreen
import com.rosan.installer.ui.page.tv.components.TvProgressBar
import com.rosan.installer.ui.page.tv.components.TvScrollableText
import com.rosan.installer.ui.page.tv.settings.TvSettingsPage
import com.rosan.installer.util.getErrorMessage
import com.rosan.installer.util.toast
import org.koin.androidx.compose.koinViewModel

private const val SOURCE_URL = "https://github.com/wxxsfxyzm/InstallerX-Revived"
private const val RELEASES_URL = "https://github.com/wxxsfxyzm/InstallerX-Revived/releases"
private const val TELEGRAM_URL = "https://t.me/installerx_revived"

/** About on TV: version, source, licenses, update links and the debug log options. */
@Composable
fun TvAboutPage(viewModel: AboutViewModel = koinViewModel()) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    var showLicenses by remember { mutableStateOf(false) }
    var showUpdateOptions by remember { mutableStateOf(false) }
    var updating by remember { mutableStateOf(false) }
    var updateError by remember { mutableStateOf<AboutEvent.ShowInAppUpdateErrorDetail?>(null) }

    // One collector for every About event, mirroring the phone's log and update collectors.
    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is AboutEvent.ShowUpdateLoading -> updating = true

                is AboutEvent.HideUpdateLoading -> updating = false

                is AboutEvent.ShowInAppUpdateErrorDetail -> {
                    updating = false
                    updateError = event
                }

                is AboutEvent.OpenLogShare -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching { context.startActivity(Intent.createChooser(intent, "Share Log")) }
                }

                is AboutEvent.ShareLogFailed -> context.toast(event.error)
            }
        }
    }

    // Many TV devices have no browser, so a missing handler must not crash the page.
    val openUri: (String) -> Unit = { url -> runCatching { uriHandler.openUri(url) } }

    if (updating) {
        BackHandler { /* Block input while the update downloads, as on the phone. */ }
        TvConfirmScreen(
            title = stringResource(R.string.updating),
            actions = {},
            extra = { TvProgressBar(progress = -1f, modifier = Modifier.fillMaxWidth()) },
        )
        return
    }

    updateError?.let { error ->
        BackHandler { updateError = null }
        TvConfirmScreen(
            title = error.title,
            description = error.exception.getErrorMessage(context),
            extra = {
                TvScrollableText(
                    text = error.exception.stackTraceToString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                )
            },
            actions = {
                TvActionRow(actions = listOf(TvAction(stringResource(R.string.close), onClick = { updateError = null }, isDefault = true)))
            },
        )
        return
    }

    if (showLicenses) {
        BackHandler { showLicenses = false }
        TvOpenSourceLicenses(openUri = openUri)
        return
    }

    if (showUpdateOptions) {
        BackHandler { showUpdateOptions = false }
        val actions = buildList {
            if (uiState.hasUpdate) {
                add(
                    TvAction(stringResource(R.string.get_update_directly), onClick = {
                        showUpdateOptions = false
                        viewModel.dispatch(AboutAction.PerformUpdate)
                    }, isDefault = true),
                )
            }
            add(TvAction("GitHub", onClick = { openUri(RELEASES_URL) }))
            add(TvAction("Telegram", onClick = { openUri(TELEGRAM_URL) }))
        }
        TvConfirmScreen(
            title = stringResource(R.string.get_update),
            description = stringResource(R.string.get_update_detail),
            badge = { TvAppIcon(bitmap = uiState.appIcon, size = 56.dp, fallback = AppIcons.Update) },
            actions = { TvActionRow(actions = actions) },
        )
        return
    }

    val level = stringResource(
        when (AppConfig.LEVEL) {
            Level.STABLE -> R.string.stable
            Level.PREVIEW -> R.string.preview
            Level.UNSTABLE -> R.string.unstable
        },
    )
    val versionInfo = stringResource(R.string.app_version_info_format, level, AppConfig.VERSION_NAME, AppConfig.VERSION_CODE)
    val updateAvailable = if (uiState.hasUpdate) stringResource(R.string.update_available, uiState.remoteVersion) else null
    val showDebug = AppConfig.isLogEnabled && context.packageName == BuildConfig.APPLICATION_ID

    TvSettingsPage(title = stringResource(R.string.about_detail), subtitle = versionInfo) {
        if (updateAvailable != null) {
            item(
                key = "update_available",
                title = updateAvailable,
                summary = context.getString(R.string.get_update_directly_desc),
                icon = AppIcons.Download,
                onClick = { viewModel.dispatch(AboutAction.PerformUpdate) },
            )
        }

        header(context.getString(R.string.about))
        item(
            key = "source_code",
            title = context.getString(R.string.get_source_code),
            summary = context.getString(R.string.get_source_code_detail),
            icon = AppIcons.ViewSourceCode,
            onClick = { openUri(SOURCE_URL) },
        )
        item(
            key = "licenses",
            title = context.getString(R.string.open_source_license),
            summary = context.getString(R.string.open_source_license_settings_description),
            icon = AppIcons.OpenSourceLicense,
            onClick = { showLicenses = true },
        )
        item(
            key = "get_update",
            title = context.getString(R.string.get_update),
            summary = context.getString(R.string.get_update_detail),
            icon = AppIcons.Update,
            onClick = { showUpdateOptions = true },
        )

        if (showDebug) {
            header(context.getString(R.string.debug))
            switch(
                key = "save_logs",
                title = context.getString(R.string.save_logs),
                summary = context.getString(R.string.save_logs_desc),
                icon = AppIcons.BugReport,
                checked = uiState.enableFileLogging,
                onCheckedChange = { viewModel.dispatch(AboutAction.SetEnableFileLogging(it)) },
            )
            if (uiState.enableFileLogging) {
                item(
                    key = "export_logs",
                    title = context.getString(R.string.export_logs),
                    summary = context.getString(R.string.export_logs_desc),
                    icon = AppIcons.BugReport,
                    onClick = { viewModel.dispatch(AboutAction.ShareLog) },
                )
            }
        }
    }
}

/** The bundled library list; a library opens a screen with its license text. */
@Composable
private fun TvOpenSourceLicenses(openUri: (String) -> Unit) {
    val libs by produceLibraries(R.raw.aboutlibraries)
    var selected by remember { mutableStateOf<Library?>(null) }

    selected?.let { library ->
        BackHandler { selected = null }
        val noLicense = stringResource(R.string.no_license_text)
        val licenseText = library.licenses
            .joinToString(separator = "\n\n") { license -> "${license.name}\n\n${license.licenseContent ?: noLicense}" }
            .ifBlank { noLicense }
        val website = library.website
        TvConfirmScreen(
            title = library.name,
            description = library.artifactVersion,
            extra = {
                TvScrollableText(
                    text = licenseText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                )
            },
            actions = {
                TvActionRow(
                    actions = buildList {
                        add(TvAction(stringResource(R.string.close), onClick = { selected = null }, isDefault = true))
                        if (!website.isNullOrBlank()) {
                            add(TvAction(stringResource(R.string.visit_home_page), onClick = { openUri(website) }))
                        }
                    },
                )
            },
        )
        return
    }

    // Compose the list only once it has loaded, so the page's first-row focus lands on a library.
    val libraries = libs?.libraries ?: return
    TvSettingsPage(title = stringResource(R.string.open_source_license)) {
        libraries.forEach { library ->
            item(
                key = "lib_${library.uniqueId}",
                title = library.name,
                summary = listOfNotNull(library.artifactVersion, library.licenses.joinToString { it.name }.ifBlank { null })
                    .joinToString(" · "),
                icon = AppIcons.OpenSourceLicense,
                onClick = { selected = library },
            )
        }
    }
}
