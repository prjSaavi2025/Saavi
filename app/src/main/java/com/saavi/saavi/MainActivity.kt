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
import androidx.camera.core.*
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.saavi.saavi.ai.GeminiAIHelper
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

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
    val coroutineScope = rememberCoroutineScope()
    var showCamera by remember { mutableStateOf(false) }
    val selectedLanguage by preferencesManager.selectedLanguage.collectAsState(initial = "Malayalam")

    val tts = remember {
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d("TTS", "Text-to-Speech initialized successfully")
            }
        }
    }

    // Request Camera Permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Log.e("CameraPermission", "Camera permission denied!")
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(selectedLanguage) {
        tts.language = when (selectedLanguage) {
            "Malayalam" -> Locale("ml")
            "Hindi" -> Locale("hi")
            "Kannada" -> Locale("kn")
            "English" -> Locale.ENGLISH
            else -> Locale.ENGLISH
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Select Language:")

        DropdownMenuBox(selectedLanguage) { newLanguage ->
            coroutineScope.launch {
                preferencesManager.saveLanguage(newLanguage)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .clickable {
                    tts.speak(
                        "Starting camera in $selectedLanguage",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        null
                    )
                    showCamera = true
                },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "OPEN CAMERA", color = Color.White)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Internet Status Indicator
        InternetStatusIndicator()
    }

    if (showCamera) {
        CameraPreviewScreen(selectedLanguage)
    }
}

@Composable
fun DropdownMenuBox(selectedLanguage: String, onLanguageSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf("English", "Malayalam", "Hindi", "Kannada")

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
fun CameraPreviewScreen(selectedLanguage: String) {
    val context = LocalContext.current
    val executor = ContextCompat.getMainExecutor(context)
    val lifecycleOwner = LocalLifecycleOwner.current
    val tts = remember { TextToSpeech(context, null) }
    var detectedText by remember { mutableStateOf("Detecting objects...") }
    var lastProcessedTime by remember { mutableStateOf(0L) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build()
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { imageProxy ->
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastProcessedTime >= 2000) {
                            lastProcessedTime = currentTime
                            val bitmap = imageProxy.toBitmap()
                            imageProxy.close()
                            bitmap?.let { safeBitmap ->
                                CoroutineScope(Dispatchers.Main).launch {
                                    try {
                                        val analysisResult = GeminiAIHelper.analyzeImage(context, safeBitmap)
                                        detectedText = analysisResult
                                        tts.speak(analysisResult, TextToSpeech.QUEUE_FLUSH, null, null)
                                    } catch (e: Exception) {
                                        detectedText = "Error processing image"
                                        Log.e("AI_PROCESS", "Error: ${e.message}")
                                    }
                                }
                            }
                        } else {
                            imageProxy.close()
                        }
                    })

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )

                    preview.setSurfaceProvider(previewView.surfaceProvider)
                }, executor)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        Text(text = detectedText, color = Color.White, fontSize = 18.sp, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
fun InternetStatusIndicator() {
    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(isInternetAvailable(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            isConnected = isInternetAvailable(context)
            delay(5000)
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth().background(if (isConnected) Color.Green else Color.Red),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isConnected) "Online" else "Offline",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.padding(8.dp)
        )
    }
}

fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
            activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
}

fun ImageProxy.toBitmap(): Bitmap? {
    return try {
        val buffer = this.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        Log.e("ImageProxy", "Error converting ImageProxy to Bitmap: ${e.message}")
        null
    }
}
