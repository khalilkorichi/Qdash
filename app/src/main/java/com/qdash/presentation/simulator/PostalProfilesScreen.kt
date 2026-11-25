package com.qdash.presentation.simulator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.core.ui.components.EmptyStateView
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.core.utils.PostalCcpUtils
import com.qdash.domain.model.PostalProfile
import com.qdash.domain.model.PostalProfileRole
import com.qdash.presentation.simulator.components.SimulatorBottomBar
import com.qdash.presentation.simulator.components.SimulatorBottomTab
import com.qdash.ui.designsystem.components.AppButton
import com.qdash.ui.designsystem.components.AppDialog
import com.qdash.ui.designsystem.components.ButtonVariant
import com.qdash.ui.designsystem.tokens.ColorTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostalProfilesScreen(
    viewModel: DocumentSimulatorViewModel,
    onAddProfile: () -> Unit,
    onEditProfile: (Long) -> Unit,
    onSelectDocType: (DocumentType) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var scrollDeltaAccumulator by remember { mutableFloatStateOf(0f) }
    var isBottomBarVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0f) {
                    scrollDeltaAccumulator += delta
                    if (scrollDeltaAccumulator < -100f) isBottomBarVisible = false
                } else if (delta > 10f) {
                    isBottomBarVisible = true
                    scrollDeltaAccumulator = 0f
                }
                return Offset.Zero
            }
        }
    }

    val listBottomPadding by animateDpAsState(
        targetValue = if (isBottomBarVisible) 164.dp else 88.dp,
        animationSpec = tween(220),
        label = "profiles_bottom_padding"
    )
    val fabBottomPadding by animateDpAsState(
        targetValue = if (isBottomBarVisible) 92.dp else 16.dp,
        animationSpec = tween(220),
        label = "profiles_fab_padding"
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        topBar = {
            FinTrackTopBar(
                title = "الحسابات البريدية",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            PostalProfilesPageContent(
                uiState = uiState,
                isDark = isDark,
                fabBottomPadding = fabBottomPadding,
                listBottomPadding = listBottomPadding,
                onAddProfile = onAddProfile,
                onEditProfile = onEditProfile,
                onToggleFavorite = { viewModel.toggleProfileFavorite(it) },
                onDeleteProfile = { viewModel.deletePostalProfile(it) }
            )

            // Floating Qdash-style 3-tab simulator bottom bar
            SimulatorBottomBar(
                selectedTab = SimulatorBottomTab.POSTAL_PROFILES,
                isVisible = isBottomBarVisible,
                onSelectTab = { tab ->
                    when (tab) {
                        SimulatorBottomTab.CHEQUE -> onSelectDocType(DocumentType.CHEQUE)
                        SimulatorBottomTab.CHEQUE_SECOURS -> onSelectDocType(DocumentType.SFP01)
                        SimulatorBottomTab.POSTAL_PROFILES -> Unit
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Reusable Postal Profiles page body shared cleanly between standalone PostalProfilesScreen
 * and Page 2 ("الحساب البريدي") of the swipeable simulator HorizontalPager.
 */
@Composable
fun PostalProfilesPageContent(
    uiState: DocumentSimulatorUiState,
    isDark: Boolean,
    fabBottomPadding: androidx.compose.ui.unit.Dp,
    listBottomPadding: androidx.compose.ui.unit.Dp,
    onAddProfile: () -> Unit,
    onEditProfile: (Long) -> Unit,
    onToggleFavorite: (PostalProfile) -> Unit,
    onDeleteProfile: (PostalProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profiles = uiState.savedProfiles
    var selectedRoleFilter by remember { mutableStateOf<PostalProfileRole?>(null) }
    var profilePendingDelete by remember { mutableStateOf<PostalProfile?>(null) }

    val filteredProfiles = remember(profiles, selectedRoleFilter) {
        val base = if (selectedRoleFilter == null) profiles else profiles.filter { it.defaultRole == selectedRoleFilter }
        base.sortedByDescending { it.isFavorite }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("postal_profiles_page")
    ) {
        if (profiles.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 96.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EmptyStateView(
                    title = "لا توجد حسابات بريدية محفوظة",
                    description = "أضف حسابك البريدي أو حسابات المستفيدين لتعبئة الصك وشاك سوكور بضغطة واحدة.",
                    icon = Icons.Default.AccountBalanceWallet
                )
                Spacer(modifier = Modifier.height(16.dp))
                AppButton(
                    onClick = onAddProfile,
                    variant = ButtonVariant.SOLID,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_add_first_profile")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة حساب بريدي", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("postal_profiles_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = listBottomPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    PostalProfilesSummaryHeader(
                        totalCount = profiles.size,
                        favoritesCount = profiles.count { it.isFavorite },
                        isDark = isDark
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedRoleFilter == null,
                                onClick = { selectedRoleFilter = null },
                                label = { Text("الكل (${profiles.size})", fontSize = 12.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedRoleFilter == PostalProfileRole.SELF,
                                onClick = {
                                    selectedRoleFilter = if (selectedRoleFilter == PostalProfileRole.SELF) null else PostalProfileRole.SELF
                                },
                                label = { Text("حسابي الشخصي", fontSize = 12.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedRoleFilter == PostalProfileRole.SENDER,
                                onClick = {
                                    selectedRoleFilter = if (selectedRoleFilter == PostalProfileRole.SENDER) null else PostalProfileRole.SENDER
                                },
                                label = { Text("مرسل", fontSize = 12.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedRoleFilter == PostalProfileRole.BENEFICIARY,
                                onClick = {
                                    selectedRoleFilter = if (selectedRoleFilter == PostalProfileRole.BENEFICIARY) null else PostalProfileRole.BENEFICIARY
                                },
                                label = { Text("مستفيد", fontSize = 12.sp) }
                            )
                        }
                    }
                }

                items(filteredProfiles, key = { it.id }) { profile ->
                    PostalProfileCard(
                        profile = profile,
                        isDark = isDark,
                        onEditClick = { onEditProfile(profile.id) },
                        onToggleFavorite = { onToggleFavorite(profile) },
                        onCopyRip = { rip ->
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cb?.setPrimaryClip(ClipData.newPlainText("RIP", rip))
                            Toast.makeText(context, "تم نسخ رقم RIP!", Toast.LENGTH_SHORT).show()
                        },
                        onDeleteClick = { profilePendingDelete = profile }
                    )
                }
            }
        }

        // Floating Add Button above the SimulatorBottomBar
        ExtendedFloatingActionButton(
            onClick = onAddProfile,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp),
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("حساب جديد", fontWeight = FontWeight.Bold) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = fabBottomPadding)
                .testTag("fab_add_postal_profile")
        )
    }

    profilePendingDelete?.let { pending ->
        AppDialog(
            onDismissRequest = { profilePendingDelete = null },
            title = "حذف الحساب البريدي",
            text = "هل أنت متأكد من حذف الحساب «${pending.profileName}»؟ لا يمكن التراجع عن هذا الإجراء.",
            confirmButtonText = "حذف",
            dismissButtonText = "إلغاء",
            isDestructive = true,
            onConfirm = {
                onDeleteProfile(pending)
                profilePendingDelete = null
            }
        )
    }
}

@Composable
private fun PostalProfilesSummaryHeader(
    totalCount: Int,
    favoritesCount: Int,
    isDark: Boolean
) {
    val accent = if (isDark) ColorTokens.PosteYellow else ColorTokens.PosteBlue
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(accent.copy(alpha = 0.25f), accent.copy(alpha = 0.08f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "$totalCount حسابات محفوظة",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$favoritesCount في المفضلة • جاهزة للتعبئة الفورية",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PostalProfileCard(
    profile: PostalProfile,
    onEditClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    onCopyRip: (String) -> Unit = {}
) {
    val roleColor = when (profile.defaultRole) {
        PostalProfileRole.SELF -> if (isDark) ColorTokens.InfoDark else ColorTokens.PosteBlue
        PostalProfileRole.SENDER -> if (isDark) ColorTokens.SuccessDark else ColorTokens.Success
        PostalProfileRole.BENEFICIARY -> if (isDark) ColorTokens.PosteYellow else Color(0xFFD97706)
    }

    val roleName = when (profile.defaultRole) {
        PostalProfileRole.SELF -> "حسابي"
        PostalProfileRole.SENDER -> "مرسل"
        PostalProfileRole.BENEFICIARY -> "مستفيد"
    }

    val derivedRip = remember(profile.accountNumber) {
        PostalCcpUtils.buildFullRip(profile.accountNumber)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEditClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(roleColor.copy(alpha = 0.25f), roleColor.copy(alpha = 0.08f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = roleColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = profile.profileName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                color = roleColor.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = roleName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = roleColor,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = profile.fullName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (profile.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "تفضيل",
                            tint = if (profile.isFavorite) ColorTokens.PosteYellow else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Account CCP / Clé & RIP footer bar
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "\u2066CCP: ${profile.accountNumber}  •  Clé: ${profile.accountKey}\u2069",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (derivedRip != null) {
                            Text(
                                text = "\u2066RIP: $derivedRip\u2069",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (derivedRip != null) {
                        Surface(
                            onClick = { onCopyRip(derivedRip) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "نسخ RIP",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "RIP",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (!profile.city.isNullOrBlank() || !profile.address.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = listOfNotNull(profile.city?.takeIf { it.isNotBlank() }, profile.address?.takeIf { it.isNotBlank() })
                            .joinToString(" - "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
