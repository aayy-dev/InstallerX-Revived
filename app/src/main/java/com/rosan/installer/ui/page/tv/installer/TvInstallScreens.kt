// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.installer

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.rosan.installer.R
import com.rosan.installer.core.bitmask.hasFlag
import com.rosan.installer.domain.engine.model.install.InstallOption
import com.rosan.installer.domain.engine.model.install.UninstallFlags
import com.rosan.installer.domain.engine.model.packageinfo.AppEntity
import com.rosan.installer.domain.engine.model.source.DataType
import com.rosan.installer.domain.session.model.ConfirmationRequestType
import com.rosan.installer.domain.settings.model.config.Authorizer
import com.rosan.installer.domain.settings.model.config.InstallerMode
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.main.installer.InstallerStage
import com.rosan.installer.ui.page.main.installer.InstallerViewAction
import com.rosan.installer.ui.page.main.installer.InstallerViewModel
import com.rosan.installer.ui.page.main.installer.components.rememberInstallOptions
import com.rosan.installer.ui.page.main.installer.dialog.DialogParams
import com.rosan.installer.ui.page.main.installer.mapper.rememberInstallStateResult
import com.rosan.installer.ui.page.main.installer.mapper.resolveInstallPrepareTarget
import com.rosan.installer.ui.page.main.widget.chip.NoticeModel
import com.rosan.installer.ui.page.tv.components.TvAction
import com.rosan.installer.ui.page.tv.components.TvActionRow
import com.rosan.installer.ui.page.tv.components.TvAppIcon
import com.rosan.installer.ui.page.tv.components.TvBackdrop
import com.rosan.installer.ui.page.tv.components.TvConfirmScreen
import com.rosan.installer.ui.page.tv.components.TvKit
import com.rosan.installer.ui.page.tv.components.TvPageTitle
import com.rosan.installer.ui.page.tv.components.TvPillButton
import com.rosan.installer.ui.page.tv.components.TvProgressBar
import com.rosan.installer.ui.page.tv.components.TvRadioRow
import com.rosan.installer.ui.page.tv.components.TvRow
import com.rosan.installer.ui.page.tv.components.TvScrollableText
import com.rosan.installer.ui.page.tv.components.TvSectionHeader
import com.rosan.installer.ui.page.tv.components.TvSwitchRow
import com.rosan.installer.ui.page.tv.components.rememberInitialFocus
import com.rosan.installer.ui.page.tv.components.tvFocusedContainer
import com.rosan.installer.ui.util.rememberErrorSuggestions
import com.rosan.installer.util.help
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------------------------
// Shared pieces
// ---------------------------------------------------------------------------------------------

/** A long text shown full screen so the remote can scroll it; Back or Close returns. */
@Composable
private fun TvDetailScreen(title: String, text: String, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    TvBackdrop(accent = null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = TvKit.SafeHorizontal, vertical = TvKit.SafeVertical),
        ) {
            TvPageTitle(title)
            Spacer(Modifier.height(20.dp))
            TvScrollableText(
                text = text,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            TvActionRow(listOf(TvAction(stringResource(R.string.close), onClose, isDefault = true)))
        }
    }
}

