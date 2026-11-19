// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.settings.pages

import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import com.rosan.installer.R
import com.rosan.installer.core.device.model.Manufacturer
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.domain.settings.model.app.NamedPackage
import com.rosan.installer.domain.settings.model.app.SharedUid
import com.rosan.installer.domain.settings.model.config.BiometricAuthMode
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.preferred.installer.InstallerSettingsAction
import com.rosan.installer.ui.page.main.settings.preferred.installer.InstallerSettingsState
import com.rosan.installer.ui.page.main.settings.preferred.installer.InstallerSettingsViewModel
import com.rosan.installer.ui.page.tv.components.TvAction
import com.rosan.installer.ui.page.tv.components.TvActionRow
import com.rosan.installer.ui.page.tv.components.TvConfirmScreen
import com.rosan.installer.ui.page.tv.settings.LocalTvSettingsNavigator
import com.rosan.installer.ui.page.tv.settings.TvChoiceOption
import com.rosan.installer.ui.page.tv.settings.TvSettingsDest
import com.rosan.installer.ui.page.tv.settings.TvSettingsPage
import org.koin.androidx.compose.koinViewModel

/** The user-managed lists on the installer settings page. */
private enum class ManagedListKind { InstallerPackages, BlacklistPackages, SharedUidBlacklist, SharedUidExempted }

/** One managed list, flattened to name/detail rows plus the actions that edit it. */
private class ManagedListModel(
    val title: String,
    val emptyText: String,
    val addTitle: String,
    val nameLabel: String,
    val valueLabel: String,
    val icon: ImageVector,
    val entries: List<Pair<String, String>>,
    val isValidValue: (String) -> Boolean,
    val add: (name: String, value: String) -> Unit,
    val remove: (index: Int) -> Unit,
    val move: (from: Int, to: Int) -> Unit,
)

