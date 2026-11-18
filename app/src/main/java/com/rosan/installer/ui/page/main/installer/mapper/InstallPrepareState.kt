// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.installer.mapper

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.rosan.installer.R
import com.rosan.installer.core.device.model.Manufacturer
import com.rosan.installer.core.env.DeviceConfig
import com.rosan.installer.domain.device.provider.DeviceCapabilityProvider
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.packageinfo.PackageAnalysisResult
import com.rosan.installer.domain.engine.model.packageinfo.sortedBest
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.engine.usecase.AnalyzeInstallStateUseCase
import com.rosan.installer.ui.page.main.installer.InstallerViewSettings
import org.koin.compose.koinInject

/**
 * The entities an install-prepare screen acts on, shared by every installer UI.
 */
data class InstallPrepareTarget(
    val currentPackage: PackageAnalysisResult,
    val selectedEntities: List<AppEntity>,
    val primaryEntity: AppEntity,
    val entityToInstall: AppEntity.BaseEntity?,
    val containerType: DataType?,
    val isSplitUpdateMode: Boolean,
)

/**
 * Resolves what the prepare screen installs, or null when nothing is selected.
 */
fun resolveInstallPrepareTarget(currentPackage: PackageAnalysisResult): InstallPrepareTarget? {
    val allAvailableApps = currentPackage.appEntities.map { it.app }

    val selectedEntities = currentPackage.appEntities.filter { it.selected }.map { it.app }
    if (selectedEntities.isEmpty()) return null

    val primaryEntity = selectedEntities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
        ?: selectedEntities.filterIsInstance<AppEntity.ModuleEntity>().firstOrNull()
        ?: selectedEntities.filterIsInstance<AppEntity.SplitEntity>().firstOrNull()
        ?: selectedEntities.firstOrNull()
        ?: allAvailableApps.sortedBest().firstOrNull()
        ?: return null

    val entityToInstall = selectedEntities.filterIsInstance<AppEntity.BaseEntity>().firstOrNull()
    val preInstallAppInfo = currentPackage.installedAppInfo

    val isPureSplit = primaryEntity is AppEntity.SplitEntity
    val isBundleSplitUpdate = primaryEntity is AppEntity.BaseEntity &&
        entityToInstall == null &&
        selectedEntities.isNotEmpty()

    return InstallPrepareTarget(
        currentPackage = currentPackage,
        selectedEntities = selectedEntities,
        primaryEntity = primaryEntity,
        entityToInstall = entityToInstall,
        containerType = primaryEntity.sourceType,
        isSplitUpdateMode = (isBundleSplitUpdate || isPureSplit) && preInstallAppInfo != null,
    )
}

/**
 * Runs the install-state analysis for [target] and maps it to notices and the primary button label.
 */
