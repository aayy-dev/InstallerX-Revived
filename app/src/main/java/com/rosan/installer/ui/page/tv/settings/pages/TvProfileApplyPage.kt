// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.settings.pages

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewAction
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewModel
import com.rosan.installer.ui.page.main.settings.config.apply.ApplyViewState
import com.rosan.installer.ui.page.main.settings.config.apply.ViewContent
import com.rosan.installer.ui.page.tv.settings.TvChoiceOption
import com.rosan.installer.ui.page.tv.settings.TvSettingsPage
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** Apps a profile applies to, backed by the phone's [ApplyViewModel]; each app is a switch. */
@Composable
fun TvProfileApplyPage(id: Long) {
    TvProfileViewModelScope { TvProfileApplyContent(id) }
}

@Composable
private fun TvProfileApplyContent(id: Long, viewModel: ApplyViewModel = koinViewModel { parametersOf(id) }) {
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var showOptions by remember { mutableStateOf(false) }

    val orderOptions = listOf(
        ApplyViewState.OrderType.Label to R.string.sort_by_label,
        ApplyViewState.OrderType.PackageName to R.string.sort_by_package_name,
        ApplyViewState.OrderType.FirstInstallTime to R.string.sort_by_install_time,
    )

    if (showOptions) {
        // The phone page's overflow menu: sort order and list filters.
        BackHandler { showOptions = false }
        TvSettingsPage(title = stringResource(R.string.options)) {
            choice(
                key = "sort",
                title = context.getString(R.string.sort),
                options = orderOptions.map { TvChoiceOption(context.getString(it.second)) },
                selectedIndex = orderOptions.indexOfFirst { it.first == uiState.orderType },
                onSelect = { index -> orderOptions.getOrNull(index)?.let { viewModel.dispatch(ApplyViewAction.Order(it.first)) } },
            )
            switch(
                key = "reverse_order",
                title = context.getString(R.string.sort_by_reverse_order),
                checked = uiState.orderInReverse,
                onCheckedChange = { viewModel.dispatch(ApplyViewAction.OrderInReverse(it)) },
            )
            switch(
                key = "selected_first",
                title = context.getString(R.string.sort_by_selected_first),
                checked = uiState.selectedFirst,
                onCheckedChange = { viewModel.dispatch(ApplyViewAction.SelectedFirst(it)) },
            )
            switch(
                key = "show_system_app",
                title = context.getString(R.string.sort_by_show_system_app),
                checked = uiState.showSystemApp,
                onCheckedChange = { viewModel.dispatch(ApplyViewAction.ShowSystemApp(it)) },
            )
            switch(
                key = "show_package_name",
                title = context.getString(R.string.sort_by_show_package_name),
                checked = uiState.showPackageName,
                onCheckedChange = { viewModel.dispatch(ApplyViewAction.ShowPackageName(it)) },
            )
            switch(
                key = "show_unknown_scope",
                title = context.getString(R.string.sort_by_show_unknown_scope),
                checked = uiState.showUnknownScope,
                onCheckedChange = { viewModel.dispatch(ApplyViewAction.ShowUnknownScope(it)) },
            )
        }
        return
    }

    val appliedPackageSet = remember(uiState.appEntities.data) {
        uiState.appEntities.data.map { it.packageName }.toHashSet()
    }
    val unknownLabel = stringResource(R.string.config_scope_unknown)
    val search = uiState.search
    val apps = uiState.checkedApps
    val showUnknown = uiState.showUnknownScope &&
        (search.isBlank() || unknownLabel.contains(search, ignoreCase = true))
    val currentOrderLabel = orderOptions.firstOrNull { it.first == uiState.orderType }?.let { context.getString(it.second) }

    TvSettingsPage(title = stringResource(R.string.config_scope)) {
        textInput(
            key = "search",
            title = context.getString(R.string.search),
            value = search,
            onValueChange = { viewModel.dispatch(ApplyViewAction.Search(it)) },
            icon = AppIcons.Search,
        )
        item(
            key = "options",
            title = context.getString(R.string.options),
            summary = currentOrderLabel,
            icon = AppIcons.Rule,
            onClick = { showOptions = true },
        )

        if (uiState.apps.progress is ViewContent.Progress.Loading && uiState.apps.data.isEmpty()) {
            info(key = "loading", title = context.getString(R.string.loading))
            return@TvSettingsPage
        }

        if (showUnknown) {
            header(context.getString(R.string.config_scope_unknown_group))
            val isApplied = appliedPackageSet.contains(null)
            switch(
                key = "unknown_scope",
                title = unknownLabel,
                checked = isApplied,
                onCheckedChange = { viewModel.dispatch(ApplyViewAction.ApplyPackageName(null, it)) },
            )
        }

        if (apps.isNotEmpty()) {
            header(context.getString(R.string.config_scope_apps))
            apps.forEach { app ->
                val isApplied = appliedPackageSet.contains(app.packageName)
                val title = app.label ?: app.packageName
                switch(
                    key = "app_${app.packageName}",
                    title = title,
                    summary = if (uiState.showPackageName) app.packageName else null,
                    description = listOfNotNull(app.packageName, app.versionName).joinToString("\n"),
                    checked = isApplied,
                    onCheckedChange = { viewModel.dispatch(ApplyViewAction.ApplyPackageName(app.packageName, it)) },
                )
            }
        }
    }
}
