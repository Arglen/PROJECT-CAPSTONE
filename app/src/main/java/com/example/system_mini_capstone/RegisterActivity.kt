package com.example.system_mini_capstone

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system_mini_capstone.ui.RegisterEvent
import com.example.system_mini_capstone.ui.RegisterUiState
import com.example.system_mini_capstone.ui.RegisterViewModel
import com.example.system_mini_capstone.ui.components.SignLinkLogo
import com.example.system_mini_capstone.ui.components.SignLinkPrimaryButton
import com.example.system_mini_capstone.ui.components.SignLinkTextField
import com.example.system_mini_capstone.ui.components.SignLinkTintedBanner
import com.example.system_mini_capstone.ui.theme.SignLinkColors
import com.example.system_mini_capstone.ui.theme.System_mini_capstoneTheme

class RegisterActivity : SignLinkBaseActivity() {

    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            System_mini_capstoneTheme {
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.eventFlow.collect { event ->
                        when (event) {
                            is RegisterEvent.Success -> {
                                navigateToDashboard()
                            }
                            is RegisterEvent.Error -> {
                                // Handled by red error banner in UI
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    RegisterScreenContent(
                        uiState = uiState,
                        onDisplayNameChanged = viewModel::onDisplayNameChanged,
                        onEmailChanged = viewModel::onEmailChanged,
                        onPasswordChanged = viewModel::onPasswordChanged,
                        onConfirmPasswordChanged = viewModel::onConfirmPasswordChanged,
                        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                        onRegisterClick = viewModel::register,
                        onDismissError = viewModel::dismissError,
                        onBackToLogin = { finish() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}

@Composable
fun RegisterScreenContent(
    uiState: RegisterUiState,
    onDisplayNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onRegisterClick: () -> Unit,
    onDismissError: () -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = { keyboardController?.hide() })
            }
            .verticalScroll(scrollState)
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        // Top: SignLink Logo
        SignLinkLogo()

        Spacer(modifier = Modifier.height(24.dp))

        // Illustration Circle
        Box(
            modifier = Modifier
                .size(130.dp)
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .background(SignLinkColors.purpleTint),
            contentAlignment = Alignment.Center
        ) {
            // TODO: Use illustration asset when available
            Text(text = "🚀", fontSize = 42.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title & Subtitle
        Text(
            text = "Start your journey",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = SignLinkColors.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Create an account and learn at your own pace.",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 15.sp,
            color = SignLinkColors.secondaryText
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Error message banner
        AnimatedVisibility(
            visible = uiState.errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            uiState.errorMessage?.let { errorText ->
                SignLinkTintedBanner(
                    text = errorText,
                    containerColor = SignLinkColors.errorTint,
                    contentColor = SignLinkColors.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }

        // Full Name
        SignLinkTextField(
            value = uiState.displayName,
            onValueChange = onDisplayNameChanged,
            label = "Full name",
            placeholder = "Your full name",
            leadingIcon = Icons.Default.Person,
            isError = uiState.displayNameError != null,
            errorMessage = uiState.displayNameError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email Address
        SignLinkTextField(
            value = uiState.email,
            onValueChange = onEmailChanged,
            label = "Email address",
            placeholder = "you@example.com",
            leadingIcon = Icons.Default.Email,
            isError = uiState.emailError != null,
            errorMessage = uiState.emailError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password
        SignLinkTextField(
            value = uiState.password,
            onValueChange = onPasswordChanged,
            label = "Password",
            placeholder = "Create a password",
            leadingIcon = Icons.Default.Lock,
            isError = uiState.passwordError != null,
            errorMessage = uiState.passwordError,
            visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Text(
                        text = if (uiState.isPasswordVisible) "👁" else "🔒",
                        fontSize = 18.sp
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            )
        )

        Text(
            text = "At least 6 characters",
            style = MaterialTheme.typography.bodySmall,
            color = SignLinkColors.secondaryText,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Confirm Password
        SignLinkTextField(
            value = uiState.confirmPassword,
            onValueChange = onConfirmPasswordChanged,
            label = "Confirm password",
            placeholder = "Re-enter password",
            leadingIcon = Icons.Default.Lock,
            isError = uiState.confirmPasswordError != null,
            errorMessage = uiState.confirmPasswordError,
            visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    onRegisterClick()
                }
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Primary Create Account Button
        SignLinkPrimaryButton(
            text = "Create account",
            onClick = {
                keyboardController?.hide()
                onRegisterClick()
            },
            isLoading = uiState.isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Already have an account? Log in Link
        TextButton(
            onClick = onBackToLogin,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Already have an account? Log in",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = SignLinkColors.primary
            )
        }
    }
}
