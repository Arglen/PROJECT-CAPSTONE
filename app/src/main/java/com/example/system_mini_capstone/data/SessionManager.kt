package com.example.system_mini_capstone.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.system_mini_capstone.data.network.AuthResponse
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * Manages secure user session storage and token validation using EncryptedSharedPreferences.
 */
@Suppress("DEPRECATION")
class SessionManager(context: Context) {

    private val sharedPreferences: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (t: Throwable) {
        Log.e(TAG, "Failed to initialize EncryptedSharedPreferences, clearing and falling back to private prefs", t)
        try {
            context.deleteSharedPreferences(PREFS_FILE_NAME)
        } catch (_: Throwable) {}
        context.getSharedPreferences(FALLBACK_PREFS_FILE_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Persists the complete authentication session returned from OAuth2 / JWT backend.
     */
    fun saveAuthSession(response: AuthResponse) {
        sharedPreferences.edit().apply {
            putString(KEY_AUTH_TOKEN, response.accessToken)
            putString(KEY_REFRESH_TOKEN, response.refreshToken)
            response.user?.let { user ->
                putString(KEY_USER_ROLE, user.role)
                putString(KEY_USER_EMAIL, user.email)
                putString(KEY_USERNAME, user.username)
            }
            apply()
        }
    }

    /**
     * Persists the authentication token directly.
     */
    fun saveAuthToken(token: String) {
        sharedPreferences.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .apply()
    }

    /**
     * Retrieves the stored authentication token.
     */
    fun getAuthToken(): String? {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null)
    }

    /**
     * Retrieves the stored refresh token.
     */
    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    /**
     * Retrieves the role assigned to the authenticated user ("USER" or "ADMIN").
     */
    fun getUserRole(): String {
        return sharedPreferences.getString(KEY_USER_ROLE, "USER") ?: "USER"
    }

    /**
     * Retrieves the email of the authenticated user.
     */
    fun getUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    /**
     * Retrieves the username of the authenticated user.
     */
    fun getUsername(): String? {
        return sharedPreferences.getString(KEY_USERNAME, null)
    }

    /**
     * Checks if a valid authentication token exists and has not expired.
     * Supports JWT validation by decoding the exp claim, or fallback presence check.
     */
    fun isSessionValid(): Boolean {
        val token = getAuthToken() ?: return false
        if (token.isBlank()) return false

        return if (isJwtToken(token)) {
            val isExpired = isJwtExpired(token)
            if (isExpired) {
                Log.w(TAG, "Stored JWT has expired. Invalidating session.")
                clearSession()
                false
            } else {
                true
            }
        } else {
            // Non-JWT opaque token fallback: considered valid if present
            true
        }
    }

    /**
     * Clears all session data (logout / expired token).
     */
    fun clearSession() {
        sharedPreferences.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ROLE)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USERNAME)
            .apply()
    }

    private fun isJwtToken(token: String): Boolean {
        return token.split(".").size == 3
    }

    /**
     * Decodes the JWT payload and verifies the "exp" (expiration) claim against the current time.
     */
    private fun isJwtExpired(jwtToken: String): Boolean {
        return try {
            val parts = jwtToken.split(".")
            if (parts.size < 2) return true

            val payloadEncoded = parts[1]
            val decodedBytes = Base64.decode(payloadEncoded, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            val payloadJson = String(decodedBytes, StandardCharsets.UTF_8)
            val jsonObject = JSONObject(payloadJson)

            if (jsonObject.has("exp")) {
                val expSeconds = jsonObject.getLong("exp")
                val currentTimeSeconds = System.currentTimeMillis() / 1000
                currentTimeSeconds >= expSeconds
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding or parsing JWT payload", e)
            true
        }
    }

    companion object {
        private const val TAG = "SessionManager"
        private const val PREFS_FILE_NAME = "secure_user_session_prefs"
        private const val FALLBACK_PREFS_FILE_NAME = "user_session_fallback_prefs"
        private const val KEY_AUTH_TOKEN = "key_auth_token"
        private const val KEY_REFRESH_TOKEN = "key_refresh_token"
        private const val KEY_USER_ROLE = "key_user_role"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_USERNAME = "key_username"
    }
}