/** A focusable info card for the hero's details row; OK opens its full text. */
@Composable
private fun TvInfoCard(title: String, body: String, accent: Color?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(300.dp)
            .height(148.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            focusedContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (accent != null) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accent),
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A small non-focusable colored label, like the store's metadata badges. */
@Composable
private fun TvBadge(text: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun TvLoadingScreen(title: String, description: String?, progress: Float, actions: @Composable () -> Unit) {
    TvConfirmScreen(
        title = title,
        description = description,
        extra = { TvProgressBar(progress = progress, modifier = Modifier.widthIn(max = 520.dp)) },
        actions = actions,
    )
}

@Composable
fun TvErrorScreen(viewModel: InstallerViewModel, title: String, actions: @Composable () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDetail by remember { mutableStateOf(false) }
    val detailText = uiState.error.message ?: uiState.error.toString()
    if (showDetail) {
        TvDetailScreen(title = uiState.error.help(), text = detailText, onClose = { showDetail = false })
        return
    }
    TvConfirmScreen(
        title = title,
        description = uiState.error.help(),
        accent = MaterialTheme.colorScheme.error,
        extra = {
            TvPillButton(text = stringResource(R.string.details), onClick = { showDetail = true })
        },
        actions = actions,
    )
}

// ---------------------------------------------------------------------------------------------
// App page: prepare / installing / success / failed
// ---------------------------------------------------------------------------------------------

/**
 * Store-style app page: a large title with package facts, the icon on the right, the stage's actions
 * as pill buttons, and a row of focusable detail cards (signature, SDK, permissions...) underneath.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvAppHeroScreen(viewModel: InstallerViewModel, stage: InstallerStage, actions: @Composable () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentPackage = uiState.analysisResults.find { it.packageName == uiState.currentPackageName }
    val target = currentPackage?.let { resolveInstallPrepareTarget(it) }

    var detail by remember { mutableStateOf<Pair<String, String>?>(null) }
    detail?.let { (title, text) ->
        TvDetailScreen(title = title, text = text, onClose = { detail = null })
        return
    }

    val base = target?.selectedEntities?.filterIsInstance<AppEntity.BaseEntity>()?.firstOrNull()
        ?: target?.primaryEntity as? AppEntity.BaseEntity
    val module = target?.primaryEntity as? AppEntity.ModuleEntity
    val installed = currentPackage?.installedAppInfo
    val packageName = currentPackage?.packageName ?: uiState.currentPackageName.orEmpty()

    val title = (stage as? InstallerStage.Installing)?.appLabel
        ?: base?.label
        ?: module?.name
        ?: installed?.label
        ?: packageName
    val newVersion = base?.let { "${it.versionName} (${it.versionCode})" } ?: module?.let { "${it.version} (${it.versionCode})" }
    val oldVersion = installed?.let { "${it.versionName} (${it.versionCode})" }
    val versionLine = when {
        newVersion == null -> null
        oldVersion != null && oldVersion != newVersion -> "$oldVersion  →  $newVersion"
        else -> newVersion
    }
    // Same rule as the phone dialog: the size only shows when the profile asks for it.
    val size = target?.selectedEntities?.sumOf { it.size }?.takeIf { it > 0 && uiState.config.displaySize }
    val meta = listOfNotNull(packageName.takeIf { it.isNotBlank() }, versionLine, size?.let { Formatter.formatShortFileSize(context, it).replace(' ', '\u00A0') })
        .joinToString("  •  ")

    val notices: List<NoticeModel> = if (target != null) {
        rememberInstallStateResult(target, uiState.viewSettings).notices
    } else {
        emptyList()
    }
    val errorHeadline = uiState.error.help()
    // The phone dialog's explanations for when installing is not possible, so a screen offering only
    // Cancel/Previous still says why.
    val prepareProblem = if (stage is InstallerStage.InstallPrepare) {
        when {
            currentPackage == null && uiState.analysisResults.size > 1 -> stringResource(R.string.installer_prepare_install_too_many)

            target == null -> stringResource(R.string.installer_prepare_install_empty)

            installed == null && target.entityToInstall == null && target.selectedEntities.any { it is AppEntity.SplitEntity } ->
                stringResource(R.string.installer_splits_invalid_tip)

            else -> null
        }
    } else {
        null
    }
    val cards = buildList {
        notices.forEach { add(Triple(it.shortLabel, it.fullDescription, it.color as Color?)) }
        if (stage is InstallerStage.InstallFailed) {
            add(Triple(errorHeadline, uiState.error.message ?: uiState.error.toString(), null))
        }
        base?.permissions?.takeIf { it.isNotEmpty() }?.let { permissions ->
            add(
                Triple(
                    "${context.getString(R.string.permission_list)} (${permissions.size})",
                    permissions.joinToString("\n"),
                    null,
                ),
            )
        }
    }
    val accent = uiState.seedColor

    // The page scrolls by section, like the store's app pages: the full header while the buttons have
    // focus, and the buttons with the details below once the cards do. Scrolling only as far as the
    // focused card would leave the title cut in half at the top edge.
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val sectionTopPaddingPx = with(LocalDensity.current) { 48.dp.roundToPx() }
    fun showSection(index: Int) {
        scope.launch {
            // The details only move the page when they do not already fit below the header.
            val details = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == DETAILS_KEY }
            val detailsFit = details != null && details.offset + details.size <= listState.layoutInfo.viewportEndOffset
            // Offset by the list's top padding so the section starts at the very top, with no
            // sliver of the header above it.
            if (index == 0) {
                listState.animateScrollToItem(0)
            } else if (!detailsFit) {
                listState.animateScrollToItem(index, scrollOffset = sectionTopPaddingPx)
            }
        }
    }

    TvBackdrop(accent = accent) {
        CompositionLocalProvider(LocalBringIntoViewSpec provides NoScrollBringIntoViewSpec) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(vertical = 48.dp),
            ) {
                item {
                    Row(Modifier.padding(horizontal = TvKit.SafeHorizontal), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontSize = 52.sp,
                                lineHeight = 60.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (meta.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Text(meta, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                            }
                            val minSdkLabel = stringResource(R.string.installer_package_min_sdk_label)
                            val targetSdkLabel = stringResource(R.string.installer_package_target_sdk_label)
                            val sdk = base?.let {
                                listOfNotNull(it.minSdk?.let { v -> "$minSdkLabel $v" }, it.targetSdk?.let { v -> "$targetSdkLabel $v" })
                            }
                            if (uiState.config.displaySdk && !sdk.isNullOrEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text(sdk.joinToString("  •  "), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            module?.description?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(10.dp))
                                Text(it, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                            if (notices.isNotEmpty()) {
                                Spacer(Modifier.height(18.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    notices.take(4).forEach { TvBadge(it.shortLabel, it.color) }
                                }
                            }
                        }
                        Spacer(Modifier.width(40.dp))
                        TvAppIcon(bitmap = uiState.displayIcons[packageName], size = 168.dp, fallback = AppIcons.Android)
                    }
                }

                item {
                    Column(
                        Modifier
                            .padding(start = TvKit.SafeHorizontal, end = TvKit.SafeHorizontal, top = 36.dp)
                            .onFocusChanged { if (it.hasFocus) showSection(0) },
                    ) {
                        when (stage) {
                            is InstallerStage.Installing -> {
                                val status = if (stage.total > 1) {
                                    stringResource(R.string.installing_progress_text, stage.appLabel ?: title, stage.current, stage.total)
                                } else {
                                    stringResource(R.string.installer_installing)
                                }
                                Text(status, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.height(14.dp))
                                TvProgressBar(progress = stage.progress, modifier = Modifier.widthIn(max = 640.dp))
                                Spacer(Modifier.height(28.dp))
                            }

                            is InstallerStage.InstallSuccess -> {
                                Text(
                                    stringResource(R.string.installer_install_success),
                                    fontSize = 22.sp,
                                    color = tvFocusedContainer(),
                                )
                                Spacer(Modifier.height(24.dp))
                            }

                            is InstallerStage.InstallFailed -> {
                                Text(
                                    stringResource(R.string.installer_install_failed),
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(uiState.error.help(), fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.height(24.dp))
                            }

                            else -> prepareProblem?.let {
                                Text(it, fontSize = 20.sp, color = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                        actions()
                        if (stage is InstallerStage.InstallFailed && uiState.viewSettings.showSmartSuggestion) {
                            TvFailureSuggestions(viewModel)
                        }
                    }
                }

                if (cards.isNotEmpty()) {
                    item(key = DETAILS_KEY) {
                        Column(
                            Modifier
                                .padding(top = 44.dp)
                                .onFocusChanged { if (it.hasFocus) showSection(1) },
                        ) {
                            Text(
                                stringResource(R.string.details),
                                modifier = Modifier.padding(horizontal = TvKit.SafeHorizontal),
                                fontSize = 28.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(16.dp))
                            // Runs to the screen edge so the next card peeks in, as on the store's rows.
                            CompositionLocalProvider(LocalBringIntoViewSpec provides KeepVisibleBringIntoViewSpec) {
                                LazyRow(
                                    modifier = Modifier.focusRestorer(),
                                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                                    contentPadding = PaddingValues(horizontal = TvKit.SafeHorizontal, vertical = 8.dp),
                                ) {
                                    items(cards) { (cardTitle, body, color) ->
                                        TvInfoCard(title = cardTitle, body = body, accent = color, onClick = { detail = cardTitle to body })
                                    }
                                }
                            }
                        }
                    }
                }
                // Room for the button section to scroll to the top when the details need it.
                item { Spacer(Modifier.fillParentMaxHeight(0.4f)) }
            }
        }
    }
}

private const val DETAILS_KEY = "details"

/** Leaves scrolling to the page, which moves by whole sections. */
@OptIn(ExperimentalFoundationApi::class)
private val NoScrollBringIntoViewSpec = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}

/** Scrolls the minimum needed to show a focused child, and not at all when it is already visible. */
@OptIn(ExperimentalFoundationApi::class)
private val KeepVisibleBringIntoViewSpec = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val trailingEdge = offset + size
        return when {
            offset >= 0f && trailingEdge <= containerSize -> 0f
            offset < 0f || size > containerSize -> offset
            else -> trailingEdge - containerSize
        }
    }
}

