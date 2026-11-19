// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.tv.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.rosan.installer.R
import com.rosan.installer.ui.icons.AppIcons
import com.rosan.installer.ui.page.tv.components.LocalTvPreviewMode
import com.rosan.installer.ui.page.tv.components.TvAction
import com.rosan.installer.ui.page.tv.components.TvActionRow
import com.rosan.installer.ui.page.tv.components.TvKit
import com.rosan.installer.ui.page.tv.components.TvPageTitle
import com.rosan.installer.ui.page.tv.components.TvRadioRow
import com.rosan.installer.ui.page.tv.components.TvRow
import com.rosan.installer.ui.page.tv.components.TvSectionHeader
import com.rosan.installer.ui.page.tv.components.TvSwitchRow
import com.rosan.installer.ui.page.tv.components.tvFocusedContainer
import kotlinx.coroutines.delay

/** One option of a [TvSettingsListScope.choice] row. */
data class TvChoiceOption(val label: String, val summary: String? = null)

/** What the right-hand pane shows for the focused row. */
data class TvPreview(
    val title: String,
    val description: String? = null,
    val options: List<TvChoiceOption> = emptyList(),
    val selectedIndex: Int = -1,
    /** A page this row opens; the pane then shows that page itself, read-only. */
    val opens: TvSettingsDest? = null,
)

internal data class OpenTextInput(
    val title: String,
    val description: String?,
    val value: String,
    val placeholder: String?,
    val singleLine: Boolean,
    val onDone: (String) -> Unit,
)

internal data class OpenChoice(
    val title: String,
    val description: String?,
    val options: List<TvChoiceOption>,
    val selectedIndex: Int,
    val onSelect: (Int) -> Unit,
)

/**
 * Declarative rows for a TV settings page. Every row is focusable, and focusing a row shows its
 * [TvPreview] in the right pane, as in the system settings app.
 */
class TvSettingsListScope internal constructor(
    private val lazy: LazyListScope,
    private val onPreview: (TvPreview?) -> Unit,
    private val onOpenChoice: (OpenChoice) -> Unit,
    private val onOpenTextInput: (OpenTextInput) -> Unit,
    private val focusFor: (String) -> Modifier,
    private val onRowFocused: (String) -> Unit,
) {
    fun header(text: String) {
        lazy.item(contentType = "header") { TvSectionHeader(text) }
    }

    /**
     * A row that runs [onClick], typically navigating to another page. A row that [opens] a page
     * previews it on the right, and like every row that leads somewhere ([drillIn]) it also opens
     * with Right, as in the system settings.
     */
    fun item(
        key: String,
        title: String,
        summary: String? = null,
        icon: ImageVector? = null,
        enabled: Boolean = true,
        description: String? = summary,
        preview: TvPreview? = null,
        opens: TvSettingsDest? = null,
        drillIn: Boolean = opens != null,
        trailing: (@Composable (focused: Boolean) -> Unit)? = null,
        onLongClick: (() -> Unit)? = null,
        onClick: () -> Unit,
    ) {
        lazy.item(key = key, contentType = "row") {
            TvRow(
                title = title,
                summary = summary.rowSummary(),
                icon = icon,
                enabled = enabled,
                modifier = focusFor(key).openOnRight(drillIn && enabled, onClick),
                onFocused = {
                    onRowFocused(key)
                    onPreview(preview ?: TvPreview(title, if (opens != null) null else description, opens = opens))
                },
                onLongClick = onLongClick,
                trailing = trailing,
                onClick = onClick,
            )
        }
    }

    /** A focusable row that only shows information; focus keeps long pages scrollable. */
    fun info(key: String, title: String, summary: String? = null, icon: ImageVector? = null, description: String? = summary) {
        item(key = key, title = title, summary = summary, icon = icon, description = description, onClick = {})
    }

    fun switch(
        key: String,
        title: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        summary: String? = null,
        icon: ImageVector? = null,
        enabled: Boolean = true,
        description: String? = summary,
    ) {
        lazy.item(key = key, contentType = "switch") {
            TvSwitchRow(
                title = title,
                summary = summary.rowSummary(),
                icon = icon,
                checked = checked,
                enabled = enabled,
                modifier = focusFor(key),
                onFocused = {
                    onRowFocused(key)
                    onPreview(TvPreview(title, description))
                },
                onCheckedChange = onCheckedChange,
            )
        }
    }

    /**
     * A row whose value is one of [options]. Focus previews the options on the right; OK opens them
     * as a radio list, the same drill-in the system settings use.
     */
    fun choice(
        key: String,
        title: String,
        options: List<TvChoiceOption>,
        selectedIndex: Int,
        onSelect: (Int) -> Unit,
        icon: ImageVector? = null,
        enabled: Boolean = true,
        description: String? = null,
    ) {
        val summary = options.getOrNull(selectedIndex)?.label
        lazy.item(key = key, contentType = "choice") {
            TvRow(
                title = title,
                summary = summary,
                icon = icon,
                enabled = enabled,
                modifier = focusFor(key).openOnRight(enabled) {
                    onOpenChoice(OpenChoice(title, description, options, selectedIndex, onSelect))
                },
                onFocused = {
                    onRowFocused(key)
                    onPreview(TvPreview(title, description, options, selectedIndex))
                },
                onClick = { onOpenChoice(OpenChoice(title, description, options, selectedIndex, onSelect)) },
            )
        }
    }

    /**
     * A text value. The row shows the current value; OK opens a full-screen entry page with the
     * keyboard, so moving focus through the list never pops up the keyboard.
     */
    fun textInput(
        key: String,
        title: String,
        value: String,
        onValueChange: (String) -> Unit,
        placeholder: String? = null,
        icon: ImageVector? = AppIcons.Edit,
        enabled: Boolean = true,
        description: String? = null,
        singleLine: Boolean = true,
    ) {
        lazy.item(key = key, contentType = "text") {
            TvRow(
                title = title,
                summary = value.ifBlank { placeholder },
                icon = icon,
                enabled = enabled,
                modifier = focusFor(key).openOnRight(enabled) {
                    onOpenTextInput(OpenTextInput(title, description, value, placeholder, singleLine, onValueChange))
                },
                onFocused = {
                    onRowFocused(key)
                    onPreview(TvPreview(title, description ?: value.ifBlank { placeholder }))
                },
                onClick = { onOpenTextInput(OpenTextInput(title, description, value, placeholder, singleLine, onValueChange)) },
            )
        }
    }

    /** Arbitrary content for rows the helpers above do not cover. */
    fun custom(key: String, content: @Composable () -> Unit) {
        lazy.item(key = key) { content() }
    }
}

