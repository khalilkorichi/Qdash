package com.qdash.presentation.simulator

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.core.utils.PostalCcpUtils
import com.qdash.domain.model.PostalProfile
import com.qdash.domain.model.PostalProfileRole
import com.qdash.ui.designsystem.tokens.ColorTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditPostalProfileScreen(
    viewModel: DocumentSimulatorViewModel,
    profileId: Long?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val editingProfile = remember(profileId, uiState.savedProfiles) {
        if (profileId != null && profileId != 0L) {
            uiState.savedProfiles.find { it.id == profileId }
        } else null
    }

    var profileName by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var accountKey by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var defaultRole by remember { mutableStateOf(PostalProfileRole.SELF) }
    var isFavorite by remember { mutableStateOf(false) }

    LaunchedEffect(editingProfile) {
        editingProfile?.let {
            profileName = it.profileName
            firstName = it.firstName
            lastName = it.lastName
            accountNumber = it.accountNumber
            accountKey = it.accountKey
            phone = it.phone ?: ""
            address = it.address ?: ""
            city = it.city ?: ""
            defaultRole = it.defaultRole
            isFavorite = it.isFavorite
        }
    }

    val fullName = remember(firstName, lastName) {
        "${lastName.trim()} ${firstName.trim()}".trim()
    }
    val expectedKey = remember(accountNumber) { PostalCcpUtils.calculateCcpKey(accountNumber) }
    val derivedRip = remember(accountNumber) { PostalCcpUtils.buildFullRip(accountNumber) }
    val isKeyValid = remember(accountNumber, accountKey) {
        accountKey.length == 2 && PostalCcpUtils.isValidCcpKey(accountNumber, accountKey)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            FinTrackTopBar(
                title = if (editingProfile != null) "تعديل حساب بريدي" else "حساب بريدي جديد",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .testTag("create_edit_postal_profile_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Live Postal Account Card Preview
            item {
                LivePostalAccountCardPreview(
                    profileName = profileName.ifBlank { "اسم البطاقة البريدية" },
                    fullName = fullName.ifBlank { "اللقب والاسم" },
                    ccp = accountNumber,
                    key = accountKey,
                    rip = derivedRip,
                    role = defaultRole,
                    isDark = isDark
                )
            }

            // 2. Identity & Profile Title Card
            item {
                ProfileFormSectionCard(title = "هوية الحساب", icon = Icons.Default.Badge) {
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("اسم الملف (مثال: حسابي الرئيسي، بطاقة الأب)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text("اللقب (Nom)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("الاسم (Prénom)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // 3. CCP & Key Section (with instant 20-digit RIP paste support)
            item {
                ProfileFormSectionCard(title = "بيانات الحساب البريدي (CCP)", icon = Icons.Default.AccountBalance) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { raw ->
                                val extracted = PostalCcpUtils.extractFromInput(raw)
                                val rawDigits = raw.filter { it.isDigit() }
                                if (rawDigits.length == 20 && extracted != null) {
                                    accountNumber = extracted.ccpNumber
                                    accountKey = extracted.ccpKey
                                } else {
                                    accountNumber = rawDigits.take(10)
                                }
                            },
                            label = { Text("رقم الحساب CCP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = accountKey,
                            onValueChange = { accountKey = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("المفتاح (Clé)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val text = cb?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                                val extracted = PostalCcpUtils.extractFromInput(text)
                                if (extracted != null) {
                                    accountNumber = extracted.ccpNumber
                                    accountKey = extracted.ccpKey
                                    Toast.makeText(context, "تم استخراج CCP والمفتاح!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "انسخ رقم RIP أو CCP/Clé أولاً", Toast.LENGTH_SHORT).show()
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp))
                            },
                            label = { Text("لصق من الحافظة", fontSize = 11.5.sp) }
                        )

                        if (expectedKey != null) {
                            when {
                                isKeyValid -> {
                                    Text(
                                        text = "المفتاح صحيح ✓",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isDark) ColorTokens.SuccessDark else ColorTokens.Success
                                    )
                                }
                                accountKey.isEmpty() || accountKey != expectedKey -> {
                                    TextButton(
                                        onClick = { accountKey = expectedKey },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "استخدام المفتاح ($expectedKey)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Address & Contact Section
            item {
                ProfileFormSectionCard(title = "العنوان والاتصال (اختياري)", icon = Icons.Default.LocationOn) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("البلدية / الولاية") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("العنوان الكامل") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // 5. Role & Favorite Card
            item {
                ProfileFormSectionCard(title = "التصنيف الافتراضي", icon = Icons.Default.Tune) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PostalProfileRole.values().forEach { role ->
                            val isSelected = defaultRole == role
                            val label = when (role) {
                                PostalProfileRole.SELF -> "حسابي"
                                PostalProfileRole.SENDER -> "مرسل"
                                PostalProfileRole.BENEFICIARY -> "مستفيد"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { defaultRole = role },
                                label = {
                                    Text(
                                        text = label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تثبيت في الحسابات المفضلة",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "يظهر أولاً في قائمة التعبئة الفورية",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isFavorite,
                            onCheckedChange = { isFavorite = it }
                        )
                    }
                }
            }

            // 6. Save Action Button
            item {
                Button(
                    onClick = {
                        if (profileName.isBlank() || firstName.isBlank() || lastName.isBlank() || accountNumber.isBlank() || accountKey.isBlank()) {
                            Toast.makeText(context, "الرجاء ملء جميع الحقول الإلزامية!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (accountKey.length != 2) {
                            Toast.makeText(context, "المفتاح يجب أن يكون من رقمين!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val newProfile = PostalProfile(
                            id = editingProfile?.id ?: 0L,
                            profileName = profileName.trim(),
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            fullName = fullName,
                            accountNumber = accountNumber.trim(),
                            accountKey = accountKey.trim(),
                            phone = phone.trim().ifBlank { null },
                            address = address.trim().ifBlank { null },
                            city = city.trim().ifBlank { null },
                            defaultRole = defaultRole,
                            isFavorite = isFavorite,
                            createdAt = editingProfile?.createdAt ?: System.currentTimeMillis()
                        )

                        viewModel.savePostalProfile(newProfile)
                        Toast.makeText(context, "تم حفظ الحساب البريدي بنجاح!", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_save_postal_profile"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (editingProfile != null) "حفظ التعديلات" else "حفظ الحساب البريدي",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LivePostalAccountCardPreview(
    profileName: String,
    fullName: String,
    ccp: String,
    key: String,
    rip: String?,
    role: PostalProfileRole,
    isDark: Boolean
) {
    val startColor = if (isDark) Color(0xFF1E293B) else Color(0xFF0F2B8C)
    val endColor = if (isDark) Color(0xFF0F172A) else Color(0xFF1E40AF)
    val roleLabel = when (role) {
        PostalProfileRole.SELF -> "حسابي"
        PostalProfileRole.SENDER -> "مرسل"
        PostalProfileRole.BENEFICIARY -> "مستفيد"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(startColor, endColor)))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profileName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = fullName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        color = ColorTokens.PosteYellow.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, ColorTokens.PosteYellow.copy(alpha = 0.65f)),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = roleLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ColorTokens.PosteYellow,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "\u2066CCP: ${ccp.ifBlank { "••••••••" }}   Clé: ${key.ifBlank { "••" }}\u2069",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "\u2066RIP: ${rip ?: "00799999••••••••••••"}\u2069",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileFormSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}
