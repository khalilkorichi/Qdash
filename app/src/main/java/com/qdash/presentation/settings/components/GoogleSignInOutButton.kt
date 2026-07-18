package com.qdash.presentation.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qdash.R
import com.qdash.ui.designsystem.components.AppCard
import com.qdash.ui.designsystem.components.CardVariant
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.theme.TextGray

@Composable
fun GoogleSignInOutButton(
    isLinked: Boolean,
    isLoading: Boolean,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val text = if (isLinked) {
        stringResource(id = R.string.google_sign_out)
    } else {
        stringResource(id = R.string.google_sign_in)
    }

    val icon = if (isLinked) {
        Icons.Default.Logout
    } else {
        Icons.Default.AccountCircle
    }

    val iconColor = if (isLinked) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    val textColor = if (isLinked) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    AppCard(
        modifier = modifier.fillMaxWidth(),
        variant = CardVariant.INTERACTIVE,
        shape = ShapeTokens.Lg,
        onClick = {
            if (!isLoading) {
                if (isLinked) onSignOutClick() else onSignInClick()
            }
        },
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = textColor
                )
            }
        }
    }
}
