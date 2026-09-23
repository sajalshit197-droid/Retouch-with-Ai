package com.example.ui

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SamplePhotoItem
import com.example.data.SamplePhotoProvider
import com.example.data.ai.AiPhotoAnalysis
import com.example.data.ai.GeminiVisionService
import com.example.data.db.AppDatabase
import com.example.data.db.EditedProject
import com.example.data.repository.ProjectRepository
import com.example.domain.BackgroundMode
import com.example.domain.PhotoEditState
import com.example.domain.PhotoProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class PhotoEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProjectRepository
    private val geminiService = GeminiVisionService()

    init {
        val db = AppDatabase.getInstance(application)
        repository = ProjectRepository(db.editedProjectDao())
    }

    val savedProjects: StateFlow<List<EditedProject>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _sourceBitmap = MutableStateFlow<Bitmap?>(null)
    val sourceBitmap: StateFlow<Bitmap?> = _sourceBitmap.asStateFlow()

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private val _editState = MutableStateFlow(PhotoEditState())
    val editState: StateFlow<PhotoEditState> = _editState.asStateFlow()

    private val _cachedCutout = MutableStateFlow<Bitmap?>(null)

    private val _isRendering = MutableStateFlow(false)
    val isRendering: StateFlow<Boolean> = _isRendering.asStateFlow()

    private val _aiAnalysis = MutableStateFlow<AiPhotoAnalysis?>(null)
    val aiAnalysis: StateFlow<AiPhotoAnalysis?> = _aiAnalysis.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isComparing = MutableStateFlow(false)
    val isComparing: StateFlow<Boolean> = _isComparing.asStateFlow()

    private var renderJob: Job? = null

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun setComparing(comparing: Boolean) {
        _isComparing.value = comparing
    }

    fun loadSample(sampleId: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val bitmap = SamplePhotoProvider.createSampleBitmap(sampleId)
            setSource(bitmap)
        }
    }

    fun loadFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val original = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (original != null) {
                    // Normalize size for smooth interactive editing
                    val maxDimension = 1280
                    val scaled = if (original.width > maxDimension || original.height > maxDimension) {
                        val ratio = minOf(
                            maxDimension.toFloat() / original.width,
                            maxDimension.toFloat() / original.height
                        )
                        Bitmap.createScaledBitmap(
                            original,
                            (original.width * ratio).toInt(),
                            (original.height * ratio).toInt(),
                            true
                        )
                    } else {
                        original
                    }
                    setSource(scaled)
                } else {
                    _statusMessage.value = "Failed to load selected image"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error loading image: ${e.localizedMessage}"
            }
        }
    }

    fun loadFromBitmap(bitmap: Bitmap) {
        setSource(bitmap)
    }

    fun loadProject(context: Context, project: EditedProject) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var bitmap: Bitmap? = null
                if (project.imagePath.startsWith("content://")) {
                    val uri = Uri.parse(project.imagePath)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        bitmap = BitmapFactory.decodeStream(stream)
                    }
                } else {
                    val file = File(project.imagePath)
                    if (file.exists()) {
                        bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    } else if (project.thumbnailPath.isNotBlank() && File(project.thumbnailPath).exists()) {
                        bitmap = BitmapFactory.decodeFile(project.thumbnailPath)
                    }
                }

                if (bitmap != null) {
                    _sourceBitmap.value = bitmap
                    _cachedCutout.value = null
                    _editState.value = PhotoEditState(
                        isBgRemoved = project.bgMode != "ORIGINAL",
                        bgMode = try { BackgroundMode.valueOf(project.bgMode) } catch (e: Exception) { BackgroundMode.ORIGINAL },
                        selectedFilterId = project.filterName,
                        filterIntensity = project.filterIntensity,
                        brightness = project.brightness,
                        contrast = project.contrast,
                        exposure = project.exposure,
                        warmth = project.warmth,
                        saturation = project.saturation,
                        skinSmooth = project.skinSmooth,
                        faceGlow = project.faceGlow,
                        vignette = project.vignette
                    )
                    scheduleRender()
                } else {
                    _statusMessage.value = "Could not reload original file"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Failed to load project: ${e.localizedMessage}"
            }
        }
    }

    private fun setSource(bitmap: Bitmap) {
        _sourceBitmap.value = bitmap
        _cachedCutout.value = null
        _editState.value = PhotoEditState()
        _aiAnalysis.value = null
        scheduleRender()
    }

    fun updateEditState(update: (PhotoEditState) -> PhotoEditState) {
        val oldState = _editState.value
        val newState = update(oldState)
        _editState.value = newState

        // If background feather or removal changed, invalidate cutout cache
        if (oldState.bgFeather != newState.bgFeather) {
            _cachedCutout.value = null
        }

        scheduleRender()
    }

    private fun scheduleRender() {
        renderJob?.cancel()
        renderJob = viewModelScope.launch(Dispatchers.Default) {
            val src = _sourceBitmap.value ?: return@launch
            _isRendering.value = true
            delay(16) // Smooth 60fps debounce

            val state = _editState.value

            // If background removal is active and cutout not yet cached, generate & cache it
            var cutout = _cachedCutout.value
            if ((state.isBgRemoved || state.bgMode != BackgroundMode.ORIGINAL) && cutout == null) {
                cutout = PhotoProcessor.generateSubjectCutout(src, state.bgFeather)
                _cachedCutout.value = cutout
            }

            val result = PhotoProcessor.processCompletePhoto(src, state, cutout)
            _previewBitmap.value = result
            _isRendering.value = false
        }
    }

    fun resetEdits() {
        _editState.value = PhotoEditState()
        _cachedCutout.value = null
        scheduleRender()
        _statusMessage.value = "All edits reset"
    }

    fun runAiAnalysis() {
        val src = _sourceBitmap.value ?: return
        viewModelScope.launch {
            _isAnalyzing.value = true
            val result = geminiService.analyzeAndSuggestRetouch(src)
            result.onSuccess { analysis ->
                _aiAnalysis.value = analysis
                _statusMessage.value = "AI Analysis Complete (Score: ${analysis.overallScore}/100)"
            }.onFailure { error ->
                _statusMessage.value = "Analysis error: ${error.localizedMessage}"
            }
            _isAnalyzing.value = false
        }
    }

    fun applyAiRecommendations() {
        val analysis = _aiAnalysis.value ?: return
        updateEditState { current ->
            current.copy(
                brightness = analysis.suggestedBrightness,
                contrast = analysis.suggestedContrast,
                warmth = analysis.suggestedWarmth,
                saturation = analysis.suggestedSaturation,
                skinSmooth = analysis.suggestedSkinSmooth,
                faceGlow = analysis.suggestedFaceGlow,
                isBgRemoved = analysis.shouldRemoveBackground,
                bgMode = if (analysis.shouldRemoveBackground) BackgroundMode.BLUR_BOKEH else current.bgMode
            )
        }
        _statusMessage.value = "Applied AI Magic Retouch Settings!"
    }

    fun quickMagicEnhance() {
        // One-tap instant beauty enhancement
        val src = _sourceBitmap.value ?: return
        viewModelScope.launch {
            _isAnalyzing.value = true
            val result = geminiService.analyzeAndSuggestRetouch(src)
            result.onSuccess { analysis ->
                _aiAnalysis.value = analysis
                applyAiRecommendations()
            }.onFailure {
                // Fallback default magic retouch
                updateEditState {
                    it.copy(
                        brightness = 10f,
                        contrast = 12f,
                        warmth = 6f,
                        saturation = 10f,
                        skinSmooth = 35f,
                        faceGlow = 25f
                    )
                }
                _statusMessage.value = "Instant Magic Enhance Applied!"
            }
            _isAnalyzing.value = false
        }
    }

    fun saveEditedPhoto(context: Context, title: String) {
        val finalBitmap = _previewBitmap.value ?: _sourceBitmap.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val filename = "RetouchAI_${System.currentTimeMillis()}.png"
                var savedUri: Uri? = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/RetouchAI")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        }
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        context.contentResolver.update(uri, values, null, null)
                        savedUri = uri
                    }
                } else {
                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "RetouchAI")
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, filename)
                    FileOutputStream(file).use { stream ->
                        finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                    savedUri = Uri.fromFile(file)
                }

                // Save thumbnail locally for app project history
                val cacheDir = context.cacheDir
                val thumbFile = File(cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
                val thumbScale = Bitmap.createScaledBitmap(finalBitmap, 300, (300f * finalBitmap.height / finalBitmap.width).toInt(), true)
                FileOutputStream(thumbFile).use { out ->
                    thumbScale.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }

                val state = _editState.value
                val project = EditedProject(
                    title = if (title.isBlank()) "Retouch Edit" else title,
                    imagePath = savedUri?.toString() ?: thumbFile.absolutePath,
                    thumbnailPath = thumbFile.absolutePath,
                    filterName = state.selectedFilterId,
                    filterIntensity = state.filterIntensity,
                    bgMode = state.bgMode.name,
                    brightness = state.brightness,
                    contrast = state.contrast,
                    exposure = state.exposure,
                    warmth = state.warmth,
                    saturation = state.saturation,
                    skinSmooth = state.skinSmooth,
                    faceGlow = state.faceGlow,
                    vignette = state.vignette,
                    aiSummary = _aiAnalysis.value?.critique ?: "Professional Retouch"
                )
                repository.saveProject(project)

                withContext(Dispatchers.Main) {
                    _statusMessage.value = "Saved successfully to Gallery & Projects!"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _statusMessage.value = "Failed to save: ${e.localizedMessage}"
                }
            }
        }
    }

    fun deleteProject(project: EditedProject) {
        viewModelScope.launch {
            repository.deleteProject(project)
            _statusMessage.value = "Project removed"
        }
    }
}
