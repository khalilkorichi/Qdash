package com.example.presentation.ai.components

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import com.example.domain.model.TransactionType
import com.example.presentation.ai.AiChatMessage
import com.example.ui.theme.TextGray
import kotlinx.coroutines.launch

import com.example.presentation.ai.AiModelInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniChatOverlay(
    messages: List<AiChatMessage>,
    suggestions: List<String>,
    isLoading: Boolean,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onConfirmDraft: (String) -> Unit,
    onCancelDraft: (String) -> Unit,
    onFullScreenClick: () -> Unit,
    onCloseClick: () -> Unit,
    selectedModelId: String,
    models: List<AiModelInfo>,
    onModelSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
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
                        onInputTextChange(text)
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

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Card(
        modifier = modifier
            .width(360.dp)
            .height(500.dp)
            .shadow(16.dp, shape = RoundedCornerShape(24.dp))
            .border(
                BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "مساعدك المالي الذكي",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onFullScreenClick) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "ملء الشاشة",
                        tint = Color.White
                    )
                }

                IconButton(onClick = onCloseClick) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color.White
                    )
                }
            }

            // Model Selector Pill Row for Overlay
            var isModelDropdownExpanded by remember { mutableStateOf(false) }
            val selectedModel = models.find { it.id == selectedModelId } ?: models.firstOrNull()

            if (selectedModel != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "النموذج النشط:",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                    
                    Box {
                        Surface(
                            onClick = { isModelDropdownExpanded = true },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = selectedModel.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = if (isModelDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        
                        var isGoogleExpanded by remember { mutableStateOf(true) }
                        var isZaiExpanded by remember { mutableStateOf(true) }

                        DropdownMenu(
                            expanded = isModelDropdownExpanded,
                            onDismissRequest = { isModelDropdownExpanded = false },
                            modifier = Modifier.width(260.dp).background(MaterialTheme.colorScheme.surface)
                        ) {
                            // Google Header
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("جوجل (Google)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                        Icon(
                                            imageVector = if (isGoogleExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                onClick = { isGoogleExpanded = !isGoogleExpanded }
                            )
                            
                            if (isGoogleExpanded) {
                                models.filter { it.provider == "Google" }.forEach { model ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = model.name, fontSize = 12.sp)
                                        },
                                        onClick = {
                                            onModelSelected(model.id)
                                            isModelDropdownExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = if (model.id == selectedModelId) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        },
                                        modifier = Modifier.padding(start = 8.dp)
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
                                        Text("Z.ai (Agent Router)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                                        Icon(
                                            imageVector = if (isZaiExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                onClick = { isZaiExpanded = !isZaiExpanded }
                            )
                            
                            if (isZaiExpanded) {
                                models.filter { it.provider == "Z.ai" }.forEach { model ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = model.name, fontSize = 12.sp)
                                        },
                                        onClick = {
                                            onModelSelected(model.id)
                                            isModelDropdownExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = if (model.id == selectedModelId) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        },
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            }

            // Messages & Suggestions Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                if (messages.isEmpty()) {
                    // Welcome placeholder
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = TextGray.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "أنا هنا لمساعدتك! يمكنك إضافة معاملة بسرعة أو الاستفسار عن ميزانيتك.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages) { message ->
                            ChatBubbleItem(
                                message = message,
                                onConfirmDraft = { onConfirmDraft(message.id) },
                                onCancelDraft = { onCancelDraft(message.id) }
                            )
                        }

                        if (isLoading) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "جاري التفكير...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextGray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Suggestions List
            if (suggestions.isNotEmpty() && !isLoading) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(suggestions) { suggestion ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            modifier = Modifier.clickable { onSuggestionClick(suggestion) }
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Input Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    placeholder = { Text("اكتب رسالتك هنا...", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    maxLines = 2,
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
                            onClick = onSendMessage,
                            enabled = inputText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "إرسال",
                                tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else TextGray.copy(alpha = 0.4f)
                            )
                        }
                    }
                )
            }
        }
    }
}

