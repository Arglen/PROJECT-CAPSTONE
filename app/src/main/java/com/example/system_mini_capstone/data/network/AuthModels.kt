package com.example.system_mini_capstone.data.network

import com.google.gson.annotations.SerializedName

/**
 * Request payload for OAuth2 / JWT authentication.
 */
data class LoginRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("grant_type")
    val grantType: String = "password"
)

/**
 * Authentication response containing OAuth2/JWT tokens and user details.
 */
data class AuthResponse(
    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("refresh_token")
    val refreshToken: String? = null,

    @SerializedName("token_type")
    val tokenType: String = "Bearer",

    @SerializedName("expires_in")
    val expiresIn: Long? = 86400,

    @SerializedName("user")
    val user: UserDto? = null
)

/**
 * Authenticated user profile and role details.
 */
data class UserDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("role")
    val role: String = UserRole.USER.name
)

enum class UserRole {
    USER,
    ADMIN;

    companion object {
        fun fromString(value: String?): UserRole {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: USER
        }
    }
}

/**
 * Request payload to initiate password recovery.
 */
data class ForgotPasswordRequest(
    @SerializedName("email")
    val email: String
)

/**
 * Backend response confirming password reset dispatch.
 */
data class ForgotPasswordResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("reset_token")
    val resetToken: String? = null
)

/**
 * Request payload to verify token and commit a new password.
 */
data class ResetPasswordRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("token")
    val token: String,

    @SerializedName("new_password")
    val newPassword: String
)

/**
 * Backend response confirming successful password update.
 */
data class ResetPasswordResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("success")
    val success: Boolean = true
)
