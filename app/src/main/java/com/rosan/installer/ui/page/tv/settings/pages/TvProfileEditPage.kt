// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.settings.pages

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import com.rosan.installer.R
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.BiometricAuthMode
import com.rosan.installer.domain.settings.model.config.DexoptMode
import com.rosan.installer.domain.settings.model.config.InstallMode
import com.rosan.installer.domain.settings.model.config.InstallReason
import com.rosan.installer.domain.settings.model.config.InstallRequesterMode
import com.rosan.installer.domain.settings.model.config.InstallerMode
import com.rosan.installer.domain.settings.model.config.PackageSource
import com.rosan.installer.domain.settings.model.config.ToastMode
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewAction
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewEvent
import com.rosan.installer.ui.page.main.settings.config.edit.EditViewModel
import com.rosan.installer.ui.page.tv.components.TvAction
import com.rosan.installer.ui.page.tv.components.TvActionRow
import com.rosan.installer.ui.page.tv.components.TvConfirmScreen
import com.rosan.installer.ui.page.tv.settings.LocalTvSettingsNavigator
import com.rosan.installer.ui.page.tv.settings.TvChoiceOption
import com.rosan.installer.ui.page.tv.settings.TvSettingsListScope
import com.rosan.installer.ui.page.tv.settings.TvSettingsPage
import com.rosan.installer.ui.util.isNoneActive
import com.rosan.installer.ui.util.isSystemPackageInstallerActive
import com.rosan.installer.util.toast
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/** Profile editor with every field of the phone editor, backed by the same [EditViewModel]. */
@Composable
fun TvProfileEditPage(id: Long?) {
    TvProfileViewModelScope { TvProfileEditContent(id) }
}

