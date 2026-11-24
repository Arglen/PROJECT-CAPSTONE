package com.example.system_mini_capstone.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system_mini_capstone.ui.theme.SignLinkColors

/**
 * Primary action button styled with SignLink design system (Primary blue #246BFD, 14dp radius, 50dp height).
 */
@Composable
fun SignLinkPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SignLinkColors.primary,
            contentColor = SignLinkColors.onPrimary
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = SignLinkColors.onPrimary,
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}

/**
 * Secondary action button styled with SignLink design system (White background, 1dp #EBEDF0 border, dark text).
 */
@Composable
fun SignLinkSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SignLinkColors.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SignLinkColors.surface,
            contentColor = SignLinkColors.onBackground
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
    }
}

/**
 * Input text field with label 12sp semibold above, 52dp tall, bg #F7F8FC, border #EBEDF0, primary border when focused.
 */
@Composable
fun SignLinkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = SignLinkColors.onBackground
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = SignLinkColors.secondaryText, fontSize = 14.sp) },
            singleLine = singleLine,
            isError = isError,
            visualTransformation = visualTransformation,
            leadingIcon = leadingIcon?.let {
                { Icon(imageVector = it, contentDescription = null, tint = SignLinkColors.secondaryText) }
            },
            trailingIcon = trailingIcon,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SignLinkColors.fieldBackground,
                unfocusedContainerColor = SignLinkColors.fieldBackground,
                disabledContainerColor = SignLinkColors.fieldBackground,
                errorContainerColor = SignLinkColors.fieldBackground,
                focusedBorderColor = SignLinkColors.primary,
                unfocusedBorderColor = SignLinkColors.fieldBorder,
                errorBorderColor = SignLinkColors.error
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = SignLinkColors.error
            )
        }
    }
}

/**
 * Content card styled with 24dp large radius, white background, soft shadow.
 */
@Composable
fun SignLinkCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp), clip = false),
        shape = RoundedCornerShape(24.dp),
        color = SignLinkColors.surface,
        tonalElevation = 1.dp,
        content = {
            Column(
                modifier = Modifier.padding(20.dp),
                content = content
            )
        }
    )
}

/**
 * Back navigation button styled as 44dp white rounded 12dp square with chevron-left icon dark.
 */
@Composable
fun SignLinkBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = SignLinkColors.surface,
        border = BorderStroke(1.dp, SignLinkColors.outline),
        shadowElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = SignLinkColors.onBackground
            )
        }
    }
}

/**
 * Tinted banner supporting custom background tint color and text styling.
 */
@Composable
fun SignLinkTintedBanner(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icon?.invoke()
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * SignLink branding logo: 32dp blue #246BFD rounded 8dp square with a white AutoAwesome sparkle icon + "SignLink" 22sp bold navy.
 */
@Composable
fun SignLinkLogo(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(8.dp),
            color = SignLinkColors.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = SignLinkColors.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Text(
            text = "SignLink",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = SignLinkColors.onBackground
        )
    }
}

data class BottomBarItem(
    val label: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector
)

/**
 * Bottom navigation bar respecting system navigation bar insets and supporting outlined/filled states.
 */
@Composable
fun SignLinkBottomBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = SignLinkColors.surface,
        border = BorderStroke(1.dp, SignLinkColors.outline),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                BottomBarItem("Home", Icons.Default.Home, Icons.Default.Home),
                BottomBarItem("Camera", Icons.Default.CameraAlt, Icons.Default.CameraAlt),
                BottomBarItem("Dictionary", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
                BottomBarItem("Saved", Icons.Default.BookmarkBorder, Icons.Default.Bookmark)
            )

            items.forEachIndexed { index, item ->
                val selected = selectedIndex == index
                val color = if (selected) SignLinkColors.primary else SignLinkColors.secondaryText
                val icon = if (selected) item.selectedIcon else item.unselectedIcon

                Column(
                    modifier = Modifier
                        .clickable(onClick = { onTabSelected(index) })
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = item.label,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = color
                    )
                }
            }
        }
    }
}
