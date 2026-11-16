package com.example.system_mini_capstone.data.network

import android.util.Log
import com.example.system_mini_capstone.BuildConfig
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import java.net.ConnectException
import java.net.UnknownHostException

/**
 * Maps raw authentication exceptions to friendly, secure user-facing error messages
 * with optional DEBUG technical hints.
 */
object AuthErrorMapper {

    private const val TAG = "SignLinkAuth"

    fun mapError(throwable: Throwable?): String {
        if (throwable == null) return "Something went wrong. Please try again."

        val message = throwable.message ?: ""
        val errorCode = (throwable as? FirebaseAuthException)?.errorCode ?: ""

        try {
            Log.e(TAG, "Auth exception caught: ${throwable.javaClass.simpleName} - $message (errorCode: $errorCode)", throwable)
        } catch (_: Throwable) {
            // Unit test environment without Android Log mock
        }

        return when {
            throwable is FirebaseNetworkException || throwable is UnknownHostException || throwable is ConnectException || message.contains("network", ignoreCase = true) || message.contains("UNREACHABLE", ignoreCase = true) ->
                "No internet connection. Check your network and try again."

            message.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
            message.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
            errorCode.equals("ERROR_OPERATION_NOT_ALLOWED", ignoreCase = true) ||
            errorCode.equals("operation-not-allowed", ignoreCase = true) ||
            errorCode.equals("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                "Sign-up is not available right now. Please try again later."

            message.contains("API key not valid", ignoreCase = true) ||
            message.contains("API_KEY", ignoreCase = true) ||
            errorCode.equals("ERROR_API_KEY_INVALID", ignoreCase = true) ->
                "App configuration error. Please contact support."

            throwable is FirebaseAuthWeakPasswordException || errorCode.equals("ERROR_WEAK_PASSWORD", ignoreCase = true) || errorCode.equals("weak-password", ignoreCase = true) || message.contains("weak-password", ignoreCase = true) ->
                "Choose a password with at least 6 characters."

            throwable is FirebaseAuthUserCollisionException || errorCode.equals("ERROR_EMAIL_ALREADY_IN_USE", ignoreCase = true) || errorCode.equals("email-already-in-use", ignoreCase = true) || message.contains("email-already-in-use", ignoreCase = true) ->
                "An account with this email already exists."

            errorCode.equals("ERROR_INVALID_EMAIL", ignoreCase = true) || errorCode.equals("invalid-email", ignoreCase = true) || message.contains("invalid-email", ignoreCase = true) ->
                "Enter a valid email address."

            message.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
            message.contains("WRONG_PASSWORD", ignoreCase = true) ||
            message.contains("USER_NOT_FOUND", ignoreCase = true) ||
            message.contains("invalid-credential", ignoreCase = true) ||
            message.contains("user-not-found", ignoreCase = true) ||
            message.contains("wrong-password", ignoreCase = true) ||
            errorCode.equals("ERROR_INVALID_CREDENTIAL", ignoreCase = true) ->
                "Incorrect email or password."

            message.contains("TOO_MANY_ATTEMPTS_TRY_LATER", ignoreCase = true) || message.contains("too-many-requests", ignoreCase = true) || errorCode.equals("ERROR_TOO_MANY_REQUESTS", ignoreCase = true) ->
                "Too many attempts. Try again later."

            else -> "Something went wrong. Please try again."
        }
    }

    fun mapErrorWithDebug(throwable: Throwable?): String {
        val base = mapError(throwable)
        if (BuildConfig.DEBUG && throwable != null) {
            val exName = throwable.javaClass.simpleName
            val exMsg = throwable.message ?: "Unknown error"
            return "$base ($exName: $exMsg)"
        }
        return base
    }
}
