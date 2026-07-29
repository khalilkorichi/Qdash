package com.qdash.presentation.currency

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.presentation.currency.tabs.ConverterTab
import com.qdash.presentation.currency.tabs.OfficialMarketTab
import com.qdash.presentation.currency.tabs.ParallelMarketTab
import com.qdash.ui.designsystem.components.currency.CurrencyTabRow
import com.qdash.ui.designsystem.tokens.SpacingTokens
import kotlinx.coroutines.launch

/**
 * Main currency exchange screen.
 *
 * Tab layout (RTL — index 0 = rightmost):
 *   0 → السوق الموازي  (Coming Soon)
 *   1 → المحول          (Converter)
 *   2 → السوق الرسمي   (Official Market)
 *
 * Navigation: back button via [onBackClick].
 * Not part of the bottom navigation bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyExchangeScreen(
    viewModel: CurrencyExchangeViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Start on the converter tab (index 1 = center)
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    val tabs = listOf("السوق الموازي", "المحول", "السوق الرسمي")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "أسعار الصرف",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // ── Tab Row ───────────────────────────────────────────────────────
            CurrencyTabRow(
                tabs = tabs,
                selected = pagerState.currentPage,
                onTabSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                modifier = Modifier.padding(
                    horizontal = SpacingTokens.Lg,
                    vertical = SpacingTokens.Sm
                )
            )

            // ── Pager ─────────────────────────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { page -> page }
            ) { page ->
                when (page) {
                    0 -> ParallelMarketTab(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                    1 -> ConverterTab(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                    2 -> OfficialMarketTab(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