@Composable
private fun TvFailureSuggestions(viewModel: InstallerViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingUninstall by remember { mutableStateOf<Pair<Boolean, String?>?>(null) }
    val suggestions = rememberErrorSuggestions(
        error = uiState.error,
        viewModel = viewModel,
        onShowUninstallConfirm = { keepData, conflictingPackage -> pendingUninstall = keepData to conflictingPackage },
    )
    pendingUninstall?.let { (keepData, conflictingPackage) ->
        BackHandler { pendingUninstall = null }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.uninstall), fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            TvActionRow(
                actions = listOf(
                    TvAction(stringResource(R.string.confirm), onClick = {
                        pendingUninstall = null
                        viewModel.dispatch(InstallerViewAction.UninstallAndRetryInstall(keepData, conflictingPackage))
                    }),
                    TvAction(stringResource(R.string.cancel), onClick = { pendingUninstall = null }, isDefault = true),
                ),
            )
        }
        return
    }
    if (suggestions.isEmpty()) return
    Spacer(Modifier.height(18.dp))
    TvActionRow(
        actions = suggestions.map { TvAction(stringResource(it.labelRes), it.onClick) },
        requestInitialFocus = false,
    )
}

// ---------------------------------------------------------------------------------------------
// Options (extended menu) and permission list
// ---------------------------------------------------------------------------------------------

