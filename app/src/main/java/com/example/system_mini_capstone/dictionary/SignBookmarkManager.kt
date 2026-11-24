package com.example.system_mini_capstone.dictionary

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages local persistence of bookmarked/saved sign IDs.
 */
class SignBookmarkManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sign_bookmarks_prefs", Context.MODE_PRIVATE)

    fun getSavedSignIds(userId: String = "guest"): Set<String> {
        return prefs.getStringSet("saved_signs_$userId", emptySet()) ?: emptySet()
    }

    fun isBookmarked(signId: String, userId: String = "guest"): Boolean {
        return getSavedSignIds(userId).contains(signId)
    }

    fun toggleBookmark(signId: String, userId: String = "guest"): Boolean {
        val current = getSavedSignIds(userId).toMutableSet()
        val isAdded = if (current.contains(signId)) {
            current.remove(signId)
            false
        } else {
            current.add(signId)
            true
        }
        prefs.edit().putStringSet("saved_signs_$userId", current).apply()
        return isAdded
    }
}
