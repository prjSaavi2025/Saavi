package com.saavi.saavi.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties

object GeminiAIHelper {

    // Function to get API Key from local.properties
    private fun getApiKey(context: Context): String {
        return try {
            val properties = Properties()
            val inputStream = context.assets.open("local.properties")
            properties.load(inputStream)
            properties.getProperty("GEMINI_API_KEY", "DEFAULT_KEY")
        } catch (e: Exception) {
            Log.e("GeminiAI", "Error loading API key: ${e.message}")
            "DEFAULT_KEY" // Fallback API Key
        }
    }

    fun getGenerativeModel(context: Context): GenerativeModel {
        return GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = getApiKey(context)
        )
    }

    // Function for object detection
    suspend fun analyzeImage(context: Context, bitmap: Bitmap): String {
        val model = getGenerativeModel(context)
        return withContext(Dispatchers.IO) {
            try {
                val inputContent = content { image(bitmap) }
                val response = model.generateContent(inputContent)
                response.text ?: "No objects detected"
            } catch (e: Exception) {
                Log.e("GeminiAI", "Error: ${e.message}")
                "Error processing image"
            }
        }
    }
}