/**
 * Two-pane options page in the style of the system settings: the app on the left, the install
 * options as rows on the right. Choices open a radio list in place.
 */
@Composable
fun TvInstallOptionsScreen(viewModel: InstallerViewModel, actions: @Composable () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val config = uiState.config
    val currentPackage = uiState.analysisResults.find { it.packageName == uiState.currentPackageName }
    val containerApk = currentPackage?.appEntities?.firstOrNull()?.app?.sourceType == DataType.APK
    val privileged = config.authorizer == Authorizer.Root || config.authorizer == Authorizer.Shizuku
    val installOptions = rememberInstallOptions(config.authorizer)

    var openChoice by rememberSaveable { mutableStateOf<String?>(null) }

    val modeSelf = stringResource(R.string.config_installer_mode_self)
    val modeInitiator = stringResource(R.string.config_installer_mode_initiator)
    val followSettings = stringResource(R.string.config_follow_settings)
    // Same options and rules as the phone menu: Custom with no installer, or with the installer from
    // the settings, means "follow settings".
    val followsSettings = config.installerMode == InstallerMode.Custom &&
        (config.installer == null || config.installer == uiState.defaultInstallerFromSettings)
    val installerSummary = when (config.installerMode) {
        InstallerMode.Self -> modeSelf

        InstallerMode.Initiator -> modeInitiator

        InstallerMode.Custom -> if (followsSettings) {
            followSettings
        } else {
            uiState.managedInstallerPackages.find { it.packageName == config.installer }?.name ?: followSettings
        }
    }

    when (openChoice) {
        "installer" -> {
            BackHandler { openChoice = null }
            TvChoicePage(
                title = stringResource(R.string.config_declare_installer),
                options = buildList {
                    add(Triple(modeSelf, null as String?, config.installerMode == InstallerMode.Self))
                    add(Triple(modeInitiator, null, config.installerMode == InstallerMode.Initiator))
                    add(Triple(followSettings, uiState.defaultInstallerFromSettings, followsSettings))
                    uiState.managedInstallerPackages.forEach {
                        add(
                            Triple(
                                it.name,
                                it.packageName,
                                config.installerMode == InstallerMode.Custom && !followsSettings && config.installer == it.packageName,
                            ),
                        )
                    }
                },
                onSelect = { index ->
                    when (index) {
                        0 -> viewModel.dispatch(InstallerViewAction.SetInstallerMode(InstallerMode.Self))

                        1 -> viewModel.dispatch(InstallerViewAction.SetInstallerMode(InstallerMode.Initiator))

                        2 -> {
                            viewModel.dispatch(InstallerViewAction.SetInstallerMode(InstallerMode.Custom))
                            viewModel.dispatch(InstallerViewAction.SetInstaller(uiState.defaultInstallerFromSettings))
                        }

                        else -> {
                            viewModel.dispatch(InstallerViewAction.SetInstallerMode(InstallerMode.Custom))
                            viewModel.dispatch(InstallerViewAction.SetInstaller(uiState.managedInstallerPackages[index - 3].packageName))
                        }
                    }
                    openChoice = null
                },
            )
            return
        }

        "user" -> {
            BackHandler { openChoice = null }
            val users = uiState.availableUsers.entries.toList()
            TvChoicePage(
                title = stringResource(R.string.config_target_user),
                options = users.map { (id, name) -> Triple(name, id.toString(), id == config.targetUserId) },
                onSelect = { index ->
                    viewModel.dispatch(InstallerViewAction.SetTargetUser(users[index].key))
                    openChoice = null
                },
            )
            return
        }
    }

    TvBackdrop(accent = uiState.seedColor) {
        Row(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .padding(start = TvKit.SafeHorizontal, top = 64.dp, end = 24.dp),
            ) {
                TvAppIcon(bitmap = uiState.displayIcons[currentPackage?.packageName], size = 96.dp, fallback = AppIcons.Android)
                Spacer(Modifier.height(24.dp))
                TvPageTitle(stringResource(R.string.extended_menu))
                currentPackage?.installedAppInfo?.label?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.padding(bottom = TvKit.SafeVertical)) { actions() }
            }
            LazyColumn(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .focusRequester(rememberInitialFocus()),
                contentPadding = PaddingValues(start = 12.dp, end = TvKit.SafeHorizontal, top = 56.dp, bottom = 56.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (containerApk) {
                    item {
                        TvRow(
                            title = stringResource(R.string.permission_list),
                            summary = stringResource(R.string.permission_list_desc),
                            icon = AppIcons.Permission,
                            onClick = { viewModel.dispatch(InstallerViewAction.InstallExtendedSubMenu) },
                        )
                    }
                }
                if (privileged) {
                    item {
                        TvRow(
                            title = stringResource(R.string.config_declare_installer),
                            summary = installerSummary,
                            icon = AppIcons.InstallSource,
                            onClick = { openChoice = "installer" },
                        )
                    }
                    if (config.enableCustomizeUser) {
                        item {
                            TvRow(
                                title = stringResource(R.string.config_target_user),
                                summary = uiState.availableUsers[config.targetUserId],
                                icon = AppIcons.InstallUser,
                                onClick = { openChoice = "user" },
                            )
                        }
                    }
                    item { TvSectionHeader(stringResource(R.string.extended_menu)) }
                    items(installOptions, key = InstallOption::name) { option ->
                        val checked = config.installFlags and option.value != 0
                        TvSwitchRow(
                            title = stringResource(option.labelResource),
                            summary = stringResource(option.descResource),
                            checked = checked,
                            onCheckedChange = { viewModel.toggleInstallFlag(option.value, it) },
                        )
                    }
                }
            }
        }
    }
}

