package com.example.system_mini_capstone

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system_mini_capstone.data.SessionManager
import com.example.system_mini_capstone.ui.LoginUiState
import com.example.system_mini_capstone.ui.LoginViewModel
import com.example.system_mini_capstone.ui.components.SignLinkLogo
import com.example.system_mini_capstone.ui.components.SignLinkPrimaryButton
import com.example.system_mini_capstone.ui.components.SignLinkSecondaryButton
import com.example.system_mini_capstone.ui.components.SignLinkTextField
import com.example.system_mini_capstone.ui.components.SignLinkTintedBanner
import com.example.system_mini_capstone.ui.theme.SignLinkColors
import com.example.system_mini_capstone.ui.theme.System_mini_capstoneTheme

class LoginActivity : SignLinkBaseActivity() {

    private val viewModel: LoginViewModel by viewModels()
    private val sessionManager by lazy { SessionManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            System_mini_capstoneTheme {
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.eventFlow.collect { event ->
                        when (event) {
                            is com.example.system_mini_capstone.ui.LoginEvent.Success -> {
                                navigateToDashboard()
                            }
                            is com.example.system_mini_capstone.ui.LoginEvent.Error -> {
                                // Handled by red error banner in UI
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    LoginScreenContent(
                        uiState = uiState,
                        onUsernameChanged = viewModel::onUsernameChanged,
                        onPasswordChanged = viewModel::onPasswordChanged,
                        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                        onLoginClick = viewModel::login,
                        onGoogleLoginClick = {
                            viewModel.signInWithGoogle(this)
                        },
                        onGuestLoginClick = {
                            sessionManager.saveGuestSession()
                            navigateToDashboard()
                        },
                        onDismissError = viewModel::dismissError,
                        onSelectPreset = viewModel::applyPreset,
                        onForgotPasswordClick = {
                            startActivity(Intent(this@LoginActivity, ForgotPasswordActivity::class.java))
                        },
                        onCreateAccountClick = {
                            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
                        },
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
fun LoginScreenContent(
    uiState: LoginUiState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onLoginClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    onGuestLoginClick: () -> Unit,
    onDismissError: () -> Unit,
    onSelectPreset: (username: String, password: String) -> Unit,
    onForgotPasswordClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    val showDevPresets = BuildConfig.DEBUG

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
            Text(text = "👋", fontSize = 48.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title & Subtitle
        Text(
            text = "Welcome back",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = SignLinkColors.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Your next connection starts with a sign.",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 15.sp,
            color = SignLinkColors.secondaryText
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Demo Presets Card (DEBUG Only)
        if (showDevPresets) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SignLinkColors.purpleTint.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Quick Test Presets (Debug)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SignLinkColors.accentPurple
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSelectPreset("admin@signlink.app", "admin123") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Admin Demo", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { onSelectPreset("user@signlink.app", "user123") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("User Demo", fontSize = 11.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

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

        // Email address field
        SignLinkTextField(
            value = uiState.username,
            onValueChange = onUsernameChanged,
            label = "Email address",
            placeholder = "you@example.com",
            leadingIcon = Icons.Default.Email,
            isError = uiState.usernameError != null,
            errorMessage = uiState.usernameError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        SignLinkTextField(
            value = uiState.password,
            onValueChange = onPasswordChanged,
            label = "Password",
            placeholder = "Enter your password",
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
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    onLoginClick()
                }
            )
        )

        // Forgot Password Link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onForgotPasswordClick) {
                Text(
                    text = "Forgot password?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SignLinkColors.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Primary Log in Button
        SignLinkPrimaryButton(
            text = "Log in",
            onClick = {
                keyboardController?.hide()
                onLoginClick()
            },
            isLoading = uiState.isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Create Account Link (Centered Blue Link)
        TextButton(
            onClick = onCreateAccountClick,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "New to SignLink? Create an account",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = SignLinkColors.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Divider "or"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = SignLinkColors.outline)
            Text("or", style = MaterialTheme.typography.bodySmall, color = SignLinkColors.secondaryText)
            HorizontalDivider(modifier = Modifier.weight(1f), color = SignLinkColors.outline)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Continue with Google Button
        OutlinedButton(
            onClick = onGoogleLoginClick,
            enabled = !uiState.isLoading,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, SignLinkColors.outline),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = SignLinkColors.surface,
                contentColor = SignLinkColors.onBackground
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = SignLinkColors.primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = SignLinkColors.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Continue as guest
        SignLinkSecondaryButton(
            text = "Continue as guest",
            onClick = onGuestLoginClick
        )
    }
}
