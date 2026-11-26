package com.example.system_mini_capstone.data.network

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.system_mini_capstone.BuildConfig
import com.example.system_mini_capstone.R
import com.example.system_mini_capstone.data.SessionManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.net.ConnectException
import java.net.UnknownHostException

/**
 * Helper class for Google Sign-In using Credential Manager API and Firebase Auth.
 */
object GoogleAuthHelper {

    private const val TAG = "SignLinkGoogleAuth"

    suspend fun signInWithGoogle(context: Context, sessionManager: SessionManager): Result<String> {
        return try {
            val serverClientId = context.getString(R.string.default_web_client_id)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(serverClientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialManager = CredentialManager.create(context)
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val googleIdToken = googleIdTokenCredential.idToken
            val googleEmail = googleIdTokenCredential.id
            val googleName = googleIdTokenCredential.displayName ?: googleEmail.substringBefore("@")

            val firebaseAuth = FirebaseAuth.getInstance()
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            val firebaseUser = authResult.user ?: throw Exception("Firebase Google auth failed.")

            // Read or create Firestore user doc with 5s timeout
            val firestore = FirebaseFirestore.getInstance()
            var role = "USER"
            try {
                val doc = withTimeoutOrNull(5000) {
                    firestore.collection("users").document(firebaseUser.uid).get().await()
                }
                if (doc != null && doc.exists()) {
                    val fetchedRole = doc.getString("role")
                    if (fetchedRole.equals("ADMIN", ignoreCase = true)) {
                        role = "ADMIN"
                    }
                } else {
                    val userDoc = mapOf(
                        "uid" to firebaseUser.uid,
                        "displayName" to (firebaseUser.displayName ?: googleName),
                        "email" to (firebaseUser.email ?: googleEmail),
                        "role" to "USER",
                        "createdAt" to System.currentTimeMillis()
                    )
                    withTimeoutOrNull(5000) {
                        firestore.collection("users").document(firebaseUser.uid).set(userDoc).await()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore role fetch timed out or failed: ${e.message}", e)
                role = "USER"
            }

            sessionManager.saveUserSession(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: googleEmail,
                displayName = firebaseUser.displayName ?: googleName,
                role = role
            )

            Result.success(firebaseUser.email ?: googleEmail)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(e)
        } catch (e: NoCredentialException) {
            Log.e(TAG, "No Google credentials found on device", e)
            Result.failure(Exception("No Google account found on this device. Add one in Settings and try again."))
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Network error during Google sign-in", e)
            Result.failure(Exception("No internet connection. Check your network and try again."))
        } catch (e: ConnectException) {
            Log.e(TAG, "Network connection error during Google sign-in", e)
            Result.failure(Exception("No internet connection. Check your network and try again."))
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed", e)
            val friendly = "Google sign-in failed. Please try again."
            val finalMsg = if (BuildConfig.DEBUG) "$friendly (${e.javaClass.simpleName}: ${e.message})" else friendly
            Result.failure(Exception(finalMsg))
        }
    }

    fun signOut(context: Context, sessionManager: SessionManager) {
        try {
            FirebaseAuth.getInstance().signOut()
            sessionManager.clearSession()
            runBlocking {
                try {
                    CredentialManager.create(context).clearCredentialState(
                        ClearCredentialStateRequest()
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout", e)
        }
    }
}
