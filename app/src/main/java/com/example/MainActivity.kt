package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.PhotoEditorViewModel
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme

enum class Screen {
    HOME,
    EDITOR
}

class MainActivity : ComponentActivity() {
    private val viewModel: PhotoEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RetouchApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun RetouchApp(
    viewModel: PhotoEditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()
    val sourceBitmap by viewModel.sourceBitmap.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    // If sourceBitmap is set and user navigated to editor, stay on EDITOR
    LaunchedEffect(sourceBitmap) {
        if (sourceBitmap != null && currentScreen == Screen.HOME) {
            currentScreen = Screen.EDITOR
        }
    }

    BackHandler(enabled = currentScreen == Screen.EDITOR) {
        currentScreen = Screen.HOME
    }

    Crossfade(
        targetState = currentScreen,
        label = "ScreenTransition",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            Screen.HOME -> HomeScreen(
                savedProjects = savedProjects,
                onSelectSample = { sampleId ->
                    viewModel.loadSample(sampleId)
                    currentScreen = Screen.EDITOR
                },
                onSelectUri = { uri ->
                    viewModel.loadFromUri(context, uri)
                    currentScreen = Screen.EDITOR
                },
                onSelectBitmap = { bitmap ->
                    viewModel.loadFromBitmap(bitmap)
                    currentScreen = Screen.EDITOR
                },
                onOpenProject = { project ->
                    viewModel.loadProject(context, project)
                    currentScreen = Screen.EDITOR
                },
                onDeleteProject = { project ->
                    viewModel.deleteProject(project)
                }
            )

            Screen.EDITOR -> EditorScreen(
                viewModel = viewModel,
                onBack = {
                    currentScreen = Screen.HOME
                }
            )
        }
    }
}

