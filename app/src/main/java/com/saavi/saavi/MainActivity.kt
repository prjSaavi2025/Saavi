package com.saavi.saavi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            MainScreen()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    var selectedLanguage by remember { mutableStateOf("Malayalam") }
    val coroutineScope = rememberCoroutineScope()

    // ✅ Load saved language when the app starts
    LaunchedEffect(Unit) {
        preferencesManager.selectedLanguage.collect { savedLanguage ->
            selectedLanguage = savedLanguage
        }
    }

    // ✅ Initialize TTS once and update its language when needed
    val tts = remember {
        mutableStateOf<TextToSpeech?>(null)
    }

    LaunchedEffect(selectedLanguage) {
        tts.value = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.value?.language = when (selectedLanguage) {
                    "Malayalam" -> Locale("ml")
                    "Hindi" -> Locale("hi")
                    "Kannada" -> Locale("kn")
                    "English" -> Locale.ENGLISH
                    else -> Locale.ENGLISH
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Select Language:")

        DropdownMenuBox(selectedLanguage) { newLanguage ->
            selectedLanguage = newLanguage

            // ✅ Save selected language to DataStore
            coroutineScope.launch {
                preferencesManager.saveLanguage(newLanguage)
            }

            // ✅ Update TTS language dynamically
            tts.value?.language = when (newLanguage) {
                "Malayalam" -> Locale("ml")
                "Hindi" -> Locale("hi")
                "Kannada" -> Locale("kn")
                "English" -> Locale.ENGLISH
                else -> Locale.ENGLISH
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .clickable {
                    tts.value?.speak(
                        "Starting camera in $selectedLanguage",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        null
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "OPEN CAMERA", color = Color.White)
        }
    }
}


@Composable
fun DropdownMenuBox(selectedLanguage: String, onLanguageSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf("English","Malayalam", "Hindi", "Kannada")

    Box {
        Button(onClick = { expanded = true }) {
            Text(selectedLanguage)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            languages.forEach { language ->
                DropdownMenuItem(text = { Text(language) }, onClick = {
                    onLanguageSelected(language)
                    expanded = false
                })
            }
        }
    }
}

@Composable
fun CameraPreviewScreen() {
    val context = LocalContext.current
    val executor = ContextCompat.getMainExecutor(context)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build()

                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            context as ComponentActivity, cameraSelector, preview
                        )
                    } catch (exc: Exception) {
                        Log.e("Camera", "Failed to start camera", exc)
                    }
                }, executor)

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
