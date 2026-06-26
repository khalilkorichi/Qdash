package com.example.presentation.ai

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.presentation.ai.components.ChatBubbleItem
import com.example.ui.designsystem.tokens.ColorTokens
import com.example.ui.designsystem.tokens.MotionTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel,
    onBack: () -> Unit,
    onVoiceInput: () -> Unit,
    onNavigateToTransactions: () -> Unit = {},
    initialMessage: String? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) ColorTokens.BackgroundDark else ColorTokens.BackgroundLight

    LaunchedEffect(initialMessage) {
        if (!initialMessage.isNullOrBlank()) {
            viewModel.preFillMessage(initialMessage)
        }
    }

    LaunchedEffect(uiState.messages.size, uiState.isAiTyping) {
        val targetIndex = uiState.messages.size + if (uiState.isAiTyping) 1 else 0
        if (targetIndex > 0) {
            listState.animateScrollToItem(targetIndex - 1)
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ChatHistoryDrawer(
                    sessions = uiState.sessions,
                    currentSession = uiState.currentSessionTitle,
                    onNewSession = {
                        viewModel.createNewSession()
                        scope.launch { drawerState.close() }
                    },
                    onSelectSession = { session ->
                        viewModel.selectSession(session)
                        scope.launch { drawerState.close() }
                    },
                    onDeleteSession = viewModel::deleteSession,
                    onClose = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = backgroundColor,
                topBar = {
                    AiChatTopBar(
                        isTyping = uiState.isAiTyping,
                        onBack = onBack,
                        onOpenHistory = { scope.launch { drawerState.open() } },
                        onClearChat = viewModel::clearChat
                    )
                },
                bottomBar = {
                    ChatInputBar(
                        text = uiState.inputText,
                        onTextChange = viewModel::setInputText,
                        onSend = viewModel::sendMessage,
                        onMicClick = onVoiceInput
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                        .padding(paddingValues)
                ) {
                    AiModelSelector(
                        selectedModelId = uiState.selectedModelId,
                        models = uiState.models,
                        availability = uiState.modelAvailability,
                        onRefreshAvailability = viewModel::checkModelAvailability,
                        onModelSelected = viewModel::selectModel
                    )
                    ErrorRetryBanner(
                        error = uiState.error,
                        onRetry = viewModel::retryLastMessage,
                        onDismiss = viewModel::dismissError,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = uiState.messages.isEmpty() && uiState.error == null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            AiChatEmptyState(
                                onQuickActionClick = viewModel::preFillMessage,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = uiState.messages.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            MessagesLazyColumn(
                                messages = uiState.messages,
                                accounts = uiState.accounts,
                                categories = uiState.categories,
                                isAiTyping = uiState.isAiTyping,
                                listState = listState,
                                onViewDetails = onNavigateToTransactions,
                                onConfirmDraft = viewModel::confirmDraft,
                                onCancelDraft = viewModel::cancelDraft,
                                onUpdateDraftField = viewModel::updateDraftField,
                                onConfirmTransfer = viewModel::confirmTransfer,
                                onCancelTransfer = viewModel::cancelTransfer,
                                onSaveLowBalanceLimit = viewModel::saveLowBalanceLimit,
                                onUpdateLowBalanceLimitField = viewModel::updateLowBalanceLimitField,
                                onDuplicateTransaction = viewModel::duplicateTransaction,
                                onStartEditingTransaction = viewModel::startEditingTransaction
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiModelSelector(
    selectedModelId: String,
    models: List<AiModelInfo>,
    availability: Map<String, AiModelAvailability>,
    onRefreshAvailability: () -> Unit,
    onModelSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var googleExpanded by remember { mutableStateOf(true) }
    var zaiExpanded by remember { mutableStateOf(false) }
    val selectedModel = models.firstOrNull { it.id == selectedModelId } ?: models.firstOrNull()
    val groupedModels = models.groupBy { it.provider }

    Surface(
        color = if (isSystemInDarkTheme()) ColorTokens.BackgroundDark else ColorTokens.BackgroundLight,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "نموذج الذكاء الاصطناعي:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box {
                Surface(
                    onClick = { expanded = true },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = selectedModel?.name ?: selectedModelId,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .widthIn(min = 260.dp, max = 300.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onRefreshAvailability, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.Sync, contentDescription = "فحص توفر النماذج", tint = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "اختر نموذج المساعد",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = "يتم فحص توفر كل نموذج تلقائياً",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ModelProviderSection(
                        provider = "Google",
                        models = groupedModels["Google"].orEmpty(),
                        expanded = googleExpanded,
                        selectedModelId = selectedModelId,
                        availability = availability,
                        onToggle = { googleExpanded = !googleExpanded },
                        onModelSelected = { modelId ->
                            expanded = false
                            onModelSelected(modelId)
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ModelProviderSection(
                        provider = "Z.ai",
                        models = groupedModels["Z.ai"].orEmpty(),
                        expanded = zaiExpanded,
                        selectedModelId = selectedModelId,
                        availability = availability,
                        onToggle = { zaiExpanded = !zaiExpanded },
                        onModelSelected = { modelId ->
                            expanded = false
                            onModelSelected(modelId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelProviderSection(
    provider: String,
    models: List<AiModelInfo>,
    expanded: Boolean,
    selectedModelId: String,
    availability: Map<String, AiModelAvailability>,
    onToggle: () -> Unit,
    onModelSelected: (String) -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = provider,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "${models.size} نماذج",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
            }
        },
        onClick = onToggle
    )

    if (expanded) {
        models.forEachIndexed { index, model ->
            val status = availability[model.id] ?: AiModelAvailability.CHECKING
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ModelAvailabilityDot(status = status)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = model.name,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (model.id == selectedModelId) FontWeight.ExtraBold else FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = modelStatusLabel(status),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                onClick = { onModelSelected(model.id) }
            )
            if (index < models.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun ModelAvailabilityDot(status: AiModelAvailability) {
    val color = when (status) {
        AiModelAvailability.CHECKING -> MaterialTheme.colorScheme.tertiary
        AiModelAvailability.AVAILABLE -> ColorTokens.Success
        AiModelAvailability.UNAVAILABLE -> MaterialTheme.colorScheme.error
    }
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color, CircleShape)
    )
}

private fun modelStatusLabel(status: AiModelAvailability): String = when (status) {
    AiModelAvailability.CHECKING -> "جار الفحص"
    AiModelAvailability.AVAILABLE -> "متوفر"
    AiModelAvailability.UNAVAILABLE -> "غير متوفر"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiChatTopBar(
    isTyping: Boolean,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onClearChat: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "مستشارك المالي",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .background(ColorTokens.Success, androidx.compose.foundation.shape.CircleShape)
                            .padding(4.dp)
                    )
                    Text(
                        text = if (isTyping) "يكتب..." else "متصل",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onOpenHistory) {
                Icon(Icons.Rounded.History, contentDescription = "سجل المحادثات", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "خيارات")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("مسح المحادثة") },
                    leadingIcon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onClearChat()
                    }
                )
                DropdownMenuItem(
                    text = { Text("تصدير قريباً") },
                    enabled = false,
                    onClick = {}
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = if (isSystemInDarkTheme()) ColorTokens.BackgroundDark else ColorTokens.BackgroundLight
        )
    )
}

@Composable
private fun ChatHistoryDrawer(
    sessions: List<String>,
    currentSession: String,
    onNewSession: () -> Unit,
    onSelectSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onClose: () -> Unit
) {
    ModalDrawerSheet(
        drawerShape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 28.dp, bottomEnd = 28.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(320.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "إغلاق")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("سجل المحادثات", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                    Text("${sessions.size} جلسة محفوظة", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Surface(
            onClick = onNewSession,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.ChatBubble, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("محادثة جديدة", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 18.dp)
        ) {
            if (sessions.isEmpty()) {
                item {
                    Text(
                        text = "لا توجد محادثات محفوظة بعد.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                items(sessions, key = { it }) { session ->
                    val selected = session == currentSession
                    NavigationDrawerItem(
                        label = { Text(session, maxLines = 1, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                        selected = selected,
                        onClick = { onSelectSession(session) },
                        icon = { Icon(if (selected) Icons.Rounded.ChatBubble else Icons.Rounded.ChatBubbleOutline, contentDescription = null) },
                        badge = {
                            IconButton(onClick = { onDeleteSession(session) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.DeleteSweep, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.72f), modifier = Modifier.size(18.dp))
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorRetryBanner(
    error: AiErrorState?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = error != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (error != null) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.28f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "إخفاء", tint = MaterialTheme.colorScheme.error)
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("تعذر الحصول على رد", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.error)
                        Text(error.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                    }
                    TextButton(onClick = onRetry) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إعادة المحاولة")
                    }
                }
            }
        }
    }
}

@Composable
private fun MessagesLazyColumn(
    messages: List<AiChatMessage>,
    accounts: List<com.example.domain.model.Account>,
    categories: List<com.example.domain.model.Category>,
    isAiTyping: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onViewDetails: () -> Unit,
    onConfirmDraft: (String) -> Unit,
    onCancelDraft: (String) -> Unit,
    onUpdateDraftField: (String, DraftField, Any) -> Unit,
    onConfirmTransfer: (String) -> Unit,
    onCancelTransfer: (String) -> Unit,
    onSaveLowBalanceLimit: (String, Double) -> Unit,
    onUpdateLowBalanceLimitField: (String, Double) -> Unit,
    onDuplicateTransaction: (com.example.domain.model.Transaction) -> Unit,
    onStartEditingTransaction: (String, com.example.domain.model.Transaction) -> Unit
) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val userMaxWidth = maxWidth * AiChatConstants.BUBBLE_MAX_WIDTH_FRACTION
        val aiMaxWidth = maxWidth * AiChatConstants.AI_BUBBLE_MAX_WIDTH_FRACTION
        val latestAiMessageId = messages.lastOrNull { !it.isUser }?.id

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            reverseLayout = false,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                items = messages,
                key = { _, message -> message.id }
            ) { index, message ->
                androidx.compose.animation.AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(androidx.compose.animation.core.tween(MotionTokens.DurationMedium)) +
                        slideInVertically { it / 2 }
                ) {
                    if (message.hasSmartAiContent()) {
                        ChatBubbleItem(
                            message = message,
                            accounts = accounts,
                            categories = categories,
                            onConfirmDraft = { onConfirmDraft(message.id) },
                            onCancelDraft = { onCancelDraft(message.id) },
                            onUpdateDraftField = { field, value -> onUpdateDraftField(message.id, field, value) },
                            onConfirmTransfer = { onConfirmTransfer(message.id) },
                            onCancelTransfer = { onCancelTransfer(message.id) },
                            onSaveLowBalanceLimit = { limit -> onSaveLowBalanceLimit(message.id, limit) },
                            onUpdateLowBalanceLimitField = { limit -> onUpdateLowBalanceLimitField(message.id, limit) },
                            onDuplicateTransaction = onDuplicateTransaction,
                            onStartEditingTransaction = onStartEditingTransaction
                        )
                    } else if (message.isUser) {
                        UserMessageBubble(message = message, maxWidth = userMaxWidth)
                    } else {
                        val isFirstInGroup = index == 0 || messages[index - 1].isUser
                        AiMessageBubble(
                            message = message,
                            isFirstInGroup = isFirstInGroup,
                            isLatestAiMessage = message.id == latestAiMessageId,
                            maxWidth = aiMaxWidth,
                            onViewDetails = onViewDetails
                        )
                    }
                }
            }

            item(key = "typing_indicator") {
                TypingIndicator(
                    visible = isAiTyping,
                    modifier = Modifier.fillMaxWidth()
                )
            }

        }
    }
}

private fun AiChatMessage.hasSmartAiContent(): Boolean {
    return !isUser && (
        draftTransaction != null ||
            walletSnapshot != null ||
            recentActivitySummary != null ||
            walletDistributionSuggestion != null ||
            lowBalanceAlertState != null ||
            transferDraftState != null ||
            selectedAccountDetailsState != null ||
            quickImpactPreviewState != null
        )
}
