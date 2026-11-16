package com.example.system_mini_capstone.data.network

import android.util.Log
import com.example.system_mini_capstone.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<AuthResponse>
    suspend fun requestPasswordReset(email: String): Result<ForgotPasswordResponse>
    suspend fun resetPassword(email: String, token: String, newPassword: String): Result<ResetPasswordResponse>
}

class AuthRepositoryImpl(
    private val apiService: AuthApiService = NetworkModule.authApiService
) : AuthRepository {

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override suspend fun login(username: String, password: String): Result<AuthResponse> =
        withContext(Dispatchers.IO) {
            val email = username.trim()

            // 1. Try Firebase Authentication first
            try {
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    val token = try { firebaseUser.getIdToken(false).await()?.token ?: "fb_token_${firebaseUser.uid}" } catch (_: Exception) { "fb_token_${firebaseUser.uid}" }

                    // Fetch user role securely from Firestore
                    var role = "USER"
                    try {
                        val doc = firestore.collection("users").document(firebaseUser.uid).get().await()
                        if (doc.exists()) {
                            val fetchedRole = doc.getString("role")
                            if (fetchedRole.equals("ADMIN", ignoreCase = true)) {
                                role = "ADMIN"
                            }
                        } else {
                            // Document missing: create default USER document
                            val newDoc = mapOf(
                                "uid" to firebaseUser.uid,
                                "displayName" to (firebaseUser.displayName ?: email.substringBefore("@")),
                                "email" to email,
                                "role" to "USER",
                                "createdAt" to System.currentTimeMillis()
                            )
                            firestore.collection("users").document(firebaseUser.uid).set(newDoc)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Firestore role read failed (${e.message}), defaulting to USER", e)
                        role = "USER"
                    }

                    val userDto = UserDto(
                        id = firebaseUser.uid,
                        email = email,
                        username = firebaseUser.displayName ?: email.substringBefore("@"),
                        role = role
                    )
                    val response = AuthResponse(
                        accessToken = token,
                        refreshToken = null,
                        user = userDto
                    )
                    return@withContext Result.success(response)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firebase Auth failed (${e.message}). Checking offline test credentials / API endpoint.")
                if (e is com.google.firebase.auth.FirebaseAuthException) {
                    val mappedErr = AuthErrorMapper.mapError(e)
                    if (!BuildConfig.DEBUG) {
                        return@withContext Result.failure(Exception(mappedErr))
                    }
                }
            }

            // 2. Try REST API endpoint if configured
            try {
                val request = LoginRequest(username = email, password = password)
                val response = apiService.login(request)

                if (response.isSuccessful && response.body() != null) {
                    return@withContext Result.success(response.body()!!)
                }
            } catch (e: Exception) {
                Log.w(TAG, "REST API login failed (${e.message}).")
            }

            // 3. Fallback to offline test credentials (ONLY in DEBUG mode)
            handleOfflineFallback(email, password)
        }

    override suspend fun requestPasswordReset(email: String): Result<ForgotPasswordResponse> =
        withContext(Dispatchers.IO) {
            try {
                firebaseAuth.sendPasswordResetEmail(email.trim()).await()
                Result.success(
                    ForgotPasswordResponse(
                        message = "A password reset email has been sent to ${email.trim()}.",
                        resetToken = "FIREBASE-SENT"
                    )
                )
            } catch (e: Exception) {
                val friendlyMessage = AuthErrorMapper.mapError(e)
                Result.failure(Exception(friendlyMessage))
            }
        }

    override suspend fun resetPassword(
        email: String,
        token: String,
        newPassword: String
    ): Result<ResetPasswordResponse> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(
                    ResetPasswordResponse(
                        message = "Please check your inbox and follow the link sent to your email to complete password reset.",
                        success = true
                    )
                )
            } catch (e: Exception) {
                val friendlyMessage = AuthErrorMapper.mapError(e)
                Result.failure(Exception(friendlyMessage))
            }
        }

    private fun handleOfflineFallback(username: String, password: String): Result<AuthResponse> {
        val trimmedUser = username.trim()

        if (!BuildConfig.DEBUG) {
            return Result.failure(Exception("Incorrect email or password."))
        }

        return when {
            trimmedUser.equals("admin@signlink.app", ignoreCase = true) && password == "admin123" -> {
                Log.i(TAG, "Authenticated successfully as ADMIN via dev fallback")
                Result.success(createDevAuthResponse(trimmedUser, UserRole.ADMIN))
            }
            trimmedUser.equals("user@signlink.app", ignoreCase = true) && password == "user123" -> {
                Log.i(TAG, "Authenticated successfully as USER via dev fallback")
                Result.success(createDevAuthResponse(trimmedUser, UserRole.USER))
            }
            else -> {
                Result.failure(Exception("Incorrect email or password."))
            }
        }
    }

    private fun createDevAuthResponse(email: String, role: UserRole): AuthResponse {
        val expSeconds = (System.currentTimeMillis() / 1000) + 86400
        val header = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        val payload = """{"sub":"$email","role":"${role.name}","exp":$expSeconds}"""
        val devJwt = "$header.${android.util.Base64.encodeToString(payload.toByteArray(java.nio.charset.StandardCharsets.UTF_8), android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING)}.dev_signature"

        return AuthResponse(
            accessToken = devJwt,
            user = UserDto(
                id = if (role == UserRole.ADMIN) "usr_admin_001" else "usr_student_002",
                email = email,
                username = email.substringBefore("@"),
                role = role.name
            )
        )
    }

    companion object {
        private const val TAG = "AuthRepositoryImpl"
    }
}