/**
 * Two-pane TV settings page: the title and rows on the left, a preview of the focused row on the right.
 */
@Composable
fun TvSettingsPage(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    initialFocusKey: String? = null,
    content: TvSettingsListScope.() -> Unit,
) {
    if (LocalTvPreviewMode.current) {
        TvSettingsPreviewList(content)
        return
    }
    var preview by remember { mutableStateOf<TvPreview?>(null) }
    var openChoice by remember { mutableStateOf<OpenChoice?>(null) }
    var openTextInput by remember { mutableStateOf<OpenTextInput?>(null) }

    // Kept above the early returns so a choice or text page, or a deeper page on the back stack,
    // returns to the same scroll position and focused row. Pages that swap in a sub-screen of their
    // own remove this page entirely, so the position is also kept in the entry's page memory.
    val memory = LocalTvPageMemory.current
    val remembered = remember(memory, title) { memory?.get(title) }
    val listState = rememberLazyListState(remembered?.index ?: 0, remembered?.offset ?: 0)
    var lastFocusedKey by rememberSaveable { mutableStateOf(remembered?.focusedKey) }
    DisposableEffect(memory, title) {
        onDispose { memory?.put(title, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset, lastFocusedKey) }
    }
    val rowFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    openChoice?.let { choice ->
        BackHandler { openChoice = null }
        TvChoicePage(choice) { openChoice = null }
        return
    }
    openTextInput?.let { input ->
        BackHandler { openTextInput = null }
        TvTextEntryPage(input) { openTextInput = null }
        return
    }

    // Like the system settings, a page opens with a row focused: the one last used, the selected one, or the first.
    val listFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        // A radio-style page starts on its selected row, like the system's choice lists.
        val restored = (lastFocusedKey ?: initialFocusKey)
            ?.let { rowFocusRequesters[it] }
            ?.let { runCatching { it.requestFocus() }.isSuccess } == true
        if (!restored) runCatching { listFocusRequester.requestFocus() }
    }

    // Left from a row with nothing further left goes back, as in the system settings.
    val canGoBack = LocalTvSettingsNavigator.current.stack.size > 1
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val focusManager = LocalFocusManager.current
    Row(
        modifier = modifier
            .fillMaxSize()
            .onKeyEvent { event ->
                val isLeft = event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft
                if (isLeft && canGoBack && backDispatcher != null && !focusManager.moveFocus(FocusDirection.Left)) {
                    backDispatcher.onBackPressed()
                    true
                } else {
                    false
                }
            },
    ) {
        Column(
            modifier = Modifier
                .weight(0.48f)
                .fillMaxHeight(),
        ) {
            Column(Modifier.padding(start = TvKit.SafeHorizontal + 20.dp, top = 52.dp, end = 24.dp)) {
                TvPageTitle(title)
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(subtitle, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .fadingTopEdge()
                    .focusRequester(listFocusRequester),
                state = listState,
                contentPadding = PaddingValues(start = TvKit.SafeHorizontal - 8.dp, end = 12.dp, top = 16.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                TvSettingsListScope(
                    this,
                    onPreview = { preview = it },
                    onOpenChoice = { openChoice = it },
                    onOpenTextInput = { openTextInput = it },
                    focusFor = { key -> Modifier.focusRequester(rowFocusRequesters.getOrPut(key) { FocusRequester() }) },
                    onRowFocused = { lastFocusedKey = it },
                ).content()
            }
        }
        TvPreviewPane(
            preview = preview,
            modifier = Modifier
                .weight(0.52f)
                .fillMaxHeight(),
        )
    }
}

/** A page drawn read-only in the preview pane: its rows only, dimmed and unfocusable. */
@Composable
private fun TvSettingsPreviewList(content: TvSettingsListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.6f),
        userScrollEnabled = false,
        contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        TvSettingsListScope(
            this,
            onPreview = {},
            onOpenChoice = {},
            onOpenTextInput = {},
            focusFor = { Modifier },
            onRowFocused = {},
        ).content()
    }
}

