package com.example.presentation.ai

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.ai.components.ChatBubbleItem
import com.example.presentation.ai.components.SpeechRecognizerHelper
import com.example.ui.theme.TextGray
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val context = LocalContext.current

    val speechHelper = remember { SpeechRecognizerHelper(context) }
    var isListening by remember { mutableStateOf(false) }

    DisposableEffect(speechHelper) {
        onDispose {
            speechHelper.destroy()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                isListening = true
                speechHelper.startListening(
                    onResult = { text ->
                        viewModel.setInputText(text)
                        isListening = false
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        isListening = false
                    },
                    onReadyForSpeech = {
                        isListening = true
                    }
                )
            } else {
                Toast.makeText(context, "الرجاء منح صلاحية الميكروفون لاستخدام الإدخال الصوتي", Toast.LENGTH_LONG).show()
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.destroy()
        }
    }

    val groupedMessages = remember(uiState.messages) {
        groupMessagesByDate(uiState.messages)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.width(310.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                
                // Header with AI branding
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "سجل المحادثات",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "إجمالي الجلسات: ${uiState.sessions.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(modifier = Modifier.height(14.dp))

                // New Chat Button (Card Style)
                Surface(
                    onClick = {
                        viewModel.createNewSession()
                        scope.launch { drawerState.close() }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "محادثة جديدة",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Session List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    if (uiState.sessions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "لا توجد محادثات سابقة بعد.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextGray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(uiState.sessions) { title ->
                            val isSelected = uiState.currentSessionTitle == title
                            val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            val borderStroke = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else BorderStroke(1.dp, Color.Transparent)
                            val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

                            Surface(
                                onClick = {
                                    viewModel.selectSession(title)
                                    scope.launch { drawerState.close() }
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = bgColor,
                                border = borderStroke,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.ChatBubble else Icons.Default.ChatBubbleOutline,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else TextGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = textColor,
                                            maxLines = 1
                                        )
                                    }
                                    
                                    IconButton(
                                        onClick = { viewModel.deleteSession(title) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف المحادثة",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مستشارك المالي الذكي",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "الجلسات")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Model Selector Pill Row
                var isModelDropdownExpanded by remember { mutableStateOf(false) }
                val selectedModel = uiState.models.find { it.id == uiState.selectedModelId } ?: uiState.models.first()
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "نموذج الذكاء الاصطناعي الحالي:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                    
                    Box {
                        Surface(
                            onClick = { isModelDropdownExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = selectedModel.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = if (isModelDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        var isGoogleExpanded by remember { mutableStateOf(true) }
                        var isZaiExpanded by remember { mutableStateOf(true) }
                        
                        DropdownMenu(
                            expanded = isModelDropdownExpanded,
                            onDismissRequest = { isModelDropdownExpanded = false },
                            modifier = Modifier.width(280.dp).background(MaterialTheme.colorScheme.surface)
                        ) {
                            // Google Header
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("جوجل (Google)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                                        Icon(
                                            imageVector = if (isGoogleExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                onClick = { isGoogleExpanded = !isGoogleExpanded }
                            )
                            
                            if (isGoogleExpanded) {
                                uiState.models.filter { it.provider == "Google" }.forEach { model ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = model.name, fontSize = 13.sp)
                                        },
                                        onClick = {
                                            viewModel.selectModel(model.id)
                                            isModelDropdownExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = if (model.id == uiState.selectedModelId) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                            }
                            
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            
                            // Z.ai Header
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Z.ai (Agent Router)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                                        Icon(
                                            imageVector = if (isZaiExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                onClick = { isZaiExpanded = !isZaiExpanded }
                            )
                            
                            if (isZaiExpanded) {
                                uiState.models.filter { it.provider == "Z.ai" }.forEach { model ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = model.name, fontSize = 13.sp)
                                        },
                                        onClick = {
                                            viewModel.selectModel(model.id)
                                            isModelDropdownExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = if (model.id == uiState.selectedModelId) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Messages List
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    if (uiState.messages.isEmpty()) {
                        // Empty Chat View
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6))
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "كيف يمكنني مساعدتك اليوم؟",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "يمكنك إدخال عملياتك المالية باللغة العربية البسيطة وسأقوم بتسجيلها وتصنيفها فوراً.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        val listState = rememberLazyListState()
                        LaunchedEffect(uiState.messages.size) {
                            listState.animateScrollToItem(uiState.messages.size)
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            groupedMessages.forEach { (dateHeader, messagesForDate) ->
                                item {
                                    DateSeparator(text = dateHeader)
                                }
                                items(messagesForDate) { message ->
                                    ChatBubbleItem(
                                        message = message,
                                        onConfirmDraft = { viewModel.confirmDraft(message.id) },
                                        onCancelDraft = { viewModel.cancelDraft(message.id) }
                                    )
                                }
                            }

                            if (uiState.isLoading) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "جاري كتابة الرد...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Suggestions Horizontal List
                if (uiState.suggestions.isNotEmpty() && !uiState.isLoading) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.suggestions) { suggestion ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                modifier = Modifier.clickable { viewModel.selectSuggestion(suggestion) }
                            ) {
                                Text(
                                    text = suggestion,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Message Input Row
                Surface(
                    tonalElevation = 2.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.inputText,
                            onValueChange = { viewModel.setInputText(it) },
                            placeholder = { Text("تحدث مع المساعد المالي...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 3,
                            leadingIcon = {
                                val micColor = if (isListening) Color.Red else MaterialTheme.colorScheme.primary
                                IconButton(
                                    onClick = {
                                        if (isListening) {
                                            speechHelper.stopListening()
                                            isListening = false
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = "تحدث",
                                        tint = micColor
                                    )
                                }
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { viewModel.sendMessage() },
                                    enabled = uiState.inputText.isNotBlank()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "إرسال",
                                        tint = if (uiState.inputText.isNotBlank()) MaterialTheme.colorScheme.primary else TextGray.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateSeparator(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextGray,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

private fun groupMessagesByDate(messages: List<AiChatMessage>): Map<String, List<AiChatMessage>> {
    val cal = Calendar.getInstance()
    
    // Reset to midnight today
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis

    // Reset to midnight yesterday
    val yesterdayStart = todayStart - 86400000L

    return messages.groupBy { msg ->
        when {
            msg.timestamp >= todayStart -> "اليوم"
            msg.timestamp >= yesterdayStart -> "أمس"
            else -> {
                val formatter = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))
                formatter.format(Date(msg.timestamp))
            }
        }
    }
}
