package com.example.presentation.ai

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.model.AiVoiceState
import com.example.presentation.ai.components.SpeechRecognizerHelper
import kotlinx.coroutines.launch

@Composable
fun AiVoiceInputScreen(
    viewModel: AiChatViewModel,
    onNavigateToChat: (String) -> Unit,
    onClose: () -> Unit
) {
    val voiceState by viewModel.voiceState.collectAsState()
    val voiceText by viewModel.voiceText.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val speechHelper = remember { SpeechRecognizerHelper(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.startVoiceListening()
                speechHelper.startListening(
                    onResult = { text ->
                        viewModel.updateVoicePartial(text)
                        viewModel.stopVoiceListening()
                    },
                    onError = { message -> viewModel.setVoiceError(message) },
                    onReadyForSpeech = { viewModel.startVoiceListening() },
                    onPartialResult = { partial -> viewModel.updateVoicePartial(partial) }
                )
            } else {
                viewModel.setVoiceError("يرجى منح إذن الميكروفون من الإعدادات")
            }
        }
    )

    LaunchedEffect(voiceState) {
        val error = voiceState as? AiVoiceState.Error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error.message)
    }

    DisposableEffect(speechHelper) {
        onDispose { speechHelper.destroy() }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    VoiceHeader(
                        onClose = onClose,
                        modifier = Modifier.padding(horizontal = 22.dp)
                    )
                    Spacer(modifier = Modifier.weight(0.65f))
                    AnimatedVoiceOrb(
                        state = voiceState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                    Spacer(modifier = Modifier.height(26.dp))
                    TranscribedTextDisplay(
                        text = voiceText,
                        state = voiceState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = voiceStatusLabel(voiceState),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            textDirection = TextDirection.Rtl
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    VoiceInputBottomBar(
                        state = voiceState,
                        text = voiceText,
                        onClose = onClose,
                        onStartRecording = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        onStopRecording = {
                            speechHelper.stopListening()
                            viewModel.setVoiceProcessing()
                            viewModel.stopVoiceListening()
                        },
                        onNavigateToChat = {
                            val text = voiceText.trim()
                            viewModel.preFillMessage(text)
                            onNavigateToChat(text)
                        },
                        onSendVoiceMessage = {
                            val text = voiceText.trim()
                            if (text.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("تحدث أولاً قبل الإرسال") }
                            } else {
                                viewModel.sendVoiceMessage(text)
                                onNavigateToChat("")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Right side (RTL): Robot icon + text
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(
                modifier = Modifier.padding(start = 10.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "المستشار المالي الذكي",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "جاهز لفهم معاملاتك بالعربية",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Left side (RTL): Close button
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "خروج")
        }
    }
}

@Composable
private fun VoiceInputBottomBar(
    state: AiVoiceState,
    text: String,
    onClose: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onNavigateToChat: () -> Unit,
    onSendVoiceMessage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Right side (RTL): Actions container
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Keyboard FAB (always switch to typing mode)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                onClick = onNavigateToChat,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "التبديل للكتابة",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Send FAB (only visible if text is not blank)
            if (text.isNotBlank()) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    onClick = onSendVoiceMessage,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال الأمر",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Center: Mic button
        MicrophoneButton(
            state = state,
            onStartRecording = onStartRecording,
            onStopRecording = onStopRecording
        )

        // Left side (RTL): Close FAB
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            onClick = onClose,
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "خروج",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun voiceStatusLabel(state: AiVoiceState): String = when (state) {
    AiVoiceState.Idle -> "انقر للتحدث"
    AiVoiceState.Listening -> "جار الاستماع..."
    AiVoiceState.Processing -> "جار الفهم..."
    is AiVoiceState.Transcribed -> "فهمت! يمكنك الإرسال أو الانتقال للكتابة"
    is AiVoiceState.Error -> "حدث خطأ، يمكنك إعادة المحاولة"
}