/** Global installer settings, matching the phone Installer Settings page. */
@Composable
fun TvInstallerSettingsPage(viewModel: InstallerSettingsViewModel = koinViewModel()) {
    val navigator = LocalTvSettingsNavigator.current
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var openList by rememberSaveable { mutableStateOf<ManagedListKind?>(null) }

    val currentList = openList
    if (currentList != null) {
        BackHandler { openList = null }
        TvManagedListPage(rememberManagedListModel(currentList, uiState, viewModel::dispatch))
        return
    }

    val biometricAvailable = remember {
        BiometricManager.from(context)
            .canAuthenticate(BIOMETRIC_WEAK or BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    }
    val biometricModes = remember { listOf(BiometricAuthMode.Disable, BiometricAuthMode.Enable, BiometricAuthMode.FollowConfig) }
    val showOppo = DeviceConfig.currentManufacturer == Manufacturer.OPPO || DeviceConfig.currentManufacturer == Manufacturer.ONEPLUS
    val lists = buildList {
        add(ManagedListKind.InstallerPackages)
        add(ManagedListKind.BlacklistPackages)
        add(ManagedListKind.SharedUidBlacklist)
        // Exemptions only matter once a SharedUID is blocked, as on the phone page.
        if (uiState.managedSharedUserIdBlacklist.isNotEmpty()) add(ManagedListKind.SharedUidExempted)
    }.map { it to rememberManagedListModel(it, uiState, viewModel::dispatch) }

    TvSettingsPage(title = stringResource(R.string.installer_settings)) {
        header(context.getString(R.string.installer_settings_global_installer))
        item(
            key = "dialog_settings",
            title = context.getString(R.string.dialog_settings),
            summary = context.getString(R.string.dialog_settings_desc),
            icon = AppIcons.Dialog,
            opens = TvSettingsDest.DialogSettings,
            onClick = { navigator.push(TvSettingsDest.DialogSettings) },
        )
        item(
            key = "notification_settings",
            title = context.getString(R.string.notification_settings),
            summary = context.getString(R.string.notification_settings_desc),
            icon = AppIcons.Notification,
            opens = TvSettingsDest.NotificationSettings,
            onClick = { navigator.push(TvSettingsDest.NotificationSettings) },
        )
        item(
            key = "authorizer_customization",
            title = context.getString(R.string.authorizer_customization),
            summary = context.getString(R.string.authorizer_customization_desc),
            icon = AppIcons.Authorizer,
            opens = TvSettingsDest.AuthorizerCust,
            onClick = { navigator.push(TvSettingsDest.AuthorizerCust) },
        )
        if (biometricAvailable) {
            val selected = biometricModes.indexOf(uiState.installerRequireBiometricAuth).coerceAtLeast(0)
            choice(
                key = "biometric_auth",
                title = context.getString(R.string.installer_settings_require_biometric_auth),
                icon = AppIcons.BiometricAuth,
                description = context.getString(biometricModeDescription(biometricModes[selected])),
                options = biometricModes.map {
                    TvChoiceOption(context.getString(biometricModeLabel(it)), context.getString(biometricModeDescription(it)))
                },
                selectedIndex = selected,
                onSelect = { index ->
                    val mode = biometricModes[index]
                    if (mode != uiState.installerRequireBiometricAuth) {
                        viewModel.dispatch(InstallerSettingsAction.ChangeBiometricAuth(mode))
                    }
                },
            )
        }

        header(context.getString(R.string.installer_settings_signature_check))
        switch(
            key = "check_app_signature",
            title = context.getString(R.string.config_check_app_signature),
            summary = context.getString(R.string.config_check_app_signature_desc),
            icon = AppIcons.InstallRequestUpdateOwnership,
            checked = uiState.checkAppSignature,
            onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeCheckAppSignature(it)) },
        )
        if (uiState.checkAppSignature) {
            switch(
                key = "check_source_stamp",
                title = context.getString(R.string.config_check_source_stamp),
                summary = context.getString(R.string.config_check_source_stamp_desc),
                icon = AppIcons.Info,
                checked = uiState.checkSourceStamp,
                onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeCheckSourceStamp(it)) },
            )
            switch(
                key = "signature_info_on_match",
                title = context.getString(R.string.config_show_signature_info_on_match),
                summary = context.getString(R.string.config_show_signature_info_on_match_desc),
                icon = AppIcons.Info,
                checked = uiState.showSignatureInfoOnMatch,
                onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeShowSignatureInfoOnMatch(it)) },
            )
            switch(
                key = "signature_details",
                title = context.getString(R.string.config_show_signature_details),
                summary = context.getString(R.string.config_show_signature_details_desc),
                icon = AppIcons.InstallAllowSigUnknown,
                checked = uiState.showSignatureDetails,
                onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeShowSignatureDetails(it)) },
            )
        }

        header(context.getString(R.string.installer_settings_xposed_detection))
        switch(
            key = "detect_xposed",
            title = context.getString(R.string.config_detect_xposed_module),
            summary = context.getString(R.string.config_detect_xposed_module_desc),
            icon = AppIcons.Xposed,
            checked = uiState.detectXposedModule,
            onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeDetectXposedModule(it)) },
        )
        if (uiState.detectXposedModule) {
            switch(
                key = "quick_open_lsposed",
                title = context.getString(R.string.config_quick_open_lsposed),
                summary = context.getString(R.string.config_quick_open_lsposed_desc),
                icon = AppIcons.LSPosed,
                checked = uiState.quickOpenLSPosed,
                onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeQuickOpenLSPosed(it)) },
            )
        }

        if (showOppo) {
            header(context.getString(R.string.installer_oppo_related))
            switch(
                key = "oppo_special",
                title = context.getString(R.string.installer_show_oem_special),
                summary = context.getString(R.string.installer_show_oem_special_desc),
                icon = AppIcons.OEMSpecial,
                checked = uiState.showOPPOSpecial,
                onCheckedChange = { viewModel.dispatch(InstallerSettingsAction.ChangeShowOPPOSpecial(it)) },
            )
        }

        lists.forEach { (kind, model) ->
            // The exempted list shares the SharedUID group's header, as on the phone page.
            if (kind != ManagedListKind.SharedUidExempted) header(model.title)
            val preview = model.entries.joinToString(", ") { it.first }
            item(
                key = "list_${kind.name}",
                title = if (kind == ManagedListKind.SharedUidExempted) model.title else preview.ifBlank { model.emptyText },
                summary = if (kind == ManagedListKind.SharedUidExempted) preview.ifBlank { null } else null,
                icon = model.icon,
                description = model.entries.joinToString("\n") { "${it.first}  •  ${it.second}" }.ifBlank { model.emptyText },
                drillIn = true,
                onClick = { openList = kind },
            )
        }
    }
}

