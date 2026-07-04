package com.qdash.presentation.splash

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Progress and loading states
    var loadingStep by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf(0f) }

    // Step-by-step progress simulation (2.4 seconds total)
    LaunchedEffect(key1 = true) {
        // Step 0: Initialize
        loadingStep = 0
        progress = 0.15f
        delay(450)
        
        // Step 1: Connect to Local Database
        loadingStep = 1
        progress = 0.35f
        delay(500)
        
        // Step 2: Validate Schema and Cache
        loadingStep = 2
        progress = 0.60f
        delay(450)
        
        // Step 3: Populate and Map Default Categories
        loadingStep = 3
        progress = 0.80f
        delay(500)
        
        // Step 4: Loading accounts & assets
        loadingStep = 4
        progress = 0.95f
        delay(400)
        
        // Step 5: Completed
        loadingStep = 5
        progress = 1.00f
        delay(400)
        
        onSplashFinished()
    }

    // Text status messages based on current loading step
    val statusText = when (loadingStep) {
        0 -> "تهيئة النظام وتجهيز الذاكرة المخبئية..."
        1 -> "جاري الاتصال بقاعدة البيانات المحلية..."
        2 -> "التحقق من سلامة الجداول وبنية البيانات..."
        3 -> "تحديث فئات الدخل والمصاريف الذكية..."
        4 -> "تصفية الحسابات وتهيئة البيئة المالية..."
        5 -> "مرحباً بك في قداشّ!"
        else -> ""
    }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f // Simple dark mode check

    // Background color based on Light / Dark theme
    val backgroundColor = if (isDark) {
        Color(0xFF09090B) // BackgroundDark
    } else {
        Color(0xFFFBFBFA) // BackgroundLight
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // App Logo - white in dark mode, normal in light mode
            Box(
                modifier = Modifier.size(112.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "Qdash Logo",
                    colorFilter = ColorFilter.tint(if (isDark) Color.White else Color.Black),
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // App Name "قداشّ"
            Text(
                text = "قداشّ",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 36.sp,
                    letterSpacing = 1.sp
                ),
                color = if (isDark) Color.White else Color(0xFF191919)
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Slogan
            Text(
                text = "إدارة مالية ذكية وبسيطة",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(56.dp))

            // Custom Progress Bar - Solid Brand Primary Color matching Light/Dark themes
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
            ) {
                // Smoothly animated progress indicator
                val animatedProgressWidth by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgressWidth)
                        .fillMaxHeight()
                        .background(if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Loading steps label
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Crossfade(
                    targetState = statusText,
                    animationSpec = tween(durationMillis = 200)
                ) { targetText ->
                    Text(
                        text = targetText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = if (isDark) Color(0xFF64748B) else Color(0xFF475569),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Percentage indicator
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                textAlign = TextAlign.Center
            )
        }
    }
}
// Helper to support Center Arrangement without extra imports
private val Center = Arrangement.Center
