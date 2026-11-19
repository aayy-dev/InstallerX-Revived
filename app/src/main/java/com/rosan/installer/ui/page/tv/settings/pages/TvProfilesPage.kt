// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.settings.pages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.tv.material3.Icon
import com.rosan.installer.R
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.model.config.DeletedConfigSnapshot
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.config.all.AllViewAction
import com.rosan.installer.ui.page.main.settings.config.all.AllViewEvent
import com.rosan.installer.ui.page.main.settings.config.all.AllViewModel
import com.rosan.installer.ui.page.main.settings.config.all.AllViewState
import com.rosan.installer.ui.page.tv.components.TvAction
import com.rosan.installer.ui.page.tv.components.TvActionRow
import com.rosan.installer.ui.page.tv.components.TvConfirmScreen
import com.rosan.installer.ui.page.tv.settings.LocalTvSettingsNavigator
import com.rosan.installer.ui.page.tv.settings.TvPreview
import com.rosan.installer.ui.page.tv.settings.TvSettingsDest
import com.rosan.installer.ui.page.tv.settings.TvSettingsPage
import com.rosan.installer.util.toast
import org.koin.androidx.compose.koinViewModel

/**
 * The TV settings app has no per-destination view model stores (the phone navigation provides one per
 * entry), so the profile pages give each visit its own store. Opening a profile therefore always loads
 * fresh data, and discarded edits do not survive into the next visit.
 */
@Composable
internal fun TvProfileViewModelScope(content: @Composable () -> Unit) {
    val owner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
}

/** Profile list: add, edit, apply to apps and delete, backed by the phone's [AllViewModel]. */
@Composable
fun TvProfilesPage() {
    TvProfileViewModelScope { TvProfilesContent() }
}

private sealed interface ProfileSheet {
    data class Actions(val config: ConfigModel) : ProfileSheet
    data class ConfirmDelete(val config: ConfigModel) : ProfileSheet
}

@Composable
private fun TvProfilesContent(viewModel: AllViewModel = koinViewModel()) {
    val navigator = LocalTvSettingsNavigator.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var sheet by remember { mutableStateOf<ProfileSheet?>(null) }
    var lastDeleted by remember { mutableStateOf<DeletedConfigSnapshot?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is AllViewEvent.DeletedConfig -> {
                    lastDeleted = event.snapshot
                    context.toast(R.string.delete_success)
                }

                is AllViewEvent.NavigateToEditConfig -> navigator.push(TvSettingsDest.ProfileEdit(event.id))

                is AllViewEvent.NavigateToApplyConfig -> navigator.push(TvSettingsDest.ProfileApply(event.id))
            }
        }
    }

    val configs = uiState.data.configs
    // Same rule as the phone page: the oldest profile is the global default and cannot be deleted or scoped.
    val defaultId = configs.minByOrNull { it.id }?.id

    when (val current = sheet) {
        is ProfileSheet.Actions -> {
            BackHandler { sheet = null }
            ProfileActionsScreen(
                config = current.config,
                summary = profileSummary(context, current.config, current.config.id == defaultId),
                onEdit = {
                    sheet = null
                    viewModel.dispatch(AllViewAction.EditDataConfig(current.config))
                },
                onApply = {
                    sheet = null
                    viewModel.dispatch(AllViewAction.ApplyConfig(current.config))
                },
                onDelete = { sheet = ProfileSheet.ConfirmDelete(current.config) },
                onCancel = { sheet = null },
            )
            return
        }

        is ProfileSheet.ConfirmDelete -> {
            BackHandler { sheet = null }
            TvConfirmScreen(
                title = stringResource(R.string.config_confirm_deletion),
                description = current.config.name,
                badge = { Icon(AppIcons.Delete, contentDescription = null, modifier = Modifier.size(28.dp)) },
                actions = {
                    TvActionRow(
                        actions = listOf(
                            TvAction(stringResource(R.string.cancel), onClick = { sheet = null }, isDefault = true),
                            TvAction(
                                stringResource(R.string.delete),
                                onClick = {
                                    sheet = null
                                    viewModel.dispatch(AllViewAction.DeleteDataConfig(current.config))
                                },
                            ),
                        ),
                    )
                },
            )
            return
        }

        null -> Unit
    }

    TvSettingsPage(title = stringResource(R.string.config)) {
        lastDeleted?.let { snapshot ->
            item(
                key = "undo_delete",
                title = context.getString(R.string.restore),
                summary = "${context.getString(R.string.delete_success)}: ${snapshot.configModel.name}",
                icon = AppIcons.Delete,
                onClick = {
                    lastDeleted = null
                    viewModel.dispatch(AllViewAction.RestoreDataConfig(snapshot))
                },
            )
        }

        if (!uiState.userReadScopeTips) {
            item(
                key = "scope_tips",
                title = context.getString(R.string.got_it),
                summary = context.getString(R.string.scope_tips),
                icon = AppIcons.Tip,
                onClick = { viewModel.dispatch(AllViewAction.UserReadScopeTips) },
            )
        }

        item(
            key = "add",
            title = context.getString(R.string.add),
            icon = AppIcons.Add,
            drillIn = true,
            onClick = { navigator.push(TvSettingsDest.ProfileEdit(null)) },
        )

        if (uiState.data.progress is AllViewState.Data.Progress.Loading) {
            info(key = "loading", title = context.getString(R.string.loading))
        } else if (configs.isNotEmpty()) {
            header(context.getString(R.string.config))
            configs.forEach { config ->
                val isDefault = config.id == defaultId
                val summary = profileSummary(context, config, isDefault)
                item(
                    key = "config_${config.id}",
                    title = config.name,
                    summary = summary,
                    icon = if (isDefault) AppIcons.RoomPreferences else AppIcons.Edit,
                    preview = TvPreview(config.name, summary),
                    // The default profile only supports editing, so open it directly.
                    onClick = {
                        if (isDefault) {
                            viewModel.dispatch(AllViewAction.EditDataConfig(config))
                        } else {
                            sheet = ProfileSheet.Actions(config)
                        }
                    },
                    onLongClick = if (isDefault) null else ({ sheet = ProfileSheet.Actions(config) }),
                    drillIn = true,
                )
            }
        }
    }
}

/** Status tag (default / inactive) followed by the description, as the phone card shows them. */
private fun profileSummary(context: android.content.Context, config: ConfigModel, isDefault: Boolean): String? {
    val tag = when {
        isDefault -> context.getString(R.string.config_global_default)
        config.scopeCount == 0 -> context.getString(R.string.config_status_inactive)
        else -> null
    }
    val description = config.description.takeIf { it.isNotEmpty() }
    return listOfNotNull(tag, description).joinToString(" · ").ifEmpty { null }
}

@Composable
private fun ProfileActionsScreen(
    config: ConfigModel,
    summary: String?,
    onEdit: () -> Unit,
    onApply: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    TvConfirmScreen(
        title = config.name,
        description = summary,
        badge = { Icon(AppIcons.Edit, contentDescription = null, modifier = Modifier.size(28.dp)) },
        actions = {
            TvActionRow(
                actions = listOf(
                    TvAction(stringResource(R.string.edit), onClick = onEdit, isDefault = true),
                    TvAction(stringResource(R.string.apply), onClick = onApply),
                    TvAction(stringResource(R.string.delete), onClick = onDelete),
                    TvAction(stringResource(R.string.cancel), onClick = onCancel),
                ),
            )
        },
    )
}
