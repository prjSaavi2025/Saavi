package com.saavi.saavi.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream

object DeepSeekAIHelper {

    private fun getApiKey(context: Context): String {
        return try {
            val sharedPrefs = context.getSharedPreferences("api_keys", Context.MODE_PRIVATE)
            sharedPrefs.getString("DEEPSEEK_API_KEY", "YOUR_API_KEY_HERE") ?: "YOUR_API_KEY_HERE"
        } catch (e: Exception) {
            Log.e("DeepSeekAI", "Error loading API key: ${e.message}")
            "YOUR_API_KEY_HERE"
        }
    }

    suspend fun analyzeImage(context: Context, bitmap: Bitmap): String {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = getApiKey(context)
                val client = OkHttpClient()

                // Convert bitmap to JPEG byte array
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val imageBytes = byteArrayOutputStream.toByteArray()

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "image.jpg",
                        RequestBody.create("image/jpeg".toMediaType(), imageBytes))
                    .build()

                val request = Request.Builder()
                    .url("https://api.deepseek.com/v1/image/analyze")  // Update if needed
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e("DeepSeekAI", "API Error: ${response.code}")
                    return@withContext "Error: API request failed"
                }

                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) {
                    return@withContext "Error: Empty response"
                }

                val json = JSONObject(responseBody)
                return@withContext json.optString("description", "No objects detected")
            } catch (e: Exception) {
                Log.e("DeepSeekAI", "Error: ${e.message}")
                return@withContext "Error processing image"
            }
        }
    }
}