/** A list of mutually exclusive options, as on the system settings' choice pages. */
@Composable
private fun TvChoicePage(title: String, options: List<Triple<String, String?, Boolean>>, onSelect: (Int) -> Unit) {
    val initialFocus = rememberInitialFocus()
    TvBackdrop(accent = null) {
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(0.42f)
                    .padding(start = TvKit.SafeHorizontal, top = 64.dp, end = 24.dp),
            ) { TvPageTitle(title) }
            LazyColumn(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(start = 12.dp, end = TvKit.SafeHorizontal, top = 56.dp, bottom = 56.dp),
            ) {
                options.forEachIndexed { index, (label, summary, selected) ->
                    item {
                        TvRadioRow(
                            title = label,
                            summary = summary,
                            selected = selected,
                            // Opens on the current choice, like the system's choice lists.
                            modifier = if (selected || (index == 0 && options.none { it.third })) {
                                Modifier.focusRequester(initialFocus)
                            } else {
                                Modifier
                            },
                            onClick = { onSelect(index) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TvPermissionListScreen(viewModel: InstallerViewModel, actions: @Composable () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentPackage = uiState.analysisResults.find { it.packageName == uiState.currentPackageName }
    val permissions = currentPackage?.appEntities
        ?.map { it.app }
        ?.filterIsInstance<AppEntity.BaseEntity>()
        ?.firstOrNull()
        ?.permissions
        .orEmpty()
    TvBackdrop(accent = uiState.seedColor) {
        Row(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .padding(start = TvKit.SafeHorizontal, top = 64.dp, end = 24.dp),
            ) {
                TvPageTitle(stringResource(R.string.permission_list))
                Spacer(Modifier.height(8.dp))
                Text("${permissions.size}", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Box(Modifier.padding(bottom = TvKit.SafeVertical)) { actions() }
            }
            LazyColumn(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .focusRequester(rememberInitialFocus()),
                contentPadding = PaddingValues(start = 12.dp, end = TvKit.SafeHorizontal, top = 56.dp, bottom = 56.dp),
            ) {
                items(permissions) { permission ->
                    TvRow(
                        title = permission.substringAfterLast('.'),
                        summary = permission,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Module install log
// ---------------------------------------------------------------------------------------------

@Composable
fun TvModuleInstallScreen(viewModel: InstallerViewModel, stage: InstallerStage.InstallingModule) {
    val scrollState = rememberScrollState()
    LaunchedEffect(stage.output.size) { scrollState.animateScrollTo(scrollState.maxValue) }
    TvBackdrop(accent = null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = TvKit.SafeHorizontal, vertical = TvKit.SafeVertical),
        ) {
            TvPageTitle(
                stringResource(if (stage.isFinished) R.string.installer_install_complete else R.string.installer_installing_module),
            )
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .verticalScroll(scrollState)
                    .padding(20.dp),
            ) {
                Text(
                    text = stage.output.joinToString("\n"),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(20.dp))
            TvActionRow(
                actions = if (stage.isFinished) {
                    listOf(
                        TvAction(stringResource(R.string.reboot), { viewModel.dispatch(InstallerViewAction.Reboot("")) }),
                        TvAction(stringResource(R.string.close), { viewModel.dispatch(InstallerViewAction.Close) }, isDefault = true),
                    )
                } else {
                    listOf(TvAction(stringResource(R.string.installer_installing), {}, enabled = false))
                },
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Confirmations
// ---------------------------------------------------------------------------------------------

@Composable
fun TvSessionConfirmScreen(viewModel: InstallerViewModel, stage: InstallerStage.InstallConfirm, actions: @Composable () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unknown = stringResource(R.string.installer_label_unknown)
    val source = stage.sourceAppLabel?.toString() ?: unknown
    val message = when {
        stage.requestType == ConfirmationRequestType.PRE_APPROVAL -> stringResource(R.string.install_confirm_pre_approval_tip, source)
        stage.requestType == ConfirmationRequestType.PERMISSIONS -> stringResource(R.string.install_confirm_permissions_tip, source)
        stage.isOwnershipConflict -> stringResource(R.string.install_confirm_question_update_owner_reminder, source)
        !stage.isSelfSession -> stringResource(R.string.install_confirm_external_request_tip, source)
        else -> stringResource(R.string.installer_prepare_type_unknown_confirm)
    }
    TvConfirmScreen(
        title = stage.appLabel.toString(),
        description = "${stage.packageName}\n\n$message",
        accent = uiState.seedColor,
        badge = { TvAppIcon(bitmap = stage.appIcon?.asImageBitmap(), size = 56.dp) },
        actions = actions,
    )
}

@Composable
fun TvUnknownSourceScreen(viewModel: InstallerViewModel, actions: @Composable () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val label = uiState.unknownSourcePermissionAppLabel ?: stringResource(R.string.installer_label_unknown)
    TvConfirmScreen(
        title = stringResource(R.string.installer_waiting_unknown_source),
        description = stringResource(R.string.installer_waiting_unknown_source_desc, label),
        actions = actions,
    )
}

@Composable
fun TvInstallCompletedScreen(stage: InstallerStage.InstallCompleted, actions: @Composable () -> Unit) {
    TvBackdrop(accent = null) {
        Row(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .padding(start = TvKit.SafeHorizontal, top = 64.dp, end = 24.dp),
            ) {
                TvPageTitle(stringResource(R.string.installer_install_complete))
                Spacer(Modifier.height(8.dp))
                Text(
                    "${stage.results.count { it.success }} / ${stage.results.size}",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Box(Modifier.padding(bottom = TvKit.SafeVertical)) { actions() }
            }
            LazyColumn(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(start = 12.dp, end = TvKit.SafeHorizontal, top = 56.dp, bottom = 56.dp),
            ) {
                items(stage.results) { result ->
                    val app = result.entity.app
                    val label = (app as? AppEntity.BaseEntity)?.label ?: app.packageName
                    TvRow(
                        title = label,
                        summary = if (result.success) {
                            stringResource(R.string.installer_install_success)
                        } else {
                            result.error?.help() ?: stringResource(R.string.installer_install_failed)
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun TvUninstallScreen(viewModel: InstallerViewModel, actions: @Composable () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val info = uiState.uiUninstallInfo
    val flags = uiState.config.uninstallFlags
    // Same icon source as the phone dialog: the view model loads it into displayIcons.
    val icon = info?.let { uiState.displayIcons[it.packageName] }
    val version = when {
        info?.versionName != null && info.versionCode != null -> "${info.versionName} (${info.versionCode})"
        else -> info?.versionName ?: info?.versionCode?.toString().orEmpty()
    }
    TvConfirmScreen(
        title = "${stringResource(R.string.uninstall)} ${info?.appLabel ?: info?.packageName.orEmpty()}",
        description = listOfNotNull(info?.packageName, version.takeIf { it.isNotBlank() }).joinToString("  •  "),
        accent = info?.seedColor?.let { Color(it) },
        badge = { TvAppIcon(bitmap = icon, size = 56.dp) },
        extra = if (info?.isArchived == true) {
            { Text(stringResource(R.string.uninstall_archive_message), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            {
                Column(Modifier.widthIn(max = 560.dp)) {
                    listOf(
                        UninstallFlags.DELETE_KEEP_DATA to R.string.uninstall_keep_data,
                        UninstallFlags.DELETE_ALL_USERS to R.string.uninstall_all_users,
                        UninstallFlags.DELETE_SYSTEM_APP to R.string.uninstall_delete_system_app,
                    ).forEach { (flag, label) ->
                        val checked = flags.hasFlag(flag)
                        TvSwitchRow(
                            title = stringResource(label),
                            checked = checked,
                            onCheckedChange = { viewModel.dispatch(InstallerViewAction.ToggleUninstallFlag(flag, it)) },
                        )
                    }
                }
            }
        },
        actions = actions,
    )
}

@Composable
fun TvUninstallResultScreen(viewModel: InstallerViewModel, success: Boolean, actions: @Composable () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val info = uiState.uiUninstallInfo
    TvConfirmScreen(
        title = if (success) stringResource(R.string.uninstall_success_message) else uiState.error.help(),
        description = info?.appLabel ?: info?.packageName,
        accent = if (success) null else MaterialTheme.colorScheme.error,
        actions = actions,
    )
}

@Composable
fun TvUnarchiveScreen(stage: InstallerStage.UnarchiveReady, actions: @Composable () -> Unit) {
    TvConfirmScreen(
        title = stringResource(R.string.unarchive_title, stage.appLabel),
        description = stringResource(R.string.unarchive_message, stage.installerLabel),
        actions = actions,
    )
}

/** Fallback for stages without a dedicated TV layout yet: the stage content in a TV panel. */
@Composable
fun TvGenericStageScreen(params: DialogParams, actions: @Composable () -> Unit) {
    TvBackdrop(accent = null) {
        Row(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .padding(start = TvKit.SafeHorizontal, top = 64.dp, end = 24.dp),
            ) {
                params.icon.content?.invoke()
                Spacer(Modifier.height(16.dp))
                params.title.content?.invoke()
                params.subtitle.content?.invoke()
                Spacer(Modifier.weight(1f))
                Box(Modifier.padding(bottom = TvKit.SafeVertical)) { actions() }
            }
            Box(
                Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .padding(start = 12.dp, end = TvKit.SafeHorizontal, top = 56.dp, bottom = 56.dp),
            ) {
                (params.content.content ?: params.text.content)?.invoke()
            }
        }
    }
}