private fun biometricModeLabel(mode: BiometricAuthMode): Int = when (mode) {
    BiometricAuthMode.Disable -> R.string.installer_biometric_auth_mode_disable
    BiometricAuthMode.Enable -> R.string.installer_biometric_auth_mode_enable
    BiometricAuthMode.FollowConfig -> R.string.installer_biometric_auth_mode_follow_config
}

private fun biometricModeDescription(mode: BiometricAuthMode): Int = when (mode) {
    BiometricAuthMode.Disable -> R.string.installer_biometric_auth_mode_disable_desc
    BiometricAuthMode.Enable -> R.string.installer_biometric_auth_mode_enable_desc
    BiometricAuthMode.FollowConfig -> R.string.installer_biometric_auth_mode_follow_config_desc
}

@Composable
private fun rememberManagedListModel(
    kind: ManagedListKind,
    uiState: InstallerSettingsState,
    dispatch: (InstallerSettingsAction) -> Unit,
): ManagedListModel {
    val addPackage = stringResource(R.string.config_add_new_package)
    val name = stringResource(R.string.config_name)
    val packageName = stringResource(R.string.config_package_name)
    fun packageList(
        title: String,
        emptyText: String,
        packages: List<NamedPackage>,
        add: (NamedPackage) -> InstallerSettingsAction,
        remove: (NamedPackage) -> InstallerSettingsAction,
        move: (Int, Int) -> InstallerSettingsAction,
    ) = ManagedListModel(
        title = title,
        emptyText = emptyText,
        addTitle = addPackage,
        nameLabel = name,
        valueLabel = packageName,
        icon = AppIcons.Android,
        entries = packages.map { it.name to it.packageName },
        isValidValue = { it.isNotBlank() },
        add = { n, v -> dispatch(add(NamedPackage(n, v))) },
        remove = { index -> packages.getOrNull(index)?.let { dispatch(remove(it)) } },
        move = { from, to -> dispatch(move(from, to)) },
    )

    return when (kind) {
        ManagedListKind.InstallerPackages -> packageList(
            title = stringResource(R.string.config_managed_installer_packages_title),
            emptyText = stringResource(R.string.config_no_preset_install_sources),
            packages = uiState.managedInstallerPackages,
            add = InstallerSettingsAction::AddManagedInstallerPackage,
            remove = InstallerSettingsAction::RemoveManagedInstallerPackage,
            move = InstallerSettingsAction::MoveManagedInstallerPackage,
        )

        ManagedListKind.BlacklistPackages -> packageList(
            title = stringResource(R.string.config_managed_blacklist_by_package_name_title),
            emptyText = stringResource(R.string.config_no_managed_blacklist),
            packages = uiState.managedBlacklistPackages,
            add = InstallerSettingsAction::AddManagedBlacklistPackage,
            remove = InstallerSettingsAction::RemoveManagedBlacklistPackage,
            move = InstallerSettingsAction::MoveManagedBlacklistPackage,
        )

        ManagedListKind.SharedUidExempted -> packageList(
            title = stringResource(R.string.config_no_managed_shared_user_id_exempted_packages),
            emptyText = stringResource(R.string.config_no_managed_shared_user_id_exempted_packages),
            packages = uiState.managedSharedUserIdExemptedPackages,
            add = InstallerSettingsAction::AddManagedSharedUserIdExemptedPackages,
            remove = InstallerSettingsAction::RemoveManagedSharedUserIdExemptedPackages,
            move = InstallerSettingsAction::MoveManagedSharedUserIdExemptedPackages,
        )

        ManagedListKind.SharedUidBlacklist -> {
            val uids = uiState.managedSharedUserIdBlacklist
            ManagedListModel(
                title = stringResource(R.string.config_managed_blacklist_by_shared_user_id_title),
                emptyText = stringResource(R.string.config_no_managed_shared_user_id_blacklist),
                addTitle = stringResource(R.string.config_add_new_shared_uid),
                nameLabel = stringResource(R.string.config_shared_uid_name),
                valueLabel = stringResource(R.string.config_shared_uid_value),
                icon = AppIcons.BugReport,
                entries = uids.map { it.uidName to "UID: ${it.uidValue}" },
                isValidValue = { it.trim().toIntOrNull() != null },
                add = { n, v -> v.trim().toIntOrNull()?.let { dispatch(InstallerSettingsAction.AddManagedSharedUserIdBlacklist(SharedUid(n, it))) } },
                remove = { index -> uids.getOrNull(index)?.let { dispatch(InstallerSettingsAction.RemoveManagedSharedUserIdBlacklist(it)) } },
                move = { from, to -> dispatch(InstallerSettingsAction.MoveManagedSharedUserIdBlacklist(from, to)) },
            )
        }
    }
}