fun parseMarkdown(text: String, baseColor: Color): AnnotatedString {
    val cleanedText = text.replace("[\u200B-\u200D\u200E\u200F\uFEFF]".toRegex(), "")
    return buildAnnotatedString {
        var i = 0
        val length = cleanedText.length
        while (i < length) {
            when {
                // Strikethrough: ~~text~~
                i + 1 < length && cleanedText[i] == '~' && cleanedText[i+1] == '~' -> {
                    val end = cleanedText.indexOf("~~", i + 2)
                    if (end != -1) {
                        pushStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
                        append(cleanedText.substring(i + 2, end))
                        pop()
                        i = end + 2
                    } else {
                        append("~~")
                        i += 2
                    }
                }
                // Bold: **text**
                i + 1 < length && cleanedText[i] == '*' && cleanedText[i+1] == '*' -> {
                    val end = cleanedText.indexOf("**", i + 2)
                    if (end != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        // Recursively parse inner content for nested formatting
                        val inner = cleanedText.substring(i + 2, end)
                        append(parseMarkdownInner(inner, baseColor))
                        pop()
                        i = end + 2
                    } else {
                        append("**")
                        i += 2
                    }
                }
                // Italic: *text*
                cleanedText[i] == '*' -> {
                    val end = cleanedText.indexOf('*', i + 1)
                    if (end != -1) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(cleanedText.substring(i + 1, end))
                        pop()
                        i = end + 1
                    } else {
                        append("*")
                        i++
                    }
                }
                // Inline Code: `code`
                cleanedText[i] == '`' -> {
                    val end = cleanedText.indexOf('`', i + 1)
                    if (end != -1) {
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = baseColor.copy(alpha = 0.08f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                letterSpacing = 0.3.sp
                            )
                        )
                        append(" ")
                        append(cleanedText.substring(i + 1, end))
                        append(" ")
                        pop()
                        i = end + 1
                    } else {
                        append("`")
                        i++
                    }
                }
                else -> {
                    append(cleanedText[i])
                    i++
                }
            }
        }
    }
}

