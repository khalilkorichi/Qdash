package com.qdash.presentation.budgetgoals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.qdash.ui.designsystem.components.shimmerEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetGoalDetailsSkeleton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.size(width = 140.dp, height = 24.dp).shimmerEffect(RoundedCornerShape(6.dp)))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Stat Dial / Display Card Skeleton
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(width = 120.dp, height = 16.dp).shimmerEffect(RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.size(width = 180.dp, height = 36.dp).shimmerEffect(RoundedCornerShape(8.dp)))

                    Spacer(modifier = Modifier.height(24.dp))

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(width = 60.dp, height = 12.dp).shimmerEffect(RoundedCornerShape(4.dp)))
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(modifier = Modifier.size(width = 80.dp, height = 16.dp).shimmerEffect(RoundedCornerShape(4.dp)))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(width = 60.dp, height = 12.dp).shimmerEffect(RoundedCornerShape(4.dp)))
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(modifier = Modifier.size(width = 80.dp, height = 16.dp).shimmerEffect(RoundedCornerShape(4.dp)))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(width = 60.dp, height = 12.dp).shimmerEffect(RoundedCornerShape(4.dp)))
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(modifier = Modifier.size(width = 80.dp, height = 16.dp).shimmerEffect(RoundedCornerShape(4.dp)))
                        }
                    }
                }
            }

            // Coach card suggestion skeleton
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .shimmerEffect()
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.size(width = 160.dp, height = 16.dp).shimmerEffect(RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(12.dp).shimmerEffect(RoundedCornerShape(3.dp)))
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.size(width = 200.dp, height = 12.dp).shimmerEffect(RoundedCornerShape(3.dp)))
                    }
                }
            }

            // Period, category, threshold metadata Card Skeleton
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.size(width = 100.dp, height = 18.dp).shimmerEffect(RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.size(width = 80.dp, height = 14.dp).shimmerEffect(RoundedCornerShape(3.dp)))
                        Box(modifier = Modifier.size(width = 140.dp, height = 14.dp).shimmerEffect(RoundedCornerShape(3.dp)))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.size(width = 80.dp, height = 14.dp).shimmerEffect(RoundedCornerShape(3.dp)))
                        Box(modifier = Modifier.size(width = 100.dp, height = 14.dp).shimmerEffect(RoundedCornerShape(3.dp)))
                    }
                }
            }
        }
    }
}