/** Opens the row with Right as well as OK, for rows that lead to another page. */
private fun Modifier.openOnRight(enabled: Boolean, open: () -> Unit): Modifier = if (!enabled) {
    this
} else {
    onPreviewKeyEvent { event ->
        if (event.key == Key.DirectionRight) {
            if (event.type == KeyEventType.KeyDown) open()
            true
        } else {
            false
        }
    }
}

@Composable
private fun TvPreviewPane(preview: TvPreview?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)),
    ) {
        if (preview == null) return@Box
        preview.opens?.let { destination ->
            Column(Modifier.padding(start = 48.dp, top = 52.dp, end = TvKit.SafeHorizontal - 16.dp)) {
                Text(
                    text = preview.title,
                    modifier = Modifier.padding(start = 16.dp),
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                )
                Spacer(Modifier.height(16.dp))
                // Drawn after a short pause, so moving quickly through the list stays smooth.
                var ready by remember(destination) { mutableStateOf(false) }
                LaunchedEffect(destination) {
                    delay(150)
                    ready = true
                }
                AnimatedVisibility(visible = ready, enter = fadeIn(tween(180))) {
                    CompositionLocalProvider(LocalTvPreviewMode provides true) {
                        TvSettingsDestination(destination)
                    }
                }
            }
            return@Box
        }
        Column(Modifier.padding(start = 64.dp, top = 52.dp, end = TvKit.SafeHorizontal)) {
            Text(
                text = preview.title,
                fontSize = if (preview.title.none { it == ' ' } && preview.title.length > 22) 26.sp else 34.sp,
                lineHeight = 40.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            )
            if (!preview.description.isNullOrBlank()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = preview.description,
                    fontSize = 17.sp,
                    lineHeight = 25.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }
            if (preview.options.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                preview.options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(option.label, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                            option.summary?.let {
                                Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
                            }
                        }
                        val selected = index == preview.selectedIndex
                        Box(
                            Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f) else Color.Transparent,
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvChoicePage(choice: OpenChoice, onDone: () -> Unit) {
    val selectedFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { selectedFocusRequester.requestFocus() } }
    Row(
        Modifier
            .fillMaxSize()
            .onKeyEvent { event ->
                // Left steps back out, the mirror of Right opening it.
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                    onDone()
                    true
                } else {
                    false
                }
            },
    ) {
        Column(
            Modifier
                .weight(0.48f)
                .padding(start = TvKit.SafeHorizontal + 20.dp, top = 52.dp, end = 24.dp),
        ) {
            TvPageTitle(choice.title)
            if (!choice.description.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(choice.description, fontSize = 17.sp, lineHeight = 25.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LazyColumn(
            modifier = Modifier
                .weight(0.52f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(start = 24.dp, end = TvKit.SafeHorizontal, top = 120.dp, bottom = 48.dp),
        ) {
            choice.options.forEachIndexed { index, option ->
                item {
                    TvRadioRow(
                        title = option.label,
                        summary = option.summary,
                        selected = index == choice.selectedIndex,
                        modifier = if (index == choice.selectedIndex.coerceAtLeast(0)) {
                            Modifier.focusRequester(selectedFocusRequester)
                        } else {
                            Modifier
                        },
                        onClick = {
                            choice.onSelect(index)
                            onDone()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TvTextEntryPage(input: OpenTextInput, onDone: () -> Unit) {
    var text by remember { mutableStateOf(TextFieldValue(input.value, TextRange(input.value.length))) }
    val fieldFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var fieldFocused by remember { mutableStateOf(false) }
    val save = {
        input.onDone(text.text)
        onDone()
    }
    LaunchedEffect(Unit) { runCatching { fieldFocusRequester.requestFocus() } }

    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(0.48f)
                .padding(start = TvKit.SafeHorizontal + 20.dp, top = 52.dp, end = 24.dp),
        ) {
            TvPageTitle(input.title)
            if (!input.description.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(input.description, fontSize = 17.sp, lineHeight = 25.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(
            Modifier
                .weight(0.52f)
                .padding(start = 24.dp, end = TvKit.SafeHorizontal, top = 120.dp),
        ) {
            val fieldShape = RoundedCornerShape(16.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(fieldShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    .border(
                        2.dp,
                        if (fieldFocused) tvFocusedContainer() else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        fieldShape,
                    )
                    .padding(horizontal = 22.dp, vertical = 18.dp),
            ) {
                if (text.text.isEmpty() && !input.placeholder.isNullOrBlank()) {
                    Text(input.placeholder, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = input.singleLine,
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(imeAction = if (input.singleLine) ImeAction.Done else ImeAction.Default),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(fieldFocusRequester)
                        .onFocusChanged { fieldFocused = it.isFocused }
                        // Remote Down leaves the field for the buttons (a multi-line field only from its
                        // last line), whatever input device the key comes from.
                        .onPreviewKeyEvent { event ->
                            val leaveDown = event.type == KeyEventType.KeyDown &&
                                event.key == Key.DirectionDown &&
                                (input.singleLine || text.text.indexOf('\n', text.selection.end) < 0)
                            if (leaveDown) focusManager.moveFocus(FocusDirection.Down) else false
                        },
                )
            }
            Spacer(Modifier.height(28.dp))
            TvActionRow(
                actions = listOf(
                    TvAction(stringResource(R.string.save), onClick = save),
                    TvAction(stringResource(R.string.cancel), onClick = onDone),
                ),
                requestInitialFocus = false,
            )
        }
    }
}

/**
 * Rows keep short summaries (values, states); longer explanations are only shown in the preview pane,
 * as in the system settings.
 */
private fun String?.rowSummary(): String? = this?.takeIf { it.length <= 60 }

/** Rows scrolling up under the page title fade out instead of being cut off, as in the system settings. */
private fun Modifier.fadingTopEdge(height: Dp = 28.dp): Modifier = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(0f to Color.Transparent, height.toPx() / size.height to Color.Black),
            blendMode = BlendMode.DstIn,
        )
    }

/** Scroll position and focused row of the [TvSettingsPage]s of one back-stack entry, by page title. */
class TvPageMemory internal constructor(private val entries: HashMap<String, String> = HashMap()) {
    internal data class Position(val index: Int, val offset: Int, val focusedKey: String?)

    internal fun get(title: String): Position? = entries[title]?.split(SEPARATOR, limit = 3)?.let { parts ->
        Position(parts[0].toInt(), parts[1].toInt(), parts.getOrNull(2)?.takeIf { it.isNotEmpty() })
    }

    internal fun put(title: String, index: Int, offset: Int, focusedKey: String?) {
        entries[title] = listOf(index, offset, focusedKey.orEmpty()).joinToString(SEPARATOR)
    }

    companion object {
        private const val SEPARATOR = "|"
        val Saver: Saver<TvPageMemory, HashMap<String, String>> = Saver(save = { HashMap(it.entries) }, restore = { TvPageMemory(it) })
    }
}

val LocalTvPageMemory = staticCompositionLocalOf<TvPageMemory?> { null }