// Inner parser that avoids infinite recursion for nested inline styles
private fun parseMarkdownInner(text: String, baseColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val length = text.length
        while (i < length) {
            when {
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end != -1) {
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = baseColor.copy(alpha = 0.08f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        )
                        append(" ")
                        append(text.substring(i + 1, end))
                        append(" ")
                        pop()
                        i = end + 1
                    } else {
                        append("`")
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}

@Composable
fun CodeBlock(code: String, language: String = "") {
    val accentColor = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Language label header
        if (language.isNotEmpty()) {
            Surface(
                color = accentColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
            ) {
                Text(
                    text = language,
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
            shape = if (language.isNotEmpty())
                RoundedCornerShape(topStart = 0.dp, topEnd = 10.dp, bottomStart = 10.dp, bottomEnd = 10.dp)
            else
                RoundedCornerShape(10.dp),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Text(
                text = code,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun MarkdownMessageText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val lines = text.split("\n")
    val accentColor = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        var inCodeBlock = false
        var codeBlockLanguage = ""
        val codeBlockLines = remember { mutableStateListOf<String>() }

        lines.forEach { line ->
            // Clean up RTL markers and zero-width spaces
            val cleanLine = line.replace("[\u200B-\u200D\u200E\u200F\uFEFF]".toRegex(), "")
            val trimmed = cleanLine.trim()

            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    val codeText = codeBlockLines.joinToString("\n")
                    CodeBlock(codeText, codeBlockLanguage)
                    codeBlockLines.clear()
                    codeBlockLanguage = ""
                    inCodeBlock = false
                } else {
                    codeBlockLanguage = trimmed.removePrefix("```").trim()
                    inCodeBlock = true
                }
            } else if (inCodeBlock) {
                codeBlockLines.add(line)
            } else {
                when {
                    // Horizontal rule: --- or *** or ___
                    trimmed.matches("^[-*_]{3,}$".toRegex()) -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            color.copy(alpha = 0f),
                                            color.copy(alpha = 0.2f),
                                            color.copy(alpha = 0.2f),
                                            color.copy(alpha = 0f)
                                        )
                                    )
                                )
                        )
                    }
                    // H1
                    trimmed.startsWith("# ") -> {
                        Column(modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)) {
                            Text(
                                text = parseMarkdown(trimmed.removePrefix("# "), color),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = color,
                                    lineHeight = 28.sp
                                )
                            )
                            // Decorative accent line under H1
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .padding(top = 4.dp)
                                    .height(2.5.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(accentColor, accentColor.copy(alpha = 0.3f))
                                        ),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                    // H2
                    trimmed.startsWith("## ") -> {
                        Column(modifier = Modifier.padding(top = 4.dp, bottom = 1.dp)) {
                            Text(
                                text = parseMarkdown(trimmed.removePrefix("## "), color),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = color,
                                    lineHeight = 24.sp
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .padding(top = 3.dp)
                                    .height(2.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(accentColor.copy(alpha = 0.6f), accentColor.copy(alpha = 0.1f))
                                        ),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                    // H3
                    trimmed.startsWith("### ") -> {
                        Text(
                            text = parseMarkdown(trimmed.removePrefix("### "), color),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = color,
                                lineHeight = 22.sp
                            ),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    // Numbered list: 1. item, 2. item, etc.
                    trimmed.matches("^\\d+[.):]\\s+.*".toRegex()) -> {
                        val match = "^(\\d+)[.):]\\s+(.*)".toRegex().find(trimmed)
                        if (match != null) {
                            val number = match.groupValues[1]
                            val content = match.groupValues[2]
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.padding(start = 4.dp, top = 1.dp)
                            ) {
                                Surface(
                                    color = accentColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(end = 6.dp, top = 2.dp)
                                ) {
                                    Text(
                                        text = number,
                                        style = style.copy(
                                            color = accentColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                                Text(
                                    text = parseMarkdown(content, color),
                                    style = style.copy(color = color),
                                    lineHeight = style.lineHeight
                                )
                            }
                        }
                    }
                    // Bullet list
                    trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") || trimmed.matches("^[-*•]\\s+.*".toRegex()) -> {
                        val content = trimmed.replaceFirst("^[-*•]\\s+".toRegex(), "")
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(start = 4.dp, top = 1.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 7.dp, end = 8.dp)
                                    .size(5.dp)
                                    .background(accentColor.copy(alpha = 0.6f), shape = CircleShape)
                            )
                            Text(
                                text = parseMarkdown(content, color),
                                style = style.copy(color = color),
                                lineHeight = style.lineHeight
                            )
                        }
                    }
                    // Blockquote
                    trimmed.startsWith("> ") -> {
                        val content = trimmed.removePrefix("> ")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(IntrinsicSize.Min)
                                    .defaultMinSize(minHeight = 20.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(accentColor, accentColor.copy(alpha = 0.3f))
                                        ),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                            Surface(
                                color = color.copy(alpha = 0.04f),
                                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp)
                            ) {
                                Text(
                                    text = parseMarkdown(content, color),
                                    style = style.copy(color = color.copy(alpha = 0.85f), fontStyle = FontStyle.Italic),
                                    lineHeight = style.lineHeight,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    trimmed.isEmpty() -> {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    else -> {
                        Text(
                            text = parseMarkdown(line, color),
                            style = style.copy(color = color),
                            lineHeight = style.lineHeight
                        )
                    }
                }
            }
        }

        if (inCodeBlock && codeBlockLines.isNotEmpty()) {
            CodeBlock(codeBlockLines.joinToString("\n"), codeBlockLanguage)
            codeBlockLines.clear()
        }
    }
}

@Composable
fun ChatBubbleItem(
    message: AiChatMessage,
    onConfirmDraft: () -> Unit,
    onCancelDraft: () -> Unit
) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (message.isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bgColor,
            shape = shape,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                MarkdownMessageText(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }

        // Transaction Draft Card
        if (message.draftTransaction != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier
                    .width(280.dp)
                    .border(BorderStroke(1.dp, Color(0xFFE9E9E6)), shape = RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "📝 مسودة معاملة مقترحة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "المبلغ:", fontSize = 12.sp, color = TextGray)
                        Text(
                            text = "${message.draftTransaction.amount} دج",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "النوع:", fontSize = 12.sp, color = TextGray)
                        val typeStr = when (message.draftTransaction.type) {
                            TransactionType.EXPENSE -> "مصاريف 🔴"
                            TransactionType.INCOME -> "دخل 🟢"
                            TransactionType.TRANSFER -> "تحويل 🔵"
                        }
                        Text(text = typeStr, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "الفئة:", fontSize = 12.sp, color = TextGray)
                        Text(text = message.categoryName ?: "غير محدد", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "الحساب:", fontSize = 12.sp, color = TextGray)
                        Text(text = message.accountName ?: "غير محدد", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when {
                        message.isConfirmed -> {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF22C55E).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "تمت الإضافة بنجاح ✅",
                                    color = Color(0xFF22C55E),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                        message.isCancelled -> {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFFEF4444).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "تم إلغاء المسودة ❌",
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onCancelDraft,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text("إلغاء", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = onConfirmDraft,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text("تأكيد", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
