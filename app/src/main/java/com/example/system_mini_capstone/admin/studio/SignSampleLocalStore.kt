package com.example.system_mini_capstone.admin.studio

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Manages local offline-first JSON storage for recorded sign samples.
 */
object SignSampleLocalStore {
    private const val TAG = "SignSampleLocalStore"
    private const val LOCAL_FILE_NAME = "recorded_sign_samples_local.json"

    fun saveLocal(context: Context, newDtos: List<SignSampleDto>) {
        try {
            val existing = loadLocal(context).toMutableList()
            // Avoid duplicates by sampleId
            val existingIds = existing.map { it.sampleId }.toSet()
            for (dto in newDtos) {
                if (dto.sampleId !in existingIds) {
                    existing.add(dto)
                }
            }
            val file = File(context.filesDir, LOCAL_FILE_NAME)
            file.writeText(Gson().toJson(existing))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save sign samples locally", e)
        }
    }

    fun loadLocal(context: Context): List<SignSampleDto> {
        try {
            val file = File(context.filesDir, LOCAL_FILE_NAME)
            if (!file.exists()) return emptyList()
            val json = file.readText()
            val type = object : TypeToken<List<SignSampleDto>>() {}.type
            return Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load sign samples locally", e)
            return emptyList()
        }
    }

    fun deleteLocalForLabel(context: Context, label: String) {
        try {
            val existing = loadLocal(context)
            val filtered = existing.filter { !it.label.equals(label, ignoreCase = true) }
            val file = File(context.filesDir, LOCAL_FILE_NAME)
            file.writeText(Gson().toJson(filtered))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete local sign samples for label $label", e)
        }
    }
}
