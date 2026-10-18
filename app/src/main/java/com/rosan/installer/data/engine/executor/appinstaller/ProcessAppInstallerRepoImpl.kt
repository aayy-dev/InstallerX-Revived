// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2023-2026 iamr0s, InstallerX Revived contributors
package com.rosan.installer.data.engine.executor.appinstaller

import android.content.Context
import android.content.pm.IPackageManager
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.PackageManagerHidden
import android.os.Build
import android.os.IBinder
import android.os.Process as AndroidProcess
import com.rosan.installer.R
import com.rosan.installer.core.bitmask.hasFlag
import com.rosan.installer.core.reflection.ReflectionProvider
import com.rosan.installer.data.engine.executor.PackageInstallerUtil.installFlags
import com.rosan.installer.data.engine.executor.PackageInstallerUtil.installReason
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.exception.InstallException
import com.rosan.installer.domain.engine.model.error.InstallErrorType
import com.rosan.installer.domain.engine.model.install.InstallEntity
import com.rosan.installer.domain.engine.model.install.InstallMetadata
import com.rosan.installer.domain.engine.model.install.InstallPhase
import com.rosan.installer.domain.engine.model.install.InstallWriteProgress
import com.rosan.installer.domain.privileged.provider.PostInstallTaskProvider
import com.rosan.installer.domain.privileged.provider.SystemInfoProvider
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.ConfigModel
import com.rosan.installer.domain.settings.repository.AppSettingsRepository
import com.rosan.installer.domain.settings.repository.BooleanSetting
import com.rosan.installer.framework.privileged.core.execution.authorization.requireCustomizeAuthorizer
import com.rosan.installer.framework.privileged.core.infrastructure.process.AppProcessTerminal
import com.rosan.installer.framework.privileged.core.infrastructure.process.SHELL_SH
import com.rosan.installer.framework.privileged.core.infrastructure.process.ShellCommand
import com.rosan.installer.framework.privileged.core.infrastructure.recycler.ProcessHookRecycler
import com.rosan.installer.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class ProcessAppInstallerRepoImpl(
    context: Context,
    reflect: ReflectionProvider,
    capabilityProvider: DeviceCapabilityProvider,
    postInstallTaskProvider: PostInstallTaskProvider,
    appSettingsRepo: AppSettingsRepository,
    private val systemInfoProvider: SystemInfoProvider,
) : IBinderAppInstallerRepoImpl(context, reflect, capabilityProvider, postInstallTaskProvider, appSettingsRepo) {
    private companion object {
        const val TAG = "ProcessAppInstaller"
    }

    private var localService: ProcessHookRecycler.HookedUserService? = null
    private var activeTerminal: AppProcessTerminal? = null

    private val isSystemUidSession: Boolean
        get() = activeTerminal == AppProcessTerminal.RootSystem

    override suspend fun doInstallWork(
        config: ConfigModel,
        entities: List<InstallEntity>,
        metadata: InstallMetadata,
        respectPlatformInstallPolicy: Boolean,
        blacklist: List<String>,
        sharedUserIdBlacklist: List<String>,
        sharedUserIdExemption: List<String>,
        onProgress: suspend (InstallWriteProgress) -> Unit,
        onPhaseChanged: suspend (InstallPhase) -> Unit,
    ) = runWithProcess(config, rootTerminal = resolveInstallRootTerminal()) {
        super.doInstallWork(
            config,
            entities,
            metadata,
            respectPlatformInstallPolicy,
            blacklist,
            sharedUserIdBlacklist,
            sharedUserIdExemption,
            onProgress,
            onPhaseChanged,
        )
    }

    override suspend fun doUninstallWork(config: ConfigModel, packageName: String) = runWithProcess(config) {
        super.doUninstallWork(config, packageName)
    }

    override suspend fun approveSession(config: ConfigModel, sessionId: Int, granted: Boolean) = runWithProcess(config) {
        super.approveSession(config, sessionId, granted)
    }

    override suspend fun iBinderWrapper(iBinder: IBinder): IBinder {
        val service = localService
            ?: throw IllegalStateException(
                "Service is null in iBinderWrapper. " +
                    "Make sure doInstallWork/doUninstallWork calls are properly scoped.",
            )

        return service.binderWrapper(iBinder)
    }

    override suspend fun doFinishWork(config: ConfigModel, entities: List<InstallEntity>, result: Result<Unit>) {
        super.doFinishWork(config, entities, result)
    }

    /**
     * Android only keeps INSTALL_DISABLE_VERIFICATION on non-adb sessions when the creating uid was
     * registered first. The registration is one-shot and consumed by the next createSession call, so
     * a concurrent install from another app can only make this session fall back to being verified.
     */
    override suspend fun beforeCreateSession(params: PackageInstaller.SessionParams) {
        if (!isSystemUidSession) return
        if (!params.installFlags.hasFlag(PackageManagerHidden.INSTALL_DISABLE_VERIFICATION)) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Timber.tag(TAG).w("Disable verification needs Android 14+ for system uid installs; verification stays enabled.")
            return
        }
        runCatching { resolvePackageInstallerBinder().disableVerificationForUid(AndroidProcess.SYSTEM_UID) }
            .onFailure { Timber.tag(TAG).w(it, "disableVerificationForUid failed; verification stays enabled.") }
    }

    /**
     * Android drops INSTALL_ALL_USERS from sessions created by the system uid. Enabling the installed
     * package for each remaining user keeps the install source recorded by this session. When the
     * session granted all requested permissions, mirror that for the other users too, since
     * installExistingPackageAsUser does not replay the install-time grant.
     */
    override suspend fun onPackageCommitted(
        config: ConfigModel,
        packageName: String,
        params: PackageInstaller.SessionParams,
        userId: Int,
    ) {
        if (!isSystemUidSession) return
        if (!params.installFlags.hasFlag(PackageManagerHidden.INSTALL_ALL_USERS)) return

        val otherUserIds = systemInfoProvider.getUsers(config.authorizer, config.customizeAuthorizer).keys - userId
        if (otherUserIds.isEmpty()) return

        val packageManager = runCatching { resolvePackageManagerBinder() }
            .onFailure { Timber.tag(TAG).w(it, "Cannot install $packageName for other users.") }
            .getOrNull()
        if (packageManager == null) {
            notifyPartialUserFailure(otherUserIds.size)
            return
        }

        val grantAll = params.installFlags.hasFlag(PackageManagerHidden.INSTALL_GRANT_ALL_REQUESTED_PERMISSIONS)
        val failedUserCount = otherUserIds.count { otherUserId ->
            runCatching {
                val result = installExistingPackage(packageManager, packageName, otherUserId, params.installReason)
                check(result == PackageManagerHidden.INSTALL_SUCCEEDED) { "installExistingPackageAsUser returned $result" }
                if (grantAll) grantInstalledPermissionsForUser(packageManager, packageName, fromUserId = userId, toUserId = otherUserId)
            }.onFailure { Timber.tag(TAG).w(it, "Installing $packageName for user $otherUserId failed") }.isFailure
        }
        if (failedUserCount > 0) notifyPartialUserFailure(failedUserCount)
    }

    /**
     * Grants the runtime permissions already granted on [fromUserId] to [toUserId]. Install-time
     * permissions reject grantRuntimePermission, so each grant is attempted independently.
     */
    private fun grantInstalledPermissionsForUser(packageManager: IPackageManager, packageName: String, fromUserId: Int, toUserId: Int) {
        val info = getPackageInfoForUser(packageManager, packageName, fromUserId) ?: return
        val permissions = info.requestedPermissions ?: return
        val flags = info.requestedPermissionsFlags ?: IntArray(permissions.size)
        permissions.forEachIndexed { index, permission ->
            val grantedForSource = index < flags.size && (flags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            if (!grantedForSource) return@forEachIndexed
            runCatching { packageManager.grantRuntimePermission(packageName, permission, toUserId) }
                .onFailure { Timber.tag(TAG).d("Skipped granting %s to user %d: %s", permission, toUserId, it.message) }
        }
    }

    private fun getPackageInfoForUser(packageManager: IPackageManager, packageName: String, userId: Int): PackageInfo? = runCatching {
        val flags = PackageManager.GET_PERMISSIONS.toLong()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, flags, userId)
        } else {
            packageManager.getPackageInfo(packageName, flags.toInt(), userId)
        }
    }.getOrNull()

    private suspend fun notifyPartialUserFailure(failedUserCount: Int) = withContext(Dispatchers.Main) {
        context.toast(R.string.lab_root_install_as_system_uid_partial_user_failure, failedUserCount)
    }

    /**
     * Reinstalling an identical static shared library version is only allowed for adb (root/shell)
     * sessions, so a duplicate-package rejection is retried once for this package as root (uid 0).
     */
    override suspend fun runPackageSession(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: InstallException) {
            if (!isSystemUidSession || e.errorType != InstallErrorType.DUPLICATE_PACKAGE) throw e
            Timber.tag(TAG).w(e, "System uid session was rejected as a duplicate package; retrying as root.")
            try {
                withTerminal(AppProcessTerminal.Root) { block() }
            } catch (retryError: Exception) {
                e.addSuppressed(retryError)
                throw e
            }
        }
    }

    private fun installExistingPackage(packageManager: IPackageManager, packageName: String, userId: Int, installReason: Int): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        packageManager.installExistingPackageAsUser(packageName, userId, 0, installReason, null)
    } else {
        packageManager.installExistingPackageAsUser(packageName, userId, 0, installReason)
    }

    /**
     * PackageInstallerService always records com.android.shell as the install initiator when the
     * session is created by root (uid 0) or shell (uid 2000). Creating it from the system uid instead
     * lets the resolved installer package be recorded as the initiator too.
     *
     * Only installs use this: uninstall still requires a caller that owns its callerPackageName,
     * and some ROMs (e.g. Xiaomi) reject installs from the system uid, so it stays opt-in.
     */
    private suspend fun resolveInstallRootTerminal(): AppProcessTerminal {
        val installAsSystemUid = appSettingsRepo.getBoolean(BooleanSetting.LabRootInstallAsSystemUid).first()
        return if (installAsSystemUid) AppProcessTerminal.RootSystem else AppProcessTerminal.Root
    }

    private suspend fun <T> runWithProcess(
        config: ConfigModel,
        rootTerminal: AppProcessTerminal = AppProcessTerminal.Root,
        block: suspend () -> T,
    ): T {
        val terminal = when (config.authorizer) {
            Authorizer.Root -> rootTerminal

            Authorizer.Customize -> AppProcessTerminal.Customize(
                ShellCommand.parse(requireCustomizeAuthorizer(config.customizeAuthorizer)),
            )

            else -> AppProcessTerminal.Customize(ShellCommand.of(SHELL_SH))
        }

        return withTerminal(terminal, block)
    }

    private suspend fun <T> withTerminal(terminal: AppProcessTerminal, block: suspend () -> T): T {
        val handle = GlobalContext.get().get<ProcessHookRecycler> { parametersOf(terminal) }.make()
        val previousService = localService
        val previousTerminal = activeTerminal

        return try {
            localService = handle.entity
            activeTerminal = terminal
            block()
        } finally {
            localService = previousService
            activeTerminal = previousTerminal
            handle.recycle()
        }
    }
}