/**
 * A managed list as TV pages: the list with an Add row, a per-entry page to move or delete it,
 * a delete confirmation, and an add page with text entry. Replaces the phone page's drag-to-reorder.
 */
@Composable
private fun TvManagedListPage(model: ManagedListModel) {
    val context = LocalContext.current
    var adding by rememberSaveable { mutableStateOf(false) }
    // Tracked by value rather than position, so the page follows the entry as it moves and never
    // names a neighbor while the reordered list is on its way.
    var selectedValue by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    if (adding) {
        BackHandler { adding = false }
        TvAddEntryPage(model) { adding = false }
        return
    }

    val index = selectedValue?.let { value -> model.entries.indexOfFirst { it.second == value } } ?: -1
    val selected = model.entries.getOrNull(index)
    if (selected != null) {
        if (confirmDelete) {
            BackHandler { confirmDelete = false }
            TvConfirmScreen(
                title = stringResource(R.string.config_confirm_deletion),
                description = stringResource(R.string.config_confirm_deletion_desc, selected.first),
                accent = MaterialTheme.colorScheme.error,
                actions = {
                    TvActionRow(
                        actions = listOf(
                            TvAction(stringResource(R.string.delete), onClick = {
                                model.remove(index)
                                confirmDelete = false
                                selectedValue = null
                            }),
                            TvAction(stringResource(R.string.cancel), onClick = { confirmDelete = false }, isDefault = true),
                        ),
                    )
                },
            )
            return
        }
        BackHandler { selectedValue = null }
        TvSettingsPage(title = selected.first, subtitle = selected.second) {
            item(
                key = "move_up",
                title = context.getString(R.string.move_item_up),
                summary = "${index + 1} / ${model.entries.size}",
                icon = Icons.Rounded.KeyboardArrowUp,
                // Stays enabled at the ends (a no-op there) so the focused row does not go dead.
                onClick = { if (index > 0) model.move(index, index - 1) },
            )
            item(
                key = "move_down",
                title = context.getString(R.string.move_item_down),
                summary = "${index + 1} / ${model.entries.size}",
                icon = Icons.Rounded.KeyboardArrowDown,
                onClick = { if (index < model.entries.lastIndex) model.move(index, index + 1) },
            )
            item(
                key = "delete",
                title = context.getString(R.string.delete),
                icon = AppIcons.Delete,
                onClick = { confirmDelete = true },
            )
        }
        return
    }

    TvSettingsPage(title = model.title) {
        item(
            key = "add",
            title = model.addTitle,
            icon = AppIcons.Add,
            drillIn = true,
            onClick = { adding = true },
        )
        if (model.entries.isEmpty()) {
            info(key = "empty", title = model.emptyText)
        } else {
            model.entries.forEachIndexed { index, (name, detail) ->
                item(
                    key = "entry_$index",
                    title = name,
                    summary = detail,
                    icon = model.icon,
                    drillIn = true,
                    onClick = { selectedValue = model.entries[index].second },
                )
            }
        }
    }
}

@Composable
private fun TvAddEntryPage(model: ManagedListModel, onDone: () -> Unit) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var value by rememberSaveable { mutableStateOf("") }
    val valid = name.isNotBlank() && model.isValidValue(value)

    TvSettingsPage(title = model.addTitle) {
        textInput(
            key = "name",
            title = model.nameLabel,
            value = name,
            onValueChange = { name = it },
        )
        textInput(
            key = "value",
            title = model.valueLabel,
            value = value,
            onValueChange = { value = it },
        )
        item(
            key = "confirm",
            title = context.getString(R.string.confirm),
            icon = AppIcons.Check,
            enabled = valid,
            onClick = {
                model.add(name.trim(), value.trim())
                onDone()
            },
        )
        item(
            key = "cancel",
            title = context.getString(R.string.cancel),
            icon = AppIcons.Close,
            onClick = onDone,
        )
    }
}