@Composable
private fun TvProfileEditContent(id: Long?, viewModel: EditViewModel = koinViewModel { parametersOf(id) }) {
    val navigator = LocalTvSettingsNavigator.current
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dispatch = viewModel::dispatch
    val capabilityProvider = koinInject<DeviceCapabilityProvider>()

    var showUnsavedScreen by remember { mutableStateOf(false) }
    var showAutoApproveWarning by remember { mutableStateOf(false) }

    // Same event handling as the phone editor: messages surface as toasts here, and a save closes the page.
    val unknownError = stringResource(R.string.installer_unknown_error)
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is EditViewEvent.SnackBar -> context.toast(
                    event.messageResId?.let { context.getString(it) } ?: event.message ?: unknownError,
                )

                is EditViewEvent.Saved -> navigator.pop()
            }
        }
    }

    // Back with unsaved changes or invalid input asks first, like the phone editor's dialog.
    val shouldInterceptBackPress = state.hasUnsavedChanges || state.hasErrors
    BackHandler(enabled = shouldInterceptBackPress && !showUnsavedScreen && !showAutoApproveWarning) {
        showUnsavedScreen = true
    }

    if (showUnsavedScreen) {
        BackHandler { showUnsavedScreen = false }
        val errorMessages = state.activeErrorResIds.map { stringResource(it) }
        TvConfirmScreen(
            title = stringResource(
                if (errorMessages.isNotEmpty()) R.string.config_dialog_title_invalid else R.string.config_dialog_title_unsaved_changes,
            ),
            description = if (errorMessages.isNotEmpty()) {
                errorMessages.joinToString("\n")
            } else {
                stringResource(R.string.config_dialog_message_unsaved_changes)
            },
            badge = { Icon(AppIcons.Save, contentDescription = null, modifier = Modifier.size(28.dp)) },
            actions = {
                TvActionRow(
                    actions = listOf(
                        TvAction(stringResource(R.string.back), onClick = { showUnsavedScreen = false }, isDefault = true),
                        TvAction(
                            stringResource(R.string.discard),
                            onClick = {
                                showUnsavedScreen = false
                                navigator.pop()
                            },
                        ),
                    ),
                )
            },
        )
        return
    }

    if (showAutoApproveWarning) {
        BackHandler { showAutoApproveWarning = false }
        TvConfirmScreen(
            title = stringResource(R.string.warning),
            description = stringResource(R.string.config_auto_approve_session_warning),
            badge = { Icon(AppIcons.Active, contentDescription = null, modifier = Modifier.size(28.dp)) },
            actions = {
                TvActionRow(
                    actions = listOf(
                        TvAction(stringResource(R.string.cancel), onClick = { showAutoApproveWarning = false }, isDefault = true),
                        TvAction(
                            stringResource(R.string.confirm),
                            onClick = {
                                showAutoApproveWarning = false
                                dispatch(EditViewAction.ChangeDataAutoApproveSession(true))
                            },
                        ),
                    ),
                )
            },
        )
        return
    }

    val data = state.data
    val isDhizuku = state.isDhizukuAuthorizerActive
    val labRespect = state.labRespectPlatformInstallPolicy
    val s: (Int) -> String = { res -> context.getString(res) }
    val dhizukuAware: (Int) -> String = { res -> s(state.dhizukuAwareDescriptionRes(res)) }

    // Authorizer names follow the phone editor, including the system-installer naming for "None".
    val authorizers = buildList {
        add(Authorizer.Global)
        if (capabilityProvider.isSessionInstallSupported) add(Authorizer.None)
        addAll(listOf(Authorizer.Root, Authorizer.Shizuku, Authorizer.Dhizuku, Authorizer.Customize))
    }
    val globalAuthorizerName = if (state.globalAuthorizer == Authorizer.None && capabilityProvider.isSystemApp) {
        s(R.string.working_status_system_installer)
    } else {
        s(state.globalAuthorizer.displayNameRes)
    }
    val authorizerNames = authorizers.map { authorizer ->
        when (authorizer) {
            Authorizer.Global -> context.getString(R.string.config_authorizer_global_desc, globalAuthorizerName)
            Authorizer.None -> if (capabilityProvider.isSystemApp) s(R.string.working_status_system_installer) else s(authorizer.displayNameRes)
            else -> s(authorizer.displayNameRes)
        }
    }
    val authorizerDescription = authorizers.indexOf(data.authorizer)
        .takeIf { it >= 0 }
        ?.let(authorizerNames::get)
        ?.let { description ->
            if (data.authorizer == Authorizer.Global &&
                state.globalAuthorizer == Authorizer.Customize &&
                state.globalCustomizeAuthorizer.isNotBlank()
            ) {
                "$description\n${context.getString(R.string.config_authorizer_command_desc, state.globalCustomizeAuthorizer)}"
            } else {
                description
            }
        }

    TvSettingsPage(
        title = stringResource(if (id == null) R.string.add else R.string.update),
        subtitle = data.name.ifBlank { null },
    ) {
        item(
            key = "save",
            title = s(R.string.save),
            summary = if (state.hasErrors) state.activeErrorResIds.joinToString("\n") { s(it) } else null,
            icon = AppIcons.Save,
            onClick = { dispatch(EditViewAction.SaveData) },
        )

        // --- Main settings ---
        header(s(R.string.config_label_main_settings))
        textInput(
            key = "name",
            title = s(R.string.config_name),
            value = data.name,
            onValueChange = { dispatch(EditViewAction.ChangeDataName(it)) },
            icon = AppIcons.Edit,
            description = if (data.errorName) s(R.string.config_error_name) else null,
        )
        textInput(
            key = "description",
            title = s(R.string.config_description),
            value = data.description,
            onValueChange = { dispatch(EditViewAction.ChangeDataDescription(it)) },
            singleLine = false,
        )
        choice(
            key = "authorizer",
            title = s(R.string.config_authorizer),
            options = authorizerNames.map { TvChoiceOption(it) },
            selectedIndex = authorizers.indexOf(data.authorizer),
            onSelect = { index -> authorizers.getOrNull(index)?.let { dispatch(EditViewAction.ChangeDataAuthorizer(it)) } },
            icon = AppIcons.Authorizer,
            description = authorizerDescription,
        )
        if (data.authorizerCustomize) {
            textInput(
                key = "customize_authorizer",
                title = s(R.string.config_authorizer_customize),
                value = data.customizeAuthorizer,
                onValueChange = { dispatch(EditViewAction.ChangeDataCustomizeAuthorizer(it)) },
                icon = AppIcons.Terminal,
                description = if (data.errorCustomizeAuthorizer) s(R.string.config_error_customize_authorizer) else null,
                singleLine = false,
            )
        }
        enumChoice(
            key = "install_mode",
            title = s(R.string.config_install_mode),
            entries = listOf(
                InstallMode.Dialog to s(R.string.config_install_mode_dialog),
                InstallMode.AutoDialog to s(R.string.config_install_mode_auto_dialog),
                InstallMode.Notification to s(R.string.config_install_mode_notification),
                InstallMode.AutoNotification to s(R.string.config_install_mode_auto_notification),
                InstallMode.Ignore to s(R.string.config_install_mode_ignore),
            ),
            current = data.installMode,
            icon = AppIcons.InstallMode,
        ) { dispatch(EditViewAction.ChangeDataInstallMode(it)) }
        switch(
            key = "auto_approve_session",
            title = s(R.string.config_auto_approve_session),
            summary = dhizukuAware(
                if (labRespect) R.string.config_auto_approve_session_disabled_desc else R.string.config_auto_approve_session_desc,
            ),
            icon = AppIcons.Active,
            checked = data.autoApproveSession,
            enabled = !isDhizuku && !labRespect,
            onCheckedChange = { checked ->
                if (checked) showAutoApproveWarning = true else dispatch(EditViewAction.ChangeDataAutoApproveSession(false))
            },
        )
        if (state.globalInstallerBiometricAuthMode == BiometricAuthMode.FollowConfig) {
            switch(
                key = "require_biometric_auth",
                title = s(R.string.installer_settings_require_biometric_auth),
                summary = s(R.string.installer_settings_require_biometric_auth_desc),
                icon = AppIcons.BiometricAuth,
                checked = data.requireBiometricAuth,
                onCheckedChange = { dispatch(EditViewAction.ChangeRequireBiometricAuth(it)) },
            )
        }
        enumChoice(
            key = "toast_mode",
            title = s(R.string.config_install_show_toast),
            entries = listOf(
                ToastMode.Disable to s(R.string.config_toast_mode_disable),
                ToastMode.BackgroundOnly to s(R.string.config_toast_mode_background_only),
                ToastMode.Always to s(R.string.config_toast_mode_always),
            ),
            current = data.toastMode,
            icon = AppIcons.Toast,
        ) { dispatch(EditViewAction.ChangeDataToastMode(it)) }
        if (isNoneActive(data.authorizer, state.globalAuthorizer)) {
            info(key = "none_tips", title = s(R.string.config_authorizer_none_tips), icon = AppIcons.Tip)
        }

        // --- Installer settings ---
        header(s(R.string.config_label_installer_settings))
        switch(
            key = "customize_user",
            title = s(R.string.config_customize_user),
            summary = dhizukuAware(R.string.config_customize_user_desc),
            icon = AppIcons.InstallUser,
            checked = data.enableCustomizeUser,
            enabled = !isDhizuku,
            onCheckedChange = { dispatch(EditViewAction.ChangeDataCustomizeUser(it)) },
        )
        if (data.enableCustomizeUser && !isDhizuku) {
            val users = state.availableUsers
            val userIds = users.keys.toList()
            choice(
                key = "target_user",
                title = s(R.string.config_target_user),
                options = users.values.map { TvChoiceOption(it) },
                selectedIndex = userIds.indexOf(data.targetUserId),
                onSelect = { index -> userIds.getOrNull(index)?.let { dispatch(EditViewAction.ChangeDataTargetUserId(it)) } },
                description = users[data.targetUserId] ?: s(R.string.config_user_not_found),
            )
        }

        val installReasonExpanded = !labRespect && data.enableCustomizeInstallReason
        switch(
            key = "customize_install_reason",
            title = s(R.string.config_customize_install_reason),
            summary = s(
                if (!labRespect) R.string.config_customize_install_reason_desc else R.string.config_customize_install_reason_disabled_desc,
            ),
            icon = AppIcons.InstallReason,
            checked = installReasonExpanded,
            enabled = !labRespect,
            onCheckedChange = { dispatch(EditViewAction.ChangeDataEnableCustomizeInstallReason(it)) },
        )
        if (installReasonExpanded) {
            enumChoice(
                key = "install_reason",
                title = s(R.string.config_install_reason),
                entries = listOf(
                    InstallReason.UNKNOWN to s(R.string.config_install_reason_unknown),
                    InstallReason.POLICY to s(R.string.config_install_reason_policy),
                    InstallReason.DEVICE_RESTORE to s(R.string.config_install_reason_device_restore),
                    InstallReason.DEVICE_SETUP to s(R.string.config_install_reason_device_setup),
                    InstallReason.USER to s(R.string.config_install_reason_user),
                ),
                current = data.installReason,
            ) { dispatch(EditViewAction.ChangeDataInstallReason(it)) }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            switch(
                key = "customize_package_source",
                title = s(R.string.config_customize_package_source),
                summary = s(
                    if (labRespect) R.string.config_customize_package_source_disabled_desc else R.string.config_customize_package_source_desc,
                ),
                icon = AppIcons.InstallPackageSource,
                checked = data.enableCustomizePackageSource,
                enabled = !labRespect,
                onCheckedChange = { dispatch(EditViewAction.ChangeDataEnableCustomizePackageSource(it)) },
            )
            if (!labRespect && data.enableCustomizePackageSource) {
                enumChoice(
                    key = "package_source",
                    title = s(R.string.config_package_source),
                    entries = listOf(
                        PackageSource.UNSPECIFIED to s(R.string.config_package_source_unspecified),
                        PackageSource.OTHER to s(R.string.config_package_source_other),
                        PackageSource.STORE to s(R.string.config_package_source_store),
                        PackageSource.LOCAL_FILE to s(R.string.config_package_source_local_file),
                        PackageSource.DOWNLOADED_FILE to s(R.string.config_package_source_downloaded_file),
                    ),
                    current = data.packageSource,
                ) { dispatch(EditViewAction.ChangeDataPackageSource(it)) }
            }
        }

        val requesterEntries = listOf(
            InstallRequesterMode.Disable to s(R.string.config_install_requester_mode_disable),
            InstallRequesterMode.Initiator to s(R.string.config_installer_mode_initiator),
            InstallRequesterMode.Custom to s(R.string.config_installer_mode_custom),
        )
        enumChoice(
            key = "install_requester_mode",
            title = s(R.string.config_declare_install_requester),
            entries = requesterEntries,
            current = data.installRequesterMode,
            icon = AppIcons.InstallSource,
            description = if (data.installRequesterMode == InstallRequesterMode.Custom) {
                s(
                    if (data.errorInstallRequester) {
                        R.string.config_declare_install_requester_error_desc
                    } else {
                        R.string.config_declare_install_requester_desc
                    },
                )
            } else {
                null
            },
        ) { dispatch(EditViewAction.ChangeDataInstallRequesterMode(it)) }
        if (data.installRequesterMode == InstallRequesterMode.Custom) {
            val requester = data.installRequester
            val uid = data.installRequesterUid
            textInput(
                key = "install_requester",
                title = s(R.string.config_install_requester),
                value = requester,
                onValueChange = { dispatch(EditViewAction.ChangeDataInstallRequester(it)) },
                icon = AppIcons.InstallSourceInput,
                description = when {
                    requester.isEmpty() -> s(R.string.config_error_package_name_empty)
                    uid != null -> "UID: $uid"
                    else -> s(R.string.config_error_package_not_found)
                },
            )
        }

        enumChoice(
            key = "installer_mode",
            title = s(R.string.config_declare_installer),
            entries = listOf(
                InstallerMode.Self to s(R.string.config_installer_mode_self),
                InstallerMode.Initiator to s(R.string.config_installer_mode_initiator),
                InstallerMode.Custom to s(R.string.config_installer_mode_custom),
            ),
            current = data.installerMode,
            icon = AppIcons.InstallSource,
            enabled = !isDhizuku,
            description = if (isDhizuku) {
                s(R.string.dhizuku_unsupported_desc)
            } else {
                when (data.installerMode) {
                    InstallerMode.Self -> s(
                        if (isSystemPackageInstallerActive(data.authorizer, state.globalAuthorizer, capabilityProvider.isSystemApp)) {
                            R.string.config_declare_installer_system_default_desc
                        } else {
                            R.string.config_declare_installer_desc
                        },
                    )

                    InstallerMode.Initiator -> s(R.string.config_installer_mode_initiator)

                    InstallerMode.Custom -> s(R.string.config_installer_mode_custom)
                }
            },
        ) { if (!isDhizuku) dispatch(EditViewAction.ChangeDataInstallerMode(it)) }
        if (data.installerMode == InstallerMode.Custom && !isDhizuku) {
            val managed = state.managedInstallerPackages
            val matching = managed.find { it.packageName == data.installer }
            textInput(
                key = "installer",
                title = s(R.string.config_installer),
                value = data.installer,
                onValueChange = { dispatch(EditViewAction.ChangeDataInstaller(it)) },
                icon = AppIcons.InstallSourceInput,
                description = when {
                    data.errorInstaller -> s(R.string.config_error_installer)
                    matching != null -> context.getString(R.string.config_installer_matches, matching.name)
                    else -> null
                },
            )
            // The phone field's suggestion dropdown becomes a picker of the managed install sources.
            if (managed.isEmpty()) {
                info(key = "installer_presets_empty", title = s(R.string.config_no_managed_packages_to_suggest))
            } else {
                choice(
                    key = "installer_presets",
                    title = s(R.string.config_managed_installer_packages_title),
                    options = managed.map { TvChoiceOption("${it.name} (${it.packageName})") },
                    selectedIndex = managed.indexOfFirst { it.packageName == data.installer },
                    onSelect = { index ->
                        managed.getOrNull(index)?.let { dispatch(EditViewAction.ChangeDataInstaller(it.packageName)) }
                    },
                )
            }
        }

        switch(
            key = "manual_dexopt",
            title = s(R.string.config_manual_dexopt),
            summary = dhizukuAware(R.string.config_manual_dexopt_desc),
            checked = data.enableManualDexopt,
            enabled = !isDhizuku,
            onCheckedChange = { dispatch(EditViewAction.ChangeDataEnableManualDexopt(it)) },
        )
        if (data.enableManualDexopt && !isDhizuku) {
            switch(
                key = "force_dexopt",
                title = s(R.string.config_force_dexopt),
                summary = s(R.string.config_force_dexopt_desc),
                checked = data.forceDexopt,
                onCheckedChange = { dispatch(EditViewAction.ChangeDataForceDexopt(it)) },
            )
            enumChoice(
                key = "dexopt_mode",
                title = s(R.string.config_dexopt_mode),
                entries = listOf(
                    DexoptMode.Verify to s(R.string.config_dexopt_mode_verify),
                    DexoptMode.SpeedProfile to s(R.string.config_dexopt_mode_speed_profile),
                    DexoptMode.Speed to s(R.string.config_dexopt_mode_speed),
                    DexoptMode.Everything to s(R.string.config_dexopt_mode_everything),
                ),
                current = data.dexoptMode,
            ) { dispatch(EditViewAction.ChangeDataDexoptMode(it)) }
        }

        switch(
            key = "auto_delete",
            title = s(R.string.config_auto_delete),
            summary = s(R.string.config_auto_delete_desc),
            icon = AppIcons.Delete,
            checked = data.autoDelete,
            onCheckedChange = { dispatch(EditViewAction.ChangeDataAutoDelete(it)) },
        )
        if (data.autoDelete) {
            switch(
                key = "auto_delete_zip",
                title = s(R.string.config_auto_delete_zip),
                summary = s(R.string.config_auto_delete_zip_desc),
                checked = data.autoDeleteZip,
                onCheckedChange = { dispatch(EditViewAction.ChangeDataZipAutoDelete(it)) },
            )
        }
        switch(
            key = "display_sdk",
            title = s(R.string.config_display_sdk_version),
            summary = s(R.string.config_display_sdk_version_desc),
            icon = AppIcons.Info,
            checked = data.displaySdk,
            onCheckedChange = { dispatch(EditViewAction.ChangeDisplaySdk(it)) },
        )
        switch(
            key = "display_size",
            title = s(R.string.config_display_size),
            summary = s(R.string.config_display_size_desc),
            icon = AppIcons.ShowSize,
            checked = data.displaySize,
            onCheckedChange = { dispatch(EditViewAction.ChangeDisplaySize(it)) },
        )

        // --- Install options ---
        header(s(R.string.config_label_install_options))
        switch(
            key = "for_all_user",
            title = s(R.string.config_all_users),
            summary = s(R.string.config_all_users_desc),
            icon = AppIcons.InstallForAllUsers,
            checked = data.forAllUser,
            onCheckedChange = { dispatch(EditViewAction.ChangeDataForAllUser(it)) },
        )
        switch(
            key = "allow_test_only",
            title = s(R.string.config_allow_test),
            summary = dhizukuAware(R.string.config_allow_test_desc),
            icon = AppIcons.BugReport,
            checked = data.allowTestOnly,
            enabled = !isDhizuku,
            onCheckedChange = { dispatch(EditViewAction.ChangeDataAllowTestOnly(it)) },
        )
        switch(
            key = "allow_downgrade",
            title = s(R.string.config_allow_downgrade),
            summary = dhizukuAware(R.string.config_allow_downgrade_desc),
            icon = AppIcons.InstallAllowDowngrade,
            checked = data.allowDowngrade,
            enabled = !isDhizuku,
            onCheckedChange = { dispatch(EditViewAction.ChangeDataAllowDowngrade(it)) },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            switch(
                key = "bypass_low_target_sdk",
                title = s(R.string.config_bypass_low_target_sdk),
                summary = dhizukuAware(R.string.config_bypass_low_target_sdk_desc),
                icon = AppIcons.InstallBypassLowTargetSdk,
                checked = data.bypassLowTargetSdk,
                enabled = !isDhizuku,
                onCheckedChange = { dispatch(EditViewAction.ChangeDataBypassLowTargetSdk(it)) },
            )
        }
        switch(
            key = "grant_all_permissions",
            title = s(R.string.config_grant_all_permissions),
            summary = dhizukuAware(R.string.config_grant_all_permissions_desc),
            icon = AppIcons.InstallAllowAllRequestedPermissions,
            checked = data.allowAllRequestedPermissions,
            enabled = !isDhizuku,
            onCheckedChange = { dispatch(EditViewAction.ChangeDataAllowAllRequestedPermissions(it)) },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            switch(
                key = "request_update_ownership",
                title = s(R.string.config_request_update_ownership),
                summary = dhizukuAware(R.string.config_request_update_ownership_desc),
                icon = AppIcons.InstallRequestUpdateOwnership,
                checked = data.requestUpdateOwnership,
                enabled = !isDhizuku,
                onCheckedChange = { dispatch(EditViewAction.ChangeDataRequestUpdateOwnership(it)) },
            )
        }

        // --- Preferences ---
        header(s(R.string.config_label_preferences))
        switch(
            key = "split_choose_all",
            title = s(R.string.config_split_choose_all),
            summary = s(R.string.config_split_choose_all_desc),
            icon = AppIcons.InstallSplitChooseAll,
            checked = data.splitChooseAll,
            onCheckedChange = { dispatch(EditViewAction.ChangeSplitChooseAll(it)) },
        )
        switch(
            key = "apk_choose_all",
            title = s(R.string.config_apk_choose_all),
            summary = s(R.string.config_apk_choose_all_desc),
            icon = AppIcons.InstallApkChooseAll,
            checked = data.apkChooseAll,
            onCheckedChange = { dispatch(EditViewAction.ChangeApkChooseAll(it)) },
        )
        if (state.checkAppSignature) {
            // Both switches are inverted on purpose, exactly as on the phone: "on" means the check is enforced.
            switch(
                key = "allow_sig_mismatch",
                title = s(R.string.config_allow_sig_mismatch),
                summary = s(R.string.config_allow_sig_mismatch_desc),
                icon = AppIcons.InstallAllowSigMismatch,
                checked = !data.allowSigMismatch,
                onCheckedChange = { dispatch(EditViewAction.ChangeDataAllowSigMismatch(!it)) },
            )
            switch(
                key = "allow_sig_unknown",
                title = s(R.string.config_allow_sig_unknown),
                summary = buildString {
                    append(s(R.string.config_allow_sig_unknown_desc))
                    if (state.allowInternetAccess) {
                        append('\n')
                        append(s(R.string.config_allow_sig_unknown_http_lightweight_desc))
                    }
                },
                icon = AppIcons.InstallAllowSigUnknown,
                checked = !data.allowSigUnknown,
                onCheckedChange = { dispatch(EditViewAction.ChangeDataAllowSigUnknown(!it)) },
            )
        }
    }
}

/** A [TvSettingsListScope.choice] over typed values; the description defaults to the selected label. */
private fun <T> TvSettingsListScope.enumChoice(
    key: String,
    title: String,
    entries: List<Pair<T, String>>,
    current: T,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    description: String? = null,
    onSelect: (T) -> Unit,
) {
    val index = entries.indexOfFirst { it.first == current }
    choice(
        key = key,
        title = title,
        options = entries.map { TvChoiceOption(it.second) },
        selectedIndex = index,
        onSelect = { selected -> entries.getOrNull(selected)?.let { onSelect(it.first) } },
        icon = icon,
        enabled = enabled,
        description = description ?: entries.getOrNull(index)?.second,
    )
}
