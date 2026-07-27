package com.qdash.presentation.simulator

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.domain.model.PostalProfile
import com.qdash.domain.model.PostalProfileRole
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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

    // Find profile if editing
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

    // Initialize values if editing
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

    // Auto-generate full name
    val fullName = remember(firstName, lastName) {
        "${lastName.trim()} ${firstName.trim()}".trim()
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("اسم الملف (مثال: حسابي الرئيسي، بطاقة الأب)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("اللقب (بالفرنسية أو العربية)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("الاسم") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it.filter { c -> c.isDigit() } },
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
            }

            item {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف (اختياري)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان الكامل (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("البلدية / الولاية (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "نوع الحساب البريدي (الدور الافتراضي):",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PostalProfileRole.values().forEach { role ->
                            val isSelected = defaultRole == role
                            val label = when (role) {
                                PostalProfileRole.SELF -> "حسابي الشخصي"
                                PostalProfileRole.SENDER -> "مرسل (Expéditeur)"
                                PostalProfileRole.BENEFICIARY -> "مستفيد (Bénéficiaire)"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { defaultRole = role },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "إضافة إلى الحسابات المفضلة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Switch(
                        checked = isFavorite,
                        onCheckedChange = { isFavorite = it }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
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
                        Toast.makeText(context, "تم حفظ الملف بنجاح!", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "حفظ الحساب البريدي",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
