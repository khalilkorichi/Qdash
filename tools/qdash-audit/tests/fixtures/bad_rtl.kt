// fixtures/bad_rtl.kt
// BAD: hardcoded LayoutDirection.Ltr in Composable — triggers RTL-001 and RTL-002
package com.qdash.presentation.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.Text

@Composable
fun BadTransactionScreen() {
    // BAD: hardcoded LTR direction via direct assignment
    Box(modifier = androidx.compose.ui.Modifier.then(
        androidx.compose.ui.Modifier
    )) {
        val dir = LayoutDirection.Ltr
        @Suppress("UNUSED_EXPRESSION")
        val layoutDirection = LayoutDirection.Ltr  // RTL-001: hardcoded LTR

            // BAD: hardcoded English string instead of stringResource
            Text("Transactions")
            Text("No transactions found")
        }
    }
}
