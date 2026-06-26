package com.example.presentation.splash

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation states
    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(0f) }
    var loadingStep by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf(0f) }

    // Logo entrance bouncy animation
    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        // Pulsing loop
        while (true) {
            scale.animateTo(
                targetValue = 1.05f,
                animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
            )
            scale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
            )
        }
    }

    // Alpha fade in for text components
    LaunchedEffect(key1 = true) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    // Step-by-step progress simulation
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

    // Custom themed background brush
    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF090D1A), // Deep Slate/Blue
                Color(0xFF030712), // Very dark Slate
                Color(0xFF1E1B4B)  // Ambient Dark Indigo
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF1F5F9), // Light Slate 100
                Color(0xFFFAF5FF), // Light Purple 50
                Color(0xFFE2E8F0)  // Light Slate 200
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Ambient glow effect behind logo card
            Box(
                contentAlignment = Alignment.Center
            ) {
                if (isDark) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(scale.value * 1.1f)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.15f))
                    )
                }
                
                // App Logo Wrapper Card
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .scale(scale.value)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF111827).copy(alpha = 0.9f) else Color.White)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "Qdash Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // App Name "قداشّ"
            Text(
                text = "قداشّ",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 38.sp,
                    letterSpacing = 1.sp
                ),
                color = if (isDark) Color.White else Color(0xFF1E1B4B),
                modifier = Modifier.alpha(alpha.value)
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Slogan
            Text(
                text = "إدارة مالية ذكية وبسيطة",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Premium Custom Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(6.dp)
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
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF3B82F6), // Accent Blue
                                    Color(0xFF8B5CF6), // Accent Purple
                                    Color(0xFFD946EF)  // Accent Pink
                                )
                            )
                        )
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
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (isDark) Color(0xFF64748B) else Color(0xFF64748B),
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
                color = if (isDark) Color(0xFF8B5CF6) else Color(0xFF3B82F6),
                textAlign = TextAlign.Center
            )
        }
    }
}
