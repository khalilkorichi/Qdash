package com.qdash.presentation.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.qdash.presentation.ai.components.ChatBubbleItem
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.MotionTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AiModelSelector(
    selectedModelId: String,
    models: List<AiModelInfo>,
    availability: Map<String, AiModelAvailability>,
    onRefreshAvailability: () -> Unit,
    onModelSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedModel = models.firstOrNull { it.id == selectedModelId } ?: models.firstOrNull()

    val availableModels = remember(models, availability) {
        models.filter {
            val status = availability[it.id]
            status == AiModelAvailability.AVAILABLE || status == AiModelAvailability.CHECKING || status == null
        }
    }
    val unavailableModels = remember(models, availability) {
        models.filter { availability[it.id] == AiModelAvailability.UNAVAILABLE }
    }

    val groupedAvailable = remember(availableModels) { availableModels.groupBy { it.provider } }
    val sortedAvailableProviders = remember(groupedAvailable) {
        groupedAvailable.keys.sortedWith { a, b ->
            when {
                a == "Google" && b != "Google" -> -1
                b == "Google" && a != "Google" -> 1
                a == "Z.ai" && b != "Z.ai" -> -1
                b == "Z.ai" && a != "Z.ai" -> 1
                else -> a.compareTo(b)
            }
        }
    }

    val groupedUnavailable = remember(unavailableModels) { unavailableModels.groupBy { it.provider } }
    val sortedUnavailableProviders = remember(groupedUnavailable) {
        groupedUnavailable.keys.sortedWith { a, b ->
            when {
                a == "Google" && b != "Google" -> -1
                b == "Google" && a != "Google" -> 1
                a == "Z.ai" && b != "Z.ai" -> -1
                b == "Z.ai" && a != "Z.ai" -> 1
                else -> a.compareTo(b)
            }
        }
    }

    var expandedSections by remember { mutableStateOf(setOf("available_Google")) }
    var availableListExpanded by remember { mutableStateOf(true) }
    var unavailableListExpanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.background,
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
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
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
                    modifier = Modifier.widthIn(min = 260.dp, max = 300.dp)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "اختر نموذج المساعد",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Start
                                )
                                Text(
                                    text = "يتم فحص توفر كل نموذج تلقائياً",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Start
                                )
                            }
                            IconButton(onClick = onRefreshAvailability, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.Sync, contentDescription = "فحص توفر النماذج", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        if (availableModels.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { availableListExpanded = !availableListExpanded }
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "النماذج المتوفرة",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    imageVector = if (availableListExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = if (availableListExpanded) "إغلاق" else "فتح",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                            if (availableListExpanded) {
                                sortedAvailableProviders.forEach { provider ->
                                    val sectionKey = "available_$provider"
                                    ModelProviderSection(
                                        provider = provider,
                                        models = groupedAvailable[provider].orEmpty(),
                                        expanded = expandedSections.contains(sectionKey),
                                        selectedModelId = selectedModelId,
                                        availability = availability,
                                        onToggle = {
                                            expandedSections = if (expandedSections.contains(sectionKey)) {
                                                expandedSections - sectionKey
                                            } else {
                                                expandedSections + sectionKey
                                            }
                                        },
                                        onModelSelected = { modelId ->
                                            expanded = false
                                            onModelSelected(modelId)
                                        }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                }
                            }
                        }

                        if (unavailableModels.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { unavailableListExpanded = !unavailableListExpanded }
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Cancel,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "النماذج غير المتوفرة",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Icon(
                                    imageVector = if (unavailableListExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = if (unavailableListExpanded) "إغلاق" else "فتح",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                            if (unavailableListExpanded) {
                                sortedUnavailableProviders.forEach { provider ->
                                    val sectionKey = "unavailable_$provider"
                                    ModelProviderSection(
                                        provider = provider,
                                        models = groupedUnavailable[provider].orEmpty(),
                                        expanded = expandedSections.contains(sectionKey),
                                        selectedModelId = selectedModelId,
                                        availability = availability,
                                        onToggle = {
                                            expandedSections = if (expandedSections.contains(sectionKey)) {
                                                expandedSections - sectionKey
                                            } else {
                                                expandedSections + sectionKey
                                            }
                                        },
                                        onModelSelected = { modelId ->
                                            expanded = false
                                            onModelSelected(modelId)
                                        }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ModelProviderSection(
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
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = provider,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "${models.size} نماذج",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp).rotate(if (expanded) 180f else 0f)
                )
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
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ModelAvailabilityIcon(status = status)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = model.name,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (model.id == selectedModelId) FontWeight.ExtraBold else FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = modelStatusLabel(status),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Start,
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
internal fun ModelAvailabilityIcon(status: AiModelAvailability) {
    val (icon, color) = when (status) {
        AiModelAvailability.CHECKING -> Pair(Icons.Rounded.Pending, MaterialTheme.colorScheme.tertiary)
        AiModelAvailability.AVAILABLE -> Pair(Icons.Rounded.CheckCircle, ColorTokens.Success)
        AiModelAvailability.UNAVAILABLE -> Pair(Icons.Rounded.Cancel, MaterialTheme.colorScheme.error)
    }
    Icon(
        imageVector = icon,
        contentDescription = modelStatusLabel(status),
        tint = color,
        modifier = Modifier.size(16.dp)
    )
}

internal fun modelStatusLabel(status: AiModelAvailability): String = when (status) {
    AiModelAvailability.CHECKING -> "جار الفحص"
    AiModelAvailability.AVAILABLE -> "متوفر"
    AiModelAvailability.UNAVAILABLE -> "غير متوفر"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AiChatTopBar(
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
                Row(
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .background(ColorTokens.Success, CircleShape)
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
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
internal fun ChatHistoryDrawer(
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
internal fun ErrorRetryBanner(
    error: AiErrorState?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (error != null) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.28f))
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
internal fun MessagesLazyColumn(
    messages: List<AiChatMessage>,
    accounts: List<com.qdash.domain.model.Account>,
    categories: List<com.qdash.domain.model.Category>,
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
    onDuplicateTransaction: (com.qdash.domain.model.Transaction) -> Unit,
    onStartEditingTransaction: (String, com.qdash.domain.model.Transaction) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
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
                AnimatedVisibility(
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
