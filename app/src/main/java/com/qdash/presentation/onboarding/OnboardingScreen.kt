package com.qdash.presentation.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.domain.model.AccountType
import com.qdash.presentation.onboarding.components.*
import com.qdash.ui.theme.Primary
import com.qdash.ui.theme.TextGray
import com.qdash.ui.designsystem.components.shimmerEffect

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Set up Notification Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.saveNotificationPermission(isGranted)
        viewModel.nextStep()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar Progress & App Name
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قداشّ",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        ),
                        color = Primary
                    )

                    // Step Indicator label in Arabic
                    Text(
                        text = "الخطوة ${uiState.currentStep} من 4",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextGray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(4) { index ->
                        val isCompletedOrCurrent = index + 1 <= uiState.currentStep
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(if (isCompletedOrCurrent) Primary else Color.Transparent)
                        )
                    }
                }
            }

            // Main steps content with animated visibility
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                when (uiState.currentStep) {
                    1 -> {
                        LanguageSetupScreen(
                            selectedLanguage = uiState.selectedLanguage,
                            onLanguageSelected = { viewModel.setLanguage(it) },
                            onNext = { viewModel.nextStep() }
                        )
                    }
                    2 -> {
                        NotificationPermissionScreen(
                            onEnable = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.saveNotificationPermission(true)
                                    viewModel.nextStep()
                                }
                            },
                            onLater = {
                                viewModel.saveNotificationPermission(false)
                                viewModel.nextStep()
                            }
                        )
                    }
                    3 -> {
                        InitialWalletSetupScreen(
                            uiState = uiState,
                            onBalanceChanged = { type, value -> viewModel.onBalanceChanged(type, value) },
                            onNext = {
                                keyboardController?.hide()
                                viewModel.completeWalletSetup(skip = false, onFinished = onFinished)
                            },
                            onSkip = {
                                keyboardController?.hide()
                                viewModel.completeWalletSetup(skip = true, onFinished = onFinished)
                            }
                        )
                    }
                    4 -> {
                        AuthSetupScreen(
                            onSignInSuccess = { account ->
                                viewModel.linkGoogleAccount(account, context, onFinished)
                            },
                            onSkip = {
                                viewModel.skipGoogleSignIn(onFinished)
                            }
                        )
                    }
                }
            }
        }

        if (uiState.isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .width(280.dp)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 200.dp, height = 6.dp)
                                .shimmerEffect(RoundedCornerShape(3.dp))
                        )
                        Text(
                            text = uiState.savingMessage,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (uiState.currentStep == 4) 
                                "الرجاء عدم إغلاق التطبيق أثناء مزامنة بيانات حسابك."
                            else 
                                "نعمل على إعداد الحسابات والفئات الذكية لتتبع ميزانيتك بأفضل طريقة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
