package com.example.system_mini_capstone

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system_mini_capstone.ui.ForgotPasswordEvent
import com.example.system_mini_capstone.ui.ForgotPasswordUiState
import com.example.system_mini_capstone.ui.ForgotPasswordViewModel
import com.example.system_mini_capstone.ui.RecoveryStep
import com.example.system_mini_capstone.ui.theme.System_mini_capstoneTheme

/**
 * Activity managing the multi-step Credential Recovery (Forgot Password) flow.
 */
class ForgotPasswordActivity : ComponentActivity() {

    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            System_mini_capstoneTheme {
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.eventFlow.collect { event ->
                        when (event) {
                            is ForgotPasswordEvent.CodeSent -> {
                                Toast.makeText(this@ForgotPasswordActivity, event.message, Toast.LENGTH_SHORT).show()
                            }
                            is ForgotPasswordEvent.PasswordResetSuccess -> {
                                Toast.makeText(
                                    this@ForgotPasswordActivity,
                                    "Password successfully updated!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                ForgotPasswordScreen(
                    uiState = uiState,
                    onEmailChanged = viewModel::onEmailChanged,
                    onTokenChanged = viewModel::onTokenChanged,
                    onNewPasswordChanged = viewModel::onNewPasswordChanged,
                    onConfirmPasswordChanged = viewModel::onConfirmPasswordChanged,
                    onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                    onRequestCode = viewModel::sendRecoveryRequest,
                    onSubmitNewPassword = viewModel::submitNewPassword,
                    onDismissError = viewModel::dismissError,
                    onBackToLogin = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    uiState: ForgotPasswordUiState,
    onEmailChanged: (String) -> Unit,
    onTokenChanged: (String) -> Unit,
    onNewPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onRequestCode: () -> Unit,
    onSubmitNewPassword: () -> Unit,
    onDismissError: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Credential Recovery") },
                navigationIcon = {
                    IconButton(onClick = onBackToLogin) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { keyboardController?.hide() })
                }
                .verticalScroll(scrollState)
                .imePadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Error alert banner
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.errorMessage?.let { errorText ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = errorText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = onDismissError) {
                                Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = uiState.step,
                transitionSpec = {
                    slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
                },
                label = "RecoverySteps"
            ) { step ->
                when (step) {
                    RecoveryStep.REQUEST_CODE -> {
                        RequestCodeStepContent(
                            email = uiState.email,
                            emailError = uiState.emailError,
                            isLoading = uiState.isLoading,
                            onEmailChanged = onEmailChanged,
                            onRequestCode = onRequestCode,
                            onBackToLogin = onBackToLogin
                        )
                    }
                    RecoveryStep.RESET_PASSWORD -> {
                        ResetPasswordStepContent(
                            email = uiState.email,
                            token = uiState.resetToken,
                            newPassword = uiState.newPassword,
                            confirmPassword = uiState.confirmPassword,
                            isPasswordVisible = uiState.isPasswordVisible,
                            tokenError = uiState.tokenError,
                            passwordError = uiState.passwordError,
                            confirmPasswordError = uiState.confirmPasswordError,
                            infoMessage = uiState.infoMessage,
                            isLoading = uiState.isLoading,
                            onTokenChanged = onTokenChanged,
                            onNewPasswordChanged = onNewPasswordChanged,
                            onConfirmPasswordChanged = onConfirmPasswordChanged,
                            onTogglePasswordVisibility = onTogglePasswordVisibility,
                            onSubmitNewPassword = onSubmitNewPassword
                        )
                    }
                    RecoveryStep.COMPLETED -> {
                        CompletedStepContent(
                            onBackToLogin = onBackToLogin
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestCodeStepContent(
    email: String,
    emailError: String?,
    isLoading: Boolean,
    onEmailChanged: (String) -> Unit,
    onRequestCode: () -> Unit,
    onBackToLogin: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🔑", fontSize = 32.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Reset Your Password",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your registered email address below. We'll send you a secure verification code to reset your credentials.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChanged,
            label = { Text("Registered Email") },
            placeholder = { Text("user@signlink.app") },
            singleLine = true,
            isError = emailError != null,
            supportingText = {
                emailError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestCode,
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Send Reset Code")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBackToLogin) {
            Text("Remember your password? Sign In")
        }
    }
}

@Composable
private fun ResetPasswordStepContent(
    email: String,
    token: String,
    newPassword: String,
    confirmPassword: String,
    isPasswordVisible: Boolean,
    tokenError: String?,
    passwordError: String?,
    confirmPasswordError: String?,
    infoMessage: String?,
    isLoading: Boolean,
    onTokenChanged: (String) -> Unit,
    onNewPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmitNewPassword: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("✉️", fontSize = 32.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Enter Verification Code",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = infoMessage ?: "Verification code sent to $email",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Reset Code / Token input
        OutlinedTextField(
            value = token,
            onValueChange = onTokenChanged,
            label = { Text("Reset Code / Token") },
            placeholder = { Text("e.g. SIGN-7294") },
            singleLine = true,
            isError = tokenError != null,
            supportingText = {
                tokenError?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // New Password
        OutlinedTextField(
            value = newPassword,
            onValueChange = onNewPasswordChanged,
            label = { Text("New Password") },
            singleLine = true,
            isError = passwordError != null,
            supportingText = {
                passwordError?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Text(text = if (isPasswordVisible) "👁" else "🔒", fontSize = 18.sp)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Confirm New Password
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChanged,
            label = { Text("Confirm New Password") },
            singleLine = true,
            isError = confirmPasswordError != null,
            supportingText = {
                confirmPasswordError?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSubmitNewPassword,
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Update Password")
            }
        }
    }
}

@Composable
private fun CompletedStepContent(
    onBackToLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(90.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("✅", fontSize = 42.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Password Reset Complete!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your password has been securely updated. You can now log into your account using your new credentials.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onBackToLogin,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Back to Login",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}
