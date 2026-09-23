package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.ai.AiPhotoAnalysis
import com.example.domain.BackgroundMode
import com.example.domain.PhotoEditState
import com.example.domain.PhotoProcessor
import com.example.ui.PhotoEditorViewModel
import com.example.ui.components.CheckerboardBackground
import com.example.ui.theme.*

enum class EditorTab {
    BG_REMOVE,
    RETOUCH,
    LIGHTING,
    FILTERS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: PhotoEditorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sourceBitmap by viewModel.sourceBitmap.collectAsState()
    val previewBitmap by viewModel.previewBitmap.collectAsState()
    val editState by viewModel.editState.collectAsState()
    val isRendering by viewModel.isRendering.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val aiAnalysis by viewModel.aiAnalysis.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isComparing by viewModel.isComparing.collectAsState()

    var selectedTab by remember { mutableStateOf(EditorTab.BG_REMOVE) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    var projectTitle by remember { mutableStateOf("Retouch Studio Edit") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StudioBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Photo Studio",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        if (isRendering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = AccentCyan,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Reset edits button
                    IconButton(
                        onClick = { viewModel.resetEdits() },
                        modifier = Modifier.testTag("reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = "Reset",
                            tint = TextSecondary
                        )
                    }

                    // AI Review / Analysis
                    IconButton(
                        onClick = {
                            viewModel.runAiAnalysis()
                            showAiDialog = true
                        },
                        modifier = Modifier.testTag("ai_review_button")
                    ) {
                        BadgedBox(badge = {
                            if (aiAnalysis != null) {
                                Badge(containerColor = AccentEmerald)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Review",
                                tint = AccentVioletLight
                            )
                        }
                    }

                    // Save Button
                    FilledTonalButton(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_button"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AccentViolet,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Save", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Photo Darkroom Canvas View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF070709))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background pattern when transparent
                if (editState.isBgRemoved && editState.bgMode == BackgroundMode.TRANSPARENT) {
                    CheckerboardBackground(modifier = Modifier.fillMaxSize())
                }

                val displayBitmap = if (isComparing) sourceBitmap else (previewBitmap ?: sourceBitmap)

                if (displayBitmap != null) {
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = "Editing preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    CircularProgressIndicator(color = AccentViolet)
                }

                // Comparison Badge Overlay
                if (isComparing) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, AccentAmber)
                    ) {
                        Text(
                            text = "ORIGINAL (UNTOUCHED)",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = AccentAmber
                        )
                    }
                }

                // Interactive Compare Button Overlay
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    viewModel.setComparing(true)
                                    tryAwaitRelease()
                                    viewModel.setComparing(false)
                                }
                            )
                        }
                        .testTag("compare_hold_button"),
                    color = StudioSurface.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Hold to Compare",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = TextPrimary
                        )
                    }
                }

                // AI Magic Enhance Quick Pill on canvas
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clickable { viewModel.quickMagicEnhance() }
                        .testTag("quick_magic_button"),
                    color = AccentViolet.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "AI Magic Fix",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            // 2. Active Tool Controls Area
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 210.dp, max = 290.dp),
                color = StudioSurface,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                border = BorderStroke(1.dp, StudioBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Tool panels switch
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (selectedTab) {
                            EditorTab.BG_REMOVE -> BgRemoveControls(
                                state = editState,
                                onUpdate = { viewModel.updateEditState(it) }
                            )
                            EditorTab.RETOUCH -> RetouchControls(
                                state = editState,
                                onUpdate = { viewModel.updateEditState(it) },
                                onMagicRetouch = { viewModel.quickMagicEnhance() },
                                onReviewWithAi = {
                                    viewModel.runAiAnalysis()
                                    showAiDialog = true
                                },
                                isAnalyzing = isAnalyzing
                            )
                            EditorTab.LIGHTING -> LightingControls(
                                state = editState,
                                onUpdate = { viewModel.updateEditState(it) }
                            )
                            EditorTab.FILTERS -> FiltersControls(
                                state = editState,
                                onUpdate = { viewModel.updateEditState(it) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bottom Navigation Tab Bar
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        containerColor = StudioSurfaceVariant,
                        contentColor = AccentVioletLight,
                        indicator = {
                            TabRowDefaults.PrimaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(selectedTab.ordinal),
                                color = AccentViolet
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        EditorTab.entries.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = {
                                    Text(
                                        text = when (tab) {
                                            EditorTab.BG_REMOVE -> "BG Remove"
                                            EditorTab.RETOUCH -> "AI Retouch"
                                            EditorTab.LIGHTING -> "Lighting"
                                            EditorTab.FILTERS -> "Filters"
                                        },
                                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = when (tab) {
                                            EditorTab.BG_REMOVE -> Icons.Default.CropFree
                                            EditorTab.RETOUCH -> Icons.Default.Face
                                            EditorTab.LIGHTING -> Icons.Default.Tune
                                            EditorTab.FILTERS -> Icons.Default.FilterVintage
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }
    }

    // AI Analysis Review Dialog
    if (showAiDialog) {
        AiAnalysisDialog(
            analysis = aiAnalysis,
            isLoading = isAnalyzing,
            onDismiss = { showAiDialog = false },
            onApply = {
                viewModel.applyAiRecommendations()
                showAiDialog = false
            }
        )
    }

    // Save Image Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(
                    text = "Save Edited Photo",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter a title for your project. The image will be exported in high resolution to your device gallery and project history.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = projectTitle,
                        onValueChange = { projectTitle = it },
                        label = { Text("Project Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentViolet,
                            unfocusedBorderColor = StudioBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveEditedPhoto(context, projectTitle)
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                    modifier = Modifier.testTag("confirm_save_button")
                ) {
                    Text("Export & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = StudioSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ---------------------- 1. BG Remove Controls ----------------------
@Composable
fun BgRemoveControls(
    state: PhotoEditState,
    onUpdate: ((PhotoEditState) -> PhotoEditState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Toggle Background Removal
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(StudioSurfaceVariant)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = null,
                    tint = if (state.isBgRemoved) AccentVioletLight else TextSecondary
                )
                Column {
                    Text(
                        text = "Remove Background",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = if (state.isBgRemoved) "Subject extracted cleanly" else "Enable to isolate subject",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Switch(
                checked = state.isBgRemoved,
                onCheckedChange = { isChecked ->
                    onUpdate {
                        it.copy(
                            isBgRemoved = isChecked,
                            bgMode = if (isChecked && it.bgMode == BackgroundMode.ORIGINAL)
                                BackgroundMode.TRANSPARENT
                            else if (!isChecked)
                                BackgroundMode.ORIGINAL
                            else
                                it.bgMode
                        )
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AccentViolet
                ),
                modifier = Modifier.testTag("bg_remove_switch")
            )
        }

        if (state.isBgRemoved) {
            Text(
                text = "Choose Background Style",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary
            )

            // Mode Selector: Transparent, Solid Color, Studio Gradient, Blur Bokeh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BgModeButton(
                    title = "Transparent",
                    icon = Icons.Default.GridOn,
                    isSelected = state.bgMode == BackgroundMode.TRANSPARENT,
                    onClick = { onUpdate { it.copy(bgMode = BackgroundMode.TRANSPARENT) } }
                )
                BgModeButton(
                    title = "Solid Color",
                    icon = Icons.Default.Palette,
                    isSelected = state.bgMode == BackgroundMode.SOLID_COLOR,
                    onClick = { onUpdate { it.copy(bgMode = BackgroundMode.SOLID_COLOR) } }
                )
                BgModeButton(
                    title = "Gradient",
                    icon = Icons.Default.Gradient,
                    isSelected = state.bgMode == BackgroundMode.STUDIO_GRADIENT,
                    onClick = { onUpdate { it.copy(bgMode = BackgroundMode.STUDIO_GRADIENT) } }
                )
                BgModeButton(
                    title = "Blur Bokeh",
                    icon = Icons.Default.BlurOn,
                    isSelected = state.bgMode == BackgroundMode.BLUR_BOKEH,
                    onClick = { onUpdate { it.copy(bgMode = BackgroundMode.BLUR_BOKEH) } }
                )
            }

            // Solid Color Swatches
            if (state.bgMode == BackgroundMode.SOLID_COLOR) {
                val colors = listOf(
                    android.graphics.Color.WHITE to "White",
                    android.graphics.Color.parseColor("#18181B") to "Studio Dark",
                    android.graphics.Color.parseColor("#64748B") to "Slate",
                    android.graphics.Color.parseColor("#FEF3C7") to "Warm Cream",
                    android.graphics.Color.parseColor("#D1FAE5") to "Mint",
                    android.graphics.Color.parseColor("#FFE4E6") to "Pastel Pink",
                    android.graphics.Color.parseColor("#312E81") to "Royal Navy",
                    android.graphics.Color.parseColor("#065F46") to "Deep Emerald"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(colors) { (colorInt, name) ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(colorInt))
                                .border(
                                    width = if (state.solidColor == colorInt) 3.dp else 1.dp,
                                    color = if (state.solidColor == colorInt) AccentViolet else StudioBorder,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onUpdate { it.copy(solidColor = colorInt) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.solidColor == colorInt) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (colorInt == android.graphics.Color.WHITE || colorInt == android.graphics.Color.parseColor("#FEF3C7")) Color.Black else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Studio Gradients
            if (state.bgMode == BackgroundMode.STUDIO_GRADIENT) {
                val gradients = listOf(
                    Triple(android.graphics.Color.parseColor("#312E81"), android.graphics.Color.parseColor("#831843"), "Cyber Sunset"),
                    Triple(android.graphics.Color.parseColor("#78350F"), android.graphics.Color.parseColor("#D97706"), "Golden Studio"),
                    Triple(android.graphics.Color.parseColor("#0F172A"), android.graphics.Color.parseColor("#0284C7"), "Deep Cyber"),
                    Triple(android.graphics.Color.parseColor("#3B0764"), android.graphics.Color.parseColor("#14B8A6"), "Neon Aurora")
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(gradients) { (start, end, label) ->
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(Color(start), Color(end))))
                                .border(
                                    width = if (state.gradientStart == start && state.gradientEnd == end) 3.dp else 1.dp,
                                    color = if (state.gradientStart == start && state.gradientEnd == end) Color.White else StudioBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onUpdate { it.copy(gradientStart = start, gradientEnd = end) }
                                }
                        )
                    }
                }
            }

            // Blur Slider
            if (state.bgMode == BackgroundMode.BLUR_BOKEH) {
                AdjustSlider(
                    label = "Bokeh Depth Blur",
                    value = state.blurRadius,
                    valueRange = 5f..45f,
                    displayValue = "${state.blurRadius.toInt()} px",
                    onValueChange = { newVal -> onUpdate { s -> s.copy(blurRadius = newVal) } }
                )
            }

            // Edge Feathering
            AdjustSlider(
                label = "Edge Softness / Feather",
                value = state.bgFeather,
                valueRange = 1f..25f,
                displayValue = "${state.bgFeather.toInt()} px",
                onValueChange = { newVal -> onUpdate { s -> s.copy(bgFeather = newVal) } }
            )
        }
    }
}

@Composable
fun RowScope.BgModeButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) AccentViolet.copy(alpha = 0.25f) else StudioSurfaceVariant,
        border = BorderStroke(
            1.dp,
            if (isSelected) AccentViolet else StudioBorder
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) AccentVioletLight else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

// ---------------------- 2. AI Retouch Controls ----------------------
@Composable
fun RetouchControls(
    state: PhotoEditState,
    onUpdate: ((PhotoEditState) -> PhotoEditState) -> Unit,
    onMagicRetouch: () -> Unit,
    onReviewWithAi: () -> Unit,
    isAnalyzing: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Instant Magic Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onMagicRetouch,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("ai_magic_retouch_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("AI Auto-Retouch", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onReviewWithAi,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("ask_ai_expert_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan),
                border = BorderStroke(1.dp, AccentCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = AccentCyan,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Review")
                }
            }
        }

        // Retouch Sliders
        AdjustSlider(
            label = "Skin Smoothing (Blemish Soften)",
            value = state.skinSmooth,
            valueRange = 0f..100f,
            displayValue = "${state.skinSmooth.toInt()}%",
            onValueChange = { newVal -> onUpdate { s -> s.copy(skinSmooth = newVal) } },
            onReset = { onUpdate { s -> s.copy(skinSmooth = 0f) } }
        )

        AdjustSlider(
            label = "Face Glow / Subject Spotlight",
            value = state.faceGlow,
            valueRange = 0f..100f,
            displayValue = "${state.faceGlow.toInt()}%",
            onValueChange = { newVal -> onUpdate { s -> s.copy(faceGlow = newVal) } },
            onReset = { onUpdate { s -> s.copy(faceGlow = 0f) } }
        )

        AdjustSlider(
            label = "Eye Clarity & Detail Accent",
            value = state.eyeClarity,
            valueRange = 0f..100f,
            displayValue = "${state.eyeClarity.toInt()}%",
            onValueChange = { newVal -> onUpdate { s -> s.copy(eyeClarity = newVal) } },
            onReset = { onUpdate { s -> s.copy(eyeClarity = 0f) } }
        )
    }
}

// ---------------------- 3. Lighting & Color Controls ----------------------
@Composable
fun LightingControls(
    state: PhotoEditState,
    onUpdate: ((PhotoEditState) -> PhotoEditState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AdjustSlider(
            label = "Exposure",
            value = state.exposure,
            valueRange = -100f..100f,
            displayValue = "${state.exposure.toInt()}",
            onValueChange = { newVal -> onUpdate { s -> s.copy(exposure = newVal) } },
            onReset = { onUpdate { s -> s.copy(exposure = 0f) } }
        )

        AdjustSlider(
            label = "Brightness",
            value = state.brightness,
            valueRange = -100f..100f,
            displayValue = "${state.brightness.toInt()}",
            onValueChange = { newVal -> onUpdate { s -> s.copy(brightness = newVal) } },
            onReset = { onUpdate { s -> s.copy(brightness = 0f) } }
        )

        AdjustSlider(
            label = "Contrast",
            value = state.contrast,
            valueRange = -100f..100f,
            displayValue = "${state.contrast.toInt()}",
            onValueChange = { newVal -> onUpdate { s -> s.copy(contrast = newVal) } },
            onReset = { onUpdate { s -> s.copy(contrast = 0f) } }
        )

        AdjustSlider(
            label = "Warmth (Temperature)",
            value = state.warmth,
            valueRange = -100f..100f,
            displayValue = "${state.warmth.toInt()}",
            onValueChange = { newVal -> onUpdate { s -> s.copy(warmth = newVal) } },
            onReset = { onUpdate { s -> s.copy(warmth = 0f) } }
        )

        AdjustSlider(
            label = "Saturation (Vibrance)",
            value = state.saturation,
            valueRange = -100f..100f,
            displayValue = "${state.saturation.toInt()}",
            onValueChange = { newVal -> onUpdate { s -> s.copy(saturation = newVal) } },
            onReset = { onUpdate { s -> s.copy(saturation = 0f) } }
        )

        AdjustSlider(
            label = "Color Tint",
            value = state.tint,
            valueRange = -100f..100f,
            displayValue = "${state.tint.toInt()}",
            onValueChange = { newVal -> onUpdate { s -> s.copy(tint = newVal) } },
            onReset = { onUpdate { s -> s.copy(tint = 0f) } }
        )

        AdjustSlider(
            label = "Vignette",
            value = state.vignette,
            valueRange = 0f..100f,
            displayValue = "${state.vignette.toInt()}%",
            onValueChange = { newVal -> onUpdate { s -> s.copy(vignette = newVal) } },
            onReset = { onUpdate { s -> s.copy(vignette = 0f) } }
        )
    }
}

// ---------------------- 4. Filters & Presets Controls ----------------------
@Composable
fun FiltersControls(
    state: PhotoEditState,
    onUpdate: ((PhotoEditState) -> PhotoEditState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Filter Intensity Slider (if non-original selected)
        if (state.selectedFilterId != "original") {
            AdjustSlider(
                label = "Filter Intensity",
                value = state.filterIntensity * 100f,
                valueRange = 0f..100f,
                displayValue = "${(state.filterIntensity * 100).toInt()}%",
                onValueChange = { newVal -> onUpdate { s -> s.copy(filterIntensity = newVal / 100f) } }
            )
        }

        Text(
            text = "Select Preset Look",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = TextSecondary
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(PhotoProcessor.AVAILABLE_FILTERS) { filter ->
                val isSelected = state.selectedFilterId == filter.id

                Surface(
                    modifier = Modifier
                        .width(110.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onUpdate {
                                it.copy(
                                    selectedFilterId = filter.id,
                                    filterIntensity = if (filter.id == "original") 1f else it.filterIntensity
                                )
                            }
                        }
                        .testTag("filter_item_${filter.id}"),
                    color = if (isSelected) AccentViolet.copy(alpha = 0.25f) else StudioSurfaceVariant,
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) AccentViolet else StudioBorder
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        when (filter.id) {
                                            "warm_studio" -> listOf(Color(0xFFD97706), Color(0xFFFBBF24))
                                            "vivid_pop" -> listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))
                                            "cool_cinema" -> listOf(Color(0xFF0284C7), Color(0xFFEA580C))
                                            "noir_silver" -> listOf(Color(0xFF18181B), Color(0xFF71717A))
                                            "golden_hour" -> listOf(Color(0xFFB45309), Color(0xFFF59E0B))
                                            "cyber_neon" -> listOf(Color(0xFF7C3AED), Color(0xFF06B6D4))
                                            "crisp_hdr" -> listOf(Color(0xFF059669), Color(0xFF10B981))
                                            "pastel_dream" -> listOf(Color(0xFFF472B6), Color(0xFF93C5FD))
                                            "vintage_film" -> listOf(Color(0xFF92400E), Color(0xFFD97706))
                                            else -> listOf(StudioBorder, StudioSurfaceVariant)
                                        }
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = filter.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) TextPrimary else TextSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ---------------------- Shared Adjustment Slider ----------------------
@Composable
fun AdjustSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit,
    onReset: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = TextSecondary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AccentVioletLight
                )
                if (onReset != null && value != 0f) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = TextTertiary,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable(onClick = onReset)
                    )
                }
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = AccentViolet,
                activeTrackColor = AccentViolet,
                inactiveTrackColor = StudioBorder
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}

// ---------------------- AI Analysis Bottom Dialog ----------------------
@Composable
fun AiAnalysisDialog(
    analysis: AiPhotoAnalysis?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            color = StudioSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AccentViolet.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AccentVioletLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "AI Photo Review",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                if (isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = AccentViolet)
                        Text(
                            text = "Analyzing lighting, composition & skin tones...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                } else if (analysis != null) {
                    // Overall Score Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = StudioSurfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Aesthetic Quality Score",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${analysis.overallScore} / 100",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = if (analysis.overallScore >= 80) AccentEmerald else AccentAmber
                                )
                            }
                            Icon(
                                imageVector = if (analysis.overallScore >= 80) Icons.Default.Verified else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (analysis.overallScore >= 80) AccentEmerald else AccentAmber,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Critique
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Expert Critique",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = analysis.critique,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    // Recommendations
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Suggested Adjustments",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        analysis.recommendations.forEach { rec ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AccentVioletLight,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(top = 2.dp)
                                )
                                Text(
                                    text = rec,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onApply,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("apply_ai_recommendations_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply AI Retouch Settings", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "Unable to analyze photo. Please try again.",
                        color = AccentRose,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