@Composable
fun rememberInstallStateResult(target: InstallPrepareTarget, settings: InstallerViewSettings): InstallStateResult {
    val currentPackage = target.currentPackage
    val primaryEntity = target.primaryEntity
    val entityToInstall = target.entityToInstall
    val containerType = target.containerType
    val isSplitUpdateMode = target.isSplitUpdateMode

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val tagDowngrade = stringResource(R.string.tag_downgrade)
    val downgradeWarning = stringResource(R.string.installer_prepare_type_downgrade)
    val tagSignature = stringResource(R.string.tag_signature)
    val tagSignatureMatch = stringResource(R.string.tag_signature_match)
    val tagSignatureRotation = stringResource(R.string.tag_signature_rotation)
    val tagSignatureRotationUnconfirmed = stringResource(R.string.tag_signature_rotation_unconfirmed)
    val tagAospSignature = stringResource(R.string.tag_aosp_signature)
    val textAospSignature = stringResource(R.string.installer_prepare_aosp_signature)
    val sigNewInstall = stringResource(R.string.installer_prepare_signature_new_install)
    val sigMatch = stringResource(R.string.installer_prepare_signature_match)
    val sigRotationCompatible = stringResource(R.string.installer_prepare_signature_rotation_compatible)
    val sigCandidateRotationUnconfirmed =
        stringResource(R.string.installer_prepare_signature_candidate_rotation_unconfirmed)
    val sigMismatchWarning = stringResource(R.string.installer_prepare_signature_mismatch)
    val sigUnknownWarning = stringResource(R.string.installer_prepare_signature_unknown)
    val sigAnalysisIssue = stringResource(R.string.installer_prepare_signature_analysis_issue)
    val sigSigningBlockNotInstalled =
        stringResource(R.string.installer_prepare_signature_signing_block_not_installed)
    val sigSigningBlockMatch = stringResource(R.string.installer_prepare_signature_signing_block_match)
    val sigSigningBlockUnknown = stringResource(R.string.installer_prepare_signature_signing_block_unknown)
    val labelPendingSignature = stringResource(R.string.installer_signature_pending_package)
    val labelInstalledSignature = stringResource(R.string.installer_signature_installed_package)
    val labelSignatureAnalysisIssues = stringResource(R.string.installer_signature_analysis_issues)
    val labelSignatureVerificationFailedFiles = stringResource(R.string.installer_signature_verification_failed_files)
    val labelSignatureSplitMismatchFiles = stringResource(R.string.installer_signature_split_mismatch_files)
    val labelSignatureDuplicateSplitNames = stringResource(R.string.installer_signature_duplicate_split_names)
    val labelSignatureSchemes = stringResource(R.string.installer_signature_schemes)
    val labelSignatureDeclaredSchemes = stringResource(R.string.installer_signature_declared_schemes)
    val labelSignatureCertificate = stringResource(R.string.installer_signature_certificate)
    val labelSignatureCurrentCertificate = stringResource(R.string.installer_signature_current_certificate)
    val labelSignatureCertificateLineage = stringResource(R.string.installer_signature_certificate_lineage)
    val labelSignatureLineageCertificate = stringResource(R.string.installer_signature_lineage_certificate)
    val labelSignatureCurrentMarker = stringResource(R.string.installer_signature_current_marker)
    val labelSignatureSha256 = stringResource(R.string.installer_signature_sha256)
    val labelSignatureSourceStampSha256 = stringResource(R.string.installer_signature_source_stamp_sha256)
    val labelSignatureSourceStamp = stringResource(R.string.installer_signature_source_stamp)
    val labelSignatureSourceStampStatus = stringResource(R.string.installer_signature_source_stamp_status)
    val textSourceStampUnverified = stringResource(R.string.installer_signature_source_stamp_unverified)
    val textSourceStampVerified = stringResource(R.string.installer_signature_source_stamp_verified)
    val textSourceStampFailed = stringResource(R.string.installer_signature_source_stamp_failed)
    val textSourceStampCertificateMismatch = stringResource(R.string.installer_signature_source_stamp_certificate_mismatch)
    val textSourceStampVerificationError = stringResource(R.string.installer_signature_source_stamp_verification_error)
    val labelSignatureSourceStampTimestamp = stringResource(R.string.installer_signature_source_stamp_timestamp)
    val labelSignatureSourceStampCertificate = stringResource(R.string.installer_signature_source_stamp_certificate)
    val labelSignatureSourceStampLineage = stringResource(R.string.installer_signature_source_stamp_lineage)
    val labelSignatureInformation = stringResource(R.string.installer_signature_information)
    val labelSignatureSha1 = stringResource(R.string.installer_signature_sha1)
    val labelSignatureSubject = stringResource(R.string.installer_signature_subject)
    val labelSignatureIssuer = stringResource(R.string.installer_signature_issuer)
    val labelSignatureValidFrom = stringResource(R.string.installer_signature_valid_from)
    val labelSignatureValidUntil = stringResource(R.string.installer_signature_valid_until)
    val labelSignaturePublicKeyAlgorithm = stringResource(R.string.installer_signature_public_key_algorithm)
    val labelSignatureAlgorithm = stringResource(R.string.installer_signature_algorithm)
    val labelSignatureWarnings = stringResource(R.string.installer_signature_warnings)
    val labelSignatureErrors = stringResource(R.string.installer_signature_errors)
    val labelSignatureNoCertificates = stringResource(R.string.installer_signature_no_certificates)
    val tagSdk = stringResource(R.string.tag_sdk)
    val sdkIncompatibleWarning = stringResource(R.string.installer_prepare_sdk_incompatible)
    val tagOplusOsdk = stringResource(R.string.tag_oplus_osdk)
    val oplusOsdkIncompatibleWarning = stringResource(R.string.installer_prepare_oplus_osdk_incompatible)
    val tagArch32 = stringResource(R.string.tag_arch_32)
    val textArch32 = stringResource(R.string.installer_prepare_arch_32_notice)
    val tagEmulated = stringResource(R.string.tag_arch_emulated)
    val textArchMismatch = stringResource(R.string.installer_prepare_arch_mismatch_notice)
    val tagIdentical = stringResource(R.string.tag_identical)
    val textIdentical = stringResource(R.string.installer_prepare_identical_notice)
    val tagXposed = stringResource(R.string.tag_xposed)
    val labelXposedMinApi = stringResource(R.string.installer_xposed_min_api)
    val labelXposedTargetApi = stringResource(R.string.installer_xposed_target_api)

    val labelSignatureHybridClassical = stringResource(R.string.installer_signature_hybrid_classical)
    val labelSignatureHybridPqc = stringResource(R.string.installer_signature_hybrid_pqc)

    val installResources = remember(primaryColor, errorColor, tertiaryColor, tagAospSignature, textAospSignature) {
        InstallNoticeResources(
            tagDowngrade = tagDowngrade,
            textDowngrade = downgradeWarning,
            tagSignature = tagSignature,
            tagSignatureMatch = tagSignatureMatch,
            tagSignatureRotation = tagSignatureRotation,
            tagSignatureRotationUnconfirmed = tagSignatureRotationUnconfirmed,
            tagAospSignature = tagAospSignature,
            textAospSignature = textAospSignature,
            textSigNewInstall = sigNewInstall,
            textSigMatch = sigMatch,
            textSigRotationCompatible = sigRotationCompatible,
            textSigCandidateRotationUnconfirmed = sigCandidateRotationUnconfirmed,
            textSigMismatch = sigMismatchWarning,
            textSigUnknown = sigUnknownWarning,
            textSigAnalysisIssue = sigAnalysisIssue,
            textSigSigningBlockNotInstalled = sigSigningBlockNotInstalled,
            textSigSigningBlockMatch = sigSigningBlockMatch,
            textSigSigningBlockUnknown = sigSigningBlockUnknown,
            labelPendingSignature = labelPendingSignature,
            labelInstalledSignature = labelInstalledSignature,
            labelSignatureAnalysisIssues = labelSignatureAnalysisIssues,
            labelSignatureVerificationFailedFiles = labelSignatureVerificationFailedFiles,
            labelSignatureSplitMismatchFiles = labelSignatureSplitMismatchFiles,
            labelSignatureDuplicateSplitNames = labelSignatureDuplicateSplitNames,
            labelSignatureSchemes = labelSignatureSchemes,
            labelSignatureDeclaredSchemes = labelSignatureDeclaredSchemes,
            labelSignatureCertificate = labelSignatureCertificate,
            labelSignatureCurrentCertificate = labelSignatureCurrentCertificate,
            labelSignatureCertificateLineage = labelSignatureCertificateLineage,
            labelSignatureLineageCertificate = labelSignatureLineageCertificate,
            labelSignatureCurrentMarker = labelSignatureCurrentMarker,
            labelSignatureHybridClassical = labelSignatureHybridClassical,
            labelSignatureHybridPqc = labelSignatureHybridPqc,
            labelSignatureSha256 = labelSignatureSha256,
            labelSignatureSourceStampSha256 = labelSignatureSourceStampSha256,
            labelSignatureSourceStamp = labelSignatureSourceStamp,
            labelSignatureSourceStampStatus = labelSignatureSourceStampStatus,
            textSourceStampUnverified = textSourceStampUnverified,
            textSourceStampVerified = textSourceStampVerified,
            textSourceStampFailed = textSourceStampFailed,
            textSourceStampCertificateMismatch = textSourceStampCertificateMismatch,
            textSourceStampVerificationError = textSourceStampVerificationError,
            labelSignatureSourceStampTimestamp = labelSignatureSourceStampTimestamp,
            labelSignatureSourceStampCertificate = labelSignatureSourceStampCertificate,
            labelSignatureSourceStampLineage = labelSignatureSourceStampLineage,
            labelSignatureInformation = labelSignatureInformation,
            labelSignatureSha1 = labelSignatureSha1,
            labelSignatureSubject = labelSignatureSubject,
            labelSignatureIssuer = labelSignatureIssuer,
            labelSignatureValidFrom = labelSignatureValidFrom,
            labelSignatureValidUntil = labelSignatureValidUntil,
            labelSignaturePublicKeyAlgorithm = labelSignaturePublicKeyAlgorithm,
            labelSignatureAlgorithm = labelSignatureAlgorithm,
            labelSignatureWarnings = labelSignatureWarnings,
            labelSignatureErrors = labelSignatureErrors,
            labelSignatureNoCertificates = labelSignatureNoCertificates,
            tagSdk = tagSdk,
            textSdkIncompatible = sdkIncompatibleWarning,
            tagOplusOsdk = tagOplusOsdk,
            textOplusOsdkIncompatible = oplusOsdkIncompatibleWarning,
            tagArch32 = tagArch32,
            textArch32 = textArch32,
            tagEmulated = tagEmulated,
            textArchMismatchFormat = textArchMismatch,
            tagIdentical = tagIdentical,
            textIdentical = textIdentical,
            tagXposed = tagXposed,
            labelXposedMinApi = labelXposedMinApi,
            labelXposedTargetApi = labelXposedTargetApi,
            errorColor = errorColor,
            tertiaryColor = tertiaryColor,
            primaryColor = primaryColor,
        )
    }

    // Inject the pure domain use case
    val analyzeInstallStateUseCase = koinInject<AnalyzeInstallStateUseCase>()
    val deviceCapabilityProvider = koinInject<DeviceCapabilityProvider>()
    val checkOplusOsdk = settings.showOPPOSpecial &&
        (
            DeviceConfig.currentManufacturer == Manufacturer.OPPO ||
                DeviceConfig.currentManufacturer == Manufacturer.ONEPLUS
            )
    val deviceOplusOsdkVersion = if (checkOplusOsdk) deviceCapabilityProvider.oplusOSdkVersion else null

    // Instantiate the UI mapper with the required Compose resources
    val installStateUiMapper = remember(installResources) {
        InstallStateUiMapper(installResources)
    }

    // Execute domain logic and map to UI state within the remember block
    val checkAppSignature = settings.checkAppSignature
    return remember(
        currentPackage,
        entityToInstall,
        isSplitUpdateMode,
        containerType,
        checkAppSignature,
        settings.showSignatureInfoOnMatch,
        settings.showSignatureDetails,
        settings.detectXposedModule,
        checkOplusOsdk,
        deviceOplusOsdkVersion,
        installStateUiMapper,
    ) {
        // 1. Get pure domain state
        val domainState = analyzeInstallStateUseCase(
            currentPackage = currentPackage,
            entityToInstall = entityToInstall,
            primaryEntity = primaryEntity,
            isSplitUpdateMode = isSplitUpdateMode,
            containerType = containerType,
            systemArch = DeviceConfig.currentArchitecture,
            systemSdkInt = Build.VERSION.SDK_INT,
            checkOplusOsdk = checkOplusOsdk,
            deviceOplusOsdkVersion = deviceOplusOsdkVersion,
            checkAppSignature = checkAppSignature,
            showSignatureInfoOnMatch = settings.showSignatureInfoOnMatch,
            showSignatureDetails = settings.showSignatureDetails,
            detectXposedModule = settings.detectXposedModule,
        )

        // 2. Map to UI state
        installStateUiMapper.mapToUiState(domainState)
    }
}
