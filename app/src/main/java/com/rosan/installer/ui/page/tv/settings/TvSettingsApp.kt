// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rosan.installer.R
import com.rosan.installer.domain.settings.repository.ConfigRepository
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.settings.home.HomePageViewAction
import com.rosan.installer.ui.page.main.settings.home.HomePageViewModel
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewAction
import com.rosan.installer.ui.page.main.settings.preferred.PreferredViewModel
import com.rosan.installer.ui.page.tv.components.TvBackdrop
import com.rosan.installer.ui.page.tv.settings.pages.TvAboutPage
import com.rosan.installer.ui.page.tv.settings.pages.TvAuthorizerCustPage
import com.rosan.installer.ui.page.tv.settings.pages.TvDefaultInstallerPage
import com.rosan.installer.ui.page.tv.settings.pages.TvDialogSettingsPage
import com.rosan.installer.ui.page.tv.settings.pages.TvHistoryPage
import com.rosan.installer.ui.page.tv.settings.pages.TvInstallerSettingsPage
import com.rosan.installer.ui.page.tv.settings.pages.TvLabPage
import com.rosan.installer.ui.page.tv.settings.pages.TvNetworkPage
import com.rosan.installer.ui.page.tv.settings.pages.TvNotificationSettingsPage
import com.rosan.installer.ui.page.tv.settings.pages.TvPrivilegesPage
import com.rosan.installer.ui.page.tv.settings.pages.TvProfileApplyPage
import com.rosan.installer.ui.page.tv.settings.pages.TvProfileEditPage
import com.rosan.installer.ui.page.tv.settings.pages.TvProfilesPage
import com.rosan.installer.ui.page.tv.settings.pages.TvThemePage
import com.rosan.installer.ui.page.tv.settings.pages.TvUninstallerSettingsPage
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Screens of the TV settings app. */
sealed interface TvSettingsDest {
    data object Root : TvSettingsDest
    data object Profiles : TvSettingsDest
    data class ProfileEdit(val id: Long?) : TvSettingsDest
    data class ProfileApply(val id: Long) : TvSettingsDest
    data object History : TvSettingsDest
    data object Theme : TvSettingsDest
    data object Installer : TvSettingsDest
    data object DialogSettings : TvSettingsDest
    data object NotificationSettings : TvSettingsDest
    data object AuthorizerCust : TvSettingsDest
    data object Uninstaller : TvSettingsDest
    data object Network : TvSettingsDest
    data object Lab : TvSettingsDest
    data object About : TvSettingsDest
    data object DefaultInstaller : TvSettingsDest
    data object Privileges : TvSettingsDest
}

/** Back stack for the TV settings app; Back pops until the root, then leaves the app. */
class TvSettingsNavigator {
    val stack = mutableStateListOf<TvSettingsDest>(TvSettingsDest.Root)
    val current: TvSettingsDest get() = stack.last()

    /** Saved-state key of the top entry, unique per back-stack position. */
    val currentKey: String get() = entryKey(stack.lastIndex)

    internal var onEntryRemoved: (String) -> Unit = {}

    fun push(dest: TvSettingsDest) {
        stack.add(dest)
    }

    fun pop() {
        if (stack.size > 1) {
            val key = entryKey(stack.lastIndex)
            stack.removeAt(stack.lastIndex)
            onEntryRemoved(key)
        }
    }

    private fun entryKey(index: Int) = "$index:" + stack[index]
}

val LocalTvSettingsNavigator = staticCompositionLocalOf<TvSettingsNavigator> { error("No TvSettingsNavigator provided") }

/**
 * The settings app on TV, laid out like Google TV's system settings. Pages read and write the same
 * view models as the phone settings, but render only TV components.
 */
@Composable
fun TvSettingsApp() {
    val navigator = remember { TvSettingsNavigator() }
    // Pages below the top keep their scroll and focus; a popped page starts fresh next time.
    val stateHolder = rememberSaveableStateHolder()
    navigator.onEntryRemoved = { stateHolder.removeState(it) }
    BackHandler(enabled = navigator.stack.size > 1) { navigator.pop() }
    CompositionLocalProvider(LocalTvSettingsNavigator provides navigator) {
        TvBackdrop(accent = null) {
            AnimatedContent(
                targetState = navigator.currentKey to navigator.current,
                transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                label = "tv_settings_page",
            ) { (entryKey, dest) ->
                stateHolder.SaveableStateProvider(entryKey) {
                    val pageMemory = rememberSaveable(saver = TvPageMemory.Saver) { TvPageMemory() }
                    CompositionLocalProvider(LocalTvPageMemory provides pageMemory) { TvSettingsDestination(dest) }
                }
            }
        }
    }
}

@Composable
private fun TvSettingsDestination(dest: TvSettingsDest) {
    when (dest) {
        TvSettingsDest.Root -> TvSettingsRootPage()
        TvSettingsDest.Profiles -> TvProfilesPage()
        is TvSettingsDest.ProfileEdit -> TvProfileEditPage(dest.id)
        is TvSettingsDest.ProfileApply -> TvProfileApplyPage(dest.id)
        TvSettingsDest.History -> TvHistoryPage()
        TvSettingsDest.Theme -> TvThemePage()
        TvSettingsDest.Installer -> TvInstallerSettingsPage()
        TvSettingsDest.DialogSettings -> TvDialogSettingsPage()
        TvSettingsDest.NotificationSettings -> TvNotificationSettingsPage()
        TvSettingsDest.AuthorizerCust -> TvAuthorizerCustPage()
        TvSettingsDest.Uninstaller -> TvUninstallerSettingsPage()
        TvSettingsDest.Network -> TvNetworkPage()
        TvSettingsDest.Lab -> TvLabPage()
        TvSettingsDest.About -> TvAboutPage()
        TvSettingsDest.DefaultInstaller -> TvDefaultInstallerPage()
        TvSettingsDest.Privileges -> TvPrivilegesPage()
    }
}

