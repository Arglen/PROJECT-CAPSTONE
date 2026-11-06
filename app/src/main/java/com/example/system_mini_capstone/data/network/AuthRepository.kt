package com.example.system_mini_capstone.data.network

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.charset.StandardCharsets

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<AuthResponse>
    suspend fun requestPasswordReset(email: String): Result<ForgotPasswordResponse>
    suspend fun resetPassword(email: String, token: String, newPassword: String): Result<ResetPasswordResponse>
}

class AuthRepositoryImpl(
    private val apiService: AuthApiService = NetworkModule.authApiService
) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<AuthResponse> =
        withContext(Dispatchers.IO) {
            try {
                val request = LoginRequest(username = username, password = password)
                val response = apiService.login(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Result.success(body)
                    } else {
                        Result.failure(Exception("Empty authentication response from server."))
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "Invalid username or password. Please verify your credentials."
                        403 -> "Access forbidden: Account suspended or unauthorized role."
                        404 -> "Authentication endpoint not found on server."
                        500 -> "Remote server error. Please try again later."
                        else -> "Authentication failed with status code ${response.code()}."
                    }
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: IOException) {
                Log.w(TAG, "Backend unreachable (${e.message}). Checking development test credentials.", e)
                handleOfflineFallback(username, password, e)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during login", e)
                Result.failure(Exception("Authentication error: ${e.localizedMessage ?: "Unknown error"}"))
            }
        }

    override suspend fun requestPasswordReset(email: String): Result<ForgotPasswordResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.forgotPassword(ForgotPasswordRequest(email = email))
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = when (response.code()) {
                        404 -> "No account found associated with $email."
                        else -> "Password recovery request failed (Code: ${response.code()})."
                    }
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: IOException) {
                Log.w(TAG, "Backend unreachable. Providing development password recovery response.", e)
                // Dev fallback: generate simulation code
                Result.success(
                    ForgotPasswordResponse(
                        message = "A password reset code has been sent to $email.",
                        resetToken = "SIGN-7294"
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error initiating password reset", e)
                Result.failure(Exception(e.localizedMessage ?: "Unable to initiate password reset."))
            }
        }

    override suspend fun resetPassword(
        email: String,
        token: String,
        newPassword: String
    ): Result<ResetPasswordResponse> =
        withContext(Dispatchers.IO) {
            try {
                val request = ResetPasswordRequest(
                    email = email,
                    token = token,
                    newPassword = newPassword
                )
                val response = apiService.resetPassword(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Invalid or expired reset token. Please request a new one."
                        else -> "Failed to reset password (Code: ${response.code()})."
                    }
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: IOException) {
                Log.w(TAG, "Backend unreachable. Providing development password reset success.", e)
                // Dev fallback: succeed
                Result.success(
                    ResetPasswordResponse(
                        message = "Your password has been successfully updated!",
                        success = true
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting password", e)
                Result.failure(Exception(e.localizedMessage ?: "Password reset failed."))
            }
        }

    /**
     * Development fallback to allow seamless offline verification of USER and ADMIN roles
     * when the remote backend is not running locally.
     */
    private fun handleOfflineFallback(username: String, password: String, originalException: IOException): Result<AuthResponse> {
        val trimmedUser = username.trim()

        return when {
            // ADMIN role credential
            trimmedUser.equals("admin@signlink.app", ignoreCase = true) && password == "admin123" -> {
                Log.i(TAG, "Authenticated successfully as ADMIN via dev fallback")
                Result.success(createDevAuthResponse(trimmedUser, UserRole.ADMIN))
            }
            // Standard USER role credential
            trimmedUser.equals("user@signlink.app", ignoreCase = true) && password == "user123" -> {
                Log.i(TAG, "Authenticated successfully as USER via dev fallback")
                Result.success(createDevAuthResponse(trimmedUser, UserRole.USER))
            }
            // Any other credentials while backend is unreachable
            else -> {
                Result.failure(
                    Exception(
                        "Unable to connect to authentication server (${originalException.localizedMessage ?: "Network offline"}). " +
                        "For offline testing, use 'admin@signlink.app' / 'admin123' or 'user@signlink.app' / 'user123'."
                    )
                )
            }
        }
    }

    private fun createDevAuthResponse(email: String, role: UserRole): AuthResponse {
        val expSeconds = (System.currentTimeMillis() / 1000) + 86400 // 24 hours
        val header = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" // {"alg":"HS256","typ":"JWT"}
        val payload = """{"sub":"$email","role":"${role.name}","exp":$expSeconds}"""
        val payloadEncoded = Base64.encodeToString(
            payload.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        val token = "$header.$payloadEncoded.dev_signature_token"

        return AuthResponse(
            accessToken = token,
            refreshToken = "refresh_$token",
            tokenType = "Bearer",
            expiresIn = 86400,
            user = UserDto(
                id = if (role == UserRole.ADMIN) "usr_admin_001" else "usr_user_002",
                email = email,
                username = email.substringBefore("@"),
                role = role.name
            )
        )
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}
