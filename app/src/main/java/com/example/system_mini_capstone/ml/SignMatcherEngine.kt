package com.example.system_mini_capstone.ml

import android.content.Context
import android.util.Log
import com.example.system_mini_capstone.admin.studio.SignSampleDto
import com.example.system_mini_capstone.admin.studio.SignSampleLocalStore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import kotlin.math.sqrt

data class MatchResult(
    val label: String,
    val confidence: Float,
    val distance: Float,
    val debugInfo: String
)

class SignMatcherEngine(private val context: Context?) {

    private var sampleLabels = listOf<String>()
    private var sampleVectors = floatArrayOf() // flat array of 63 * N floats
    private var isLoaded = false
    private var sampleCount = 0

    val loadedCount: Int
        get() = sampleCount

    suspend fun loadSamples() = withContext(Dispatchers.IO) {
        val ctx = context
        val allDtos = mutableMapOf<String, SignSampleDto>()

        // 1. Load local offline samples first
        if (ctx != null) {
            val localDtos = SignSampleLocalStore.loadLocal(ctx)
            for (dto in localDtos) {
                if (dto.landmarks.size == 63 && dto.label.isNotBlank()) {
                    allDtos[dto.sampleId] = dto
                }
            }
        }

        // 2. Try loading from Firestore cloud
        try {
            val firestore = FirebaseFirestore.getInstance()
            val snapshot = withTimeoutOrNull(6000) {
                firestore.collection("signSamples").get().await()
            }

            if (snapshot != null) {
                for (doc in snapshot.documents) {
                    val dto = doc.toObject(SignSampleDto::class.java)
                    if (dto != null && dto.landmarks.size == 63 && dto.label.isNotBlank()) {
                        allDtos[dto.sampleId] = dto
                    }
                }
            }
        } catch (e: Exception) {
            try { Log.w(TAG, "Failed to fetch cloud samples (${e.message}), using local samples", e) } catch (_: Throwable) {}
        }

        val merged = allDtos.values.toList()
        if (merged.isNotEmpty()) {
            buildIndex(merged)
        } else {
            loadFromCache()
        }
    }

    private fun loadFromCache() {
        val ctx = context ?: return
        try {
            val cacheFile = File(ctx.filesDir, CACHE_FILE_NAME)
            if (cacheFile.exists()) {
                val json = cacheFile.readText()
                val type = object : TypeToken<List<SignSampleDto>>() {}.type
                val cachedDtos: List<SignSampleDto> = Gson().fromJson(json, type) ?: emptyList()
                buildIndex(cachedDtos)
            } else {
                buildIndex(emptyList())
            }
        } catch (e: Exception) {
            try { Log.e(TAG, "Failed to load sign samples from local cache", e) } catch (_: Throwable) {}
            buildIndex(emptyList())
        }
    }

    private fun buildIndex(dtos: List<SignSampleDto>) {
        sampleCount = dtos.size
        sampleLabels = dtos.map { it.label }
        val flatList = mutableListOf<Float>()
        for (dto in dtos) {
            flatList.addAll(dto.landmarks)
        }
        sampleVectors = flatList.toFloatArray()
        isLoaded = true
        try { Log.i(TAG, "SignMatcherEngine index built successfully with $sampleCount samples.") } catch (_: Throwable) {}
    }

    suspend fun match(normalizedLandmarks: List<Float>): MatchResult = withContext(Dispatchers.Default) {
        if (!isLoaded || sampleCount == 0 || normalizedLandmarks.size != 63) {
            return@withContext MatchResult("UNKNOWN", 0f, 999f, "Not loaded or empty")
        }

        val q = normalizedLandmarks.toFloatArray()
        val numSamples = sampleCount
        val distances = FloatArray(numSamples)

        // Compute Euclidean distance in under 5ms for 2000 samples
        val dim = 63
        for (i in 0 until numSamples) {
            var sumSq = 0f
            val baseIdx = i * dim
            for (j in 0 until dim) {
                val diff = q[j] - sampleVectors[baseIdx + j]
                sumSq += diff * diff
            }
            distances[i] = sqrt(sumSq)
        }

        // Find k nearest neighbors (k = 5)
        val k = K_NEIGHBORS.coerceAtMost(numSamples)
        val indices = distances.indices.sortedBy { distances[it] }.take(k)

        val bestDistance = distances[indices.first()]
        if (bestDistance > MAX_DISTANCE_THRESHOLD) {
            return@withContext MatchResult("UNKNOWN", 0f, bestDistance, "Dist ($bestDistance) > threshold ($MAX_DISTANCE_THRESHOLD)")
        }

        // Distance-weighted voting
        val labelWeights = mutableMapOf<String, Float>()
        for (idx in indices) {
            val dist = distances[idx]
            val label = sampleLabels[idx]
            val weight = 1f / (dist + 0.00001f)
            labelWeights[label] = (labelWeights[label] ?: 0f) + weight
        }

        val sortedVotes = labelWeights.entries.sortedByDescending { it.value }
        val topEntry = sortedVotes.first()
        val totalWeight = labelWeights.values.sum()
        val confidence = (topEntry.value / totalWeight).coerceIn(0f, 1f)

        val dominanceRatio = if (totalWeight > 0f) topEntry.value / totalWeight else 0f

        val debugInfo = String.format(Locale.US, "Top: %s (conf: %d%%, dist: %.2f)", topEntry.key, (confidence * 100).toInt(), bestDistance)

        if (dominanceRatio < MIN_DOMINANCE_RATIO) {
            return@withContext MatchResult("UNKNOWN", confidence, bestDistance, "Low dominance: $debugInfo")
        }

        MatchResult(topEntry.key, confidence, bestDistance, debugInfo)
    }

    companion object {
        private const val TAG = "SignMatcherEngine"
        private const val CACHE_FILE_NAME = "sign_samples_cache.json"
        private const val K_NEIGHBORS = 5
        private const val MAX_DISTANCE_THRESHOLD = 0.75f
        private const val MIN_DOMINANCE_RATIO = 0.45f
    }
}