@Composable
private fun TvSettingsRootPage(
    homeViewModel: HomePageViewModel = koinViewModel(),
    preferredViewModel: PreferredViewModel = koinViewModel(),
) {
    val navigator = LocalTvSettingsNavigator.current
    val home by homeViewModel.state.collectAsStateWithLifecycle()
    val preferred by preferredViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val configRepo = koinInject<ConfigRepository>()
    val profileCountFlow = remember { configRepo.flowAll().map { it.size } }
    val profileCount by profileCountFlow.collectAsStateWithLifecycle(initialValue = 0)

    LaunchedEffect(Unit) {
        homeViewModel.dispatch(HomePageViewAction.RefreshActivateStatus)
        preferredViewModel.dispatch(PreferredViewAction.RefreshIgnoreBatteryOptimizationStatus)
    }

    val statusTitle = stringResource(
        when {
            home.isSystemApp -> R.string.working_status_system_installer
            home.isDefaultInstaller -> R.string.working_status_default_installer
            else -> R.string.working_status_not_default_installer
        },
    )
    val statusDescription = stringResource(
        when {
            home.isSystemApp -> R.string.working_status_system_installer_desc
            home.isDefaultInstaller -> R.string.working_status_default_installer_desc
            else -> R.string.working_status_not_default_installer_click_details
        },
    )

    TvSettingsPage(title = stringResource(R.string.app_name)) {
        header(context.getString(R.string.home))
        item(
            key = "status",
            title = statusTitle,
            summary = statusDescription,
            icon = if (home.isDefaultInstaller || home.isSystemApp) AppIcons.Active else AppIcons.UnlockDefault,
            onClick = { navigator.push(TvSettingsDest.DefaultInstaller) },
        )
        item(
            key = "privileges",
            title = context.getString(R.string.home_stat_authorizers),
            summary = home.availableAuthorizerCount.toString(),
            icon = AppIcons.Authorizer,
            onClick = { navigator.push(TvSettingsDest.Privileges) },
        )
        item(
            key = "profiles",
            title = context.getString(R.string.config),
            summary = profileCount.toString(),
            icon = AppIcons.RoomPreferences,
            onClick = { navigator.push(TvSettingsDest.Profiles) },
        )
        item(
            key = "history",
            title = context.getString(R.string.history),
            icon = AppIcons.History,
            onClick = { navigator.push(TvSettingsDest.History) },
        )

        header(context.getString(R.string.personalization))
        item(
            key = "theme",
            title = context.getString(R.string.theme_settings),
            icon = AppIcons.Theme,
            onClick = { navigator.push(TvSettingsDest.Theme) },
        )
        item(
            key = "installer",
            title = context.getString(R.string.installer_settings),
            icon = AppIcons.InstallMode,
            onClick = { navigator.push(TvSettingsDest.Installer) },
        )
        item(
            key = "uninstaller",
            title = context.getString(R.string.uninstaller_settings),
            icon = AppIcons.Delete,
            onClick = { navigator.push(TvSettingsDest.Uninstaller) },
        )

        header(context.getString(R.string.basic))
        item(
            key = "network",
            title = context.getString(R.string.network_settings),
            icon = AppIcons.Network,
            onClick = { navigator.push(TvSettingsDest.Network) },
        )
        switch(
            key = "adb_verify",
            title = context.getString(R.string.disable_adb_install_verify),
            summary = context.getString(R.string.disable_adb_install_verify_desc),
            icon = AppIcons.DisableAdbVerify,
            checked = !preferred.adbVerifyEnabled,
            onCheckedChange = { preferredViewModel.dispatch(PreferredViewAction.SetAdbVerifyEnabledState(!it)) },
        )
        switch(
            key = "battery",
            title = context.getString(R.string.ignore_battery_optimizations),
            summary = context.getString(R.string.ignore_battery_optimizations_desc),
            icon = AppIcons.BatteryOptimization,
            checked = preferred.isIgnoringBatteryOptimizations,
            enabled = !preferred.isIgnoringBatteryOptimizations,
            onCheckedChange = { preferredViewModel.dispatch(PreferredViewAction.RequestIgnoreBatteryOptimization) },
        )
        // The launcher alias is the only TV launcher entry, so TV offers no way to hide it, only a
        // way to bring it back if it was hidden on another device or restored from a backup.
        if (!preferred.showLauncherIcon) {
            switch(
                key = "launcher_icon",
                title = context.getString(R.string.theme_settings_hide_launcher_icon),
                summary = context.getString(R.string.theme_settings_hide_launcher_icon_desc),
                icon = AppIcons.Launcher,
                checked = true,
                onCheckedChange = { hide -> if (!hide) preferredViewModel.dispatch(PreferredViewAction.ChangeShowLauncherIcon(true)) },
            )
        }

        header(context.getString(R.string.other))
        item(
            key = "lab",
            title = context.getString(R.string.lab),
            icon = AppIcons.Lab,
            onClick = { navigator.push(TvSettingsDest.Lab) },
        )
        item(
            key = "about",
            title = context.getString(R.string.about_detail),
            icon = AppIcons.Info,
            onClick = { navigator.push(TvSettingsDest.About) },
        )
    }
}
