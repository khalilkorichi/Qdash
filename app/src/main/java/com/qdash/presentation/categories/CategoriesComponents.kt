package com.qdash.presentation.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.domain.model.Category
import com.qdash.domain.model.CategoryType
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.TextGray
import com.qdash.ui.designsystem.components.shimmerEffect

val iconOptions = listOf(
    "restaurant", "shopping_bag", "home", "directions_car", "bolt", "school",
    "medical_services", "sports_esports", "coffee", "shopping_cart", "local_taxi",
    "work", "savings", "star", "flag", "favorite"
)

val colorOptions = listOf(
    "#6C63FF", "#22C55E", "#EF4444", "#3B82F6", "#F59E0B",
    "#EC4899", "#8B5CF6", "#06B6D4", "#10B981", "#F97316"
)

fun parseHex(hex: String): Color {
    return try {
        val clean = hex.trimStart('#')
        Color(android.graphics.Color.parseColor("#$clean"))
    } catch (e: Exception) { Color(0xFF6C63FF) }
}

fun categoryTypeLabel(type: CategoryType) = when (type) {
    CategoryType.EXPENSE -> "مصروف"
    CategoryType.INCOME -> "دخل"
}

fun getIconVector(iconName: String): ImageVector {
    return when (iconName) {
        "restaurant" -> Icons.Default.Restaurant
        "shopping_bag" -> Icons.Default.ShoppingBag
        "home" -> Icons.Default.Home
        "directions_car" -> Icons.Default.DirectionsCar
        "bolt" -> Icons.Default.Bolt
        "school" -> Icons.Default.School
        "medical_services" -> Icons.Default.MedicalServices
        "sports_esports" -> Icons.Default.SportsEsports
        "coffee" -> Icons.Default.Coffee
        "shopping_cart" -> Icons.Default.ShoppingCart
        "local_taxi" -> Icons.Default.LocalTaxi
        "work" -> Icons.Default.Work
        "savings" -> Icons.Default.Savings
        "star" -> Icons.Default.Star
        "flag" -> Icons.Default.Flag
        "favorite" -> Icons.Default.Favorite
        else -> Icons.Default.Category
    }
}

@Composable
fun CategoryCard(
    category: Category,
    isExpanded: Boolean,
    subCount: Int,
    onExpandClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMergeClick: () -> Unit,
    onAddSubcategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val color = parseHex(category.color)

    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandClick() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color icon circle
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconVector(category.icon),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (category.isSystem) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "نظامي",
                            modifier = Modifier.size(14.dp),
                            tint = TextGray
                        )
                    }
                }
                if (subCount > 0) {
                    Text(
                        text = "$subCount فئة فرعية",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }
            }

            // Expand arrow
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TextGray
            )

            // More options
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = TextGray, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("✏️ تعديل") },
                        onClick = { showMenu = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("🔗 دمج مع فئة أخرى") },
                        onClick = { showMenu = false; onMergeClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("إضافة فئة فرعية") },
                        leadingIcon = { Icon(Icons.Default.Add, null) },
                        onClick = { showMenu = false; onAddSubcategory() }
                    )
                    if (!category.isSystem) {
                        DropdownMenuItem(
                            text = { Text("حذف الفئة", color = ExpenseRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = ExpenseRed) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubcategoryItem(
    subcategory: Category,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = parseHex(subcategory.color)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowForwardIos, null, tint = color, modifier = Modifier.size(14.dp))
        }

        Text(
            text = subcategory.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
        )

        if (!subcategory.isSystem) {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, null, tint = TextGray, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun CategoryRowSkeleton(modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Circle outline
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shimmerEffect(CircleShape)
            )
            // Title and subcategories count outline
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(16.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(10.dp)
                        .shimmerEffect(RoundedCornerShape(4.dp))
                )
            }
            // Arrow indicator outline
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .shimmerEffect(CircleShape)
            )
        }
    }
}
