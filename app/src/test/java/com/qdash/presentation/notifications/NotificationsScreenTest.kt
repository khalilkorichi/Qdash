package com.qdash.presentation.notifications

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.qdash.domain.model.AppNotification
import com.qdash.domain.model.NotificationType
import com.qdash.domain.repository.NotificationRepository
import com.qdash.presentation.navigation.Screen
import com.qdash.ui.theme.KdachTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h850dp")
class NotificationsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class FakeNotificationRepository(
        initial: List<AppNotification>
    ) : NotificationRepository {
        val state = MutableStateFlow(initial)
        var markAsReadCalls = 0
        var markAllReadCalls = 0
        var clearAllCalls = 0

        override fun getAllNotifications(): Flow<List<AppNotification>> = state

        override fun getUnreadNotifications(): Flow<List<AppNotification>> =
            state.map { list -> list.filter { !it.isRead } }

        override fun getUnreadCount(): Flow<Int> =
            state.map { list -> list.count { !it.isRead } }

        override fun getNotificationsByType(type: NotificationType): Flow<List<AppNotification>> =
            state.map { list -> list.filter { it.type == type } }

        override suspend fun insertNotification(notification: AppNotification): Long {
            state.value = listOf(notification) + state.value.filterNot { it.id == notification.id }
            return notification.id
        }

        override suspend fun markAsRead(id: Long) {
            markAsReadCalls++
            state.value = state.value.map { if (it.id == id) it.copy(isRead = true) else it }
        }

        override suspend fun markAllAsRead() {
            markAllReadCalls++
            state.value = state.value.map { it.copy(isRead = true) }
        }

        override suspend fun deleteNotification(notification: AppNotification) {
            state.value = state.value.filterNot { it.id == notification.id }
        }

        override suspend fun clearAll() {
            clearAllCalls++
            state.value = emptyList()
        }

        override suspend fun cleanupOldNotifications(keepMax: Int) {}
    }

    private fun sampleNotifications(): List<AppNotification> {
        val now = System.currentTimeMillis()
        return listOf(
            AppNotification(
                id = 1L,
                title = "تحديث جديد متوفر! 🎉",
                message = "إصدار جديد من التطبيق (1.0.0.115) متوفر الآن للتحميل.",
                type = NotificationType.APP_UPDATE,
                isRead = false,
                timestamp = now - 15 * 60_000L,
                deepLinkRoute = Screen.Updates.route
            ),
            AppNotification(
                id = 2L,
                title = "مراجعة مسائية 🌙",
                message = "هل انتهى يومك المالي؟ سجل معاملات اليوم الآن لتستمر في تتبع ميزانيتك باحترافية.",
                type = NotificationType.SMART_REMINDER,
                isRead = false,
                timestamp = now - 2 * 3_600_000L,
                deepLinkRoute = "home"
            ),
            AppNotification(
                id = 3L,
                title = "تجاوز الميزانية! ⚠️",
                message = "لقد وصلت إلى 95% من ميزانية المطاعم والمقاهي لهذا الشهر.",
                type = NotificationType.BUDGET_ALERT,
                isRead = false,
                timestamp = now - 26 * 3_600_000L,
                deepLinkRoute = Screen.BudgetGoals.route
            ),
            AppNotification(
                id = 4L,
                title = "تذكير صباحي ☀️",
                message = "يوم مالي منظم يبدأ من الصباح! سجل أي مصاريف أو فواتير صباحية لتظل ميزانيتك دقيقة.",
                type = NotificationType.SMART_REMINDER,
                isRead = true,
                timestamp = now - 30 * 3_600_000L,
                deepLinkRoute = Screen.AddTransaction.createRoute("EXPENSE")
            )
        )
    }

    private fun waitForNotificationsLoaded() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("notifications_list").assertExists()
                true
            }.getOrDefault(false)
        }
        composeRule.mainClock.advanceTimeBy(600)
        composeRule.waitForIdle()
    }

    @Test
    fun headerActionCirclesDoNotOverlapAndMaintainExact36dpSizes() {
        val repo = FakeNotificationRepository(sampleNotifications())
        val vm = NotificationsViewModel(repo)

        composeRule.setContent {
            KdachTheme(darkTheme = false) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.size(width = 400.dp, height = 850.dp)) {
                        NotificationsScreen(
                            viewModel = vm,
                            onNavigateTo = {},
                            onBack = {}
                        )
                    }
                }
            }
        }

        waitForNotificationsLoaded()

        val markAllBounds = composeRule.onNodeWithTag("mark_all_read_button").getBoundsInRoot()
        val clearAllBounds = composeRule.onNodeWithTag("clear_all_button").getBoundsInRoot()

        assertTrue(
            "Expected mark_all_read_button to be 36dp circle, got ${markAllBounds.width}x${markAllBounds.height}",
            abs(markAllBounds.width.value - 36f) <= 0.5f && abs(markAllBounds.height.value - 36f) <= 0.5f
        )
        assertTrue(
            "Expected clear_all_button to be 36dp circle, got ${clearAllBounds.width}x${clearAllBounds.height}",
            abs(clearAllBounds.width.value - 36f) <= 0.5f && abs(clearAllBounds.height.value - 36f) <= 0.5f
        )

        // In RTL, markAll (first in Row) sits to the right of clearAll (second in Row)
        val horizontalGapDp = markAllBounds.left.value - clearAllBounds.right.value
        assertTrue(
            "Header action circles must not overlap; expected >= 7.5dp gap, got ${horizontalGapDp}dp",
            horizontalGapDp >= 7.5f
        )

        // Verify card circle action buttons also do not overlap and stay 34dp
        val cardMarkReadBounds = composeRule.onNodeWithTag("mark_read_1").getBoundsInRoot()
        val cardDeleteBounds = composeRule.onNodeWithTag("delete_notification_1").getBoundsInRoot()
        assertTrue(
            "Card mark_read button should be 34dp, got ${cardMarkReadBounds.width}x${cardMarkReadBounds.height}",
            abs(cardMarkReadBounds.width.value - 34f) <= 0.5f && abs(cardMarkReadBounds.height.value - 34f) <= 0.5f
        )
        assertTrue(
            "Card delete button should be 34dp, got ${cardDeleteBounds.width}x${cardDeleteBounds.height}",
            abs(cardDeleteBounds.width.value - 34f) <= 0.5f && abs(cardDeleteBounds.height.value - 34f) <= 0.5f
        )
        val cardButtonsGap = cardMarkReadBounds.left.value - cardDeleteBounds.right.value
        assertTrue(
            "Card circle buttons must not overlap; expected >= 7.5dp gap, got ${cardButtonsGap}dp",
            cardButtonsGap >= 7.5f
        )
    }

    @Test
    fun contextualActionButtonsNavigateAndMarkReadSeparatelyFromMarkReadAndDeleteButtons() {
        val repo = FakeNotificationRepository(sampleNotifications())
        val vm = NotificationsViewModel(repo)
        val navigatedRoutes = mutableListOf<String>()

        composeRule.setContent {
            KdachTheme(darkTheme = false) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NotificationsScreen(
                            viewModel = vm,
                            onNavigateTo = { navigatedRoutes.add(it) },
                            onBack = {}
                        )
                    }
                }
            }
        }

        waitForNotificationsLoaded()

        // 1. Update action button should display "تحديث الآن" and navigate to Updates route
        composeRule.onNodeWithText("تحديث الآن").assertIsDisplayed()
        composeRule.onNodeWithTag("notification_action_1").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(Screen.Updates.route), navigatedRoutes)
        assertEquals(1, repo.markAsReadCalls)

        // 2. Smart reminder action button should display "تسجيل معاملة" and navigate to AddTransaction
        composeRule.onNodeWithTag("notifications_list").performScrollToKey(2L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notification_action_2").performClick()
        composeRule.waitForIdle()
        assertEquals(
            listOf(Screen.Updates.route, Screen.AddTransaction.createRoute("EXPENSE")),
            navigatedRoutes
        )

        // 3. Dedicated mark-as-read button marks read WITHOUT triggering navigation
        composeRule.onNodeWithTag("notifications_list").performScrollToKey(3L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("mark_read_3").performClick()
        composeRule.waitForIdle()
        assertEquals(2, navigatedRoutes.size)
        composeRule.waitUntil(timeoutMillis = 3_000) {
            repo.state.value.first { it.id == 3L }.isRead
        }

        // 4. Dedicated delete button deletes WITHOUT triggering navigation and shows undo snackbar
        composeRule.onNodeWithTag("notifications_list").performScrollToNode(hasTestTag("delete_notification_4"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("delete_notification_4").performClick()
        composeRule.waitForIdle()
        assertEquals(2, navigatedRoutes.size)
        composeRule.waitUntil(timeoutMillis = 3_000) {
            repo.state.value.none { it.id == 4L }
        }
        composeRule.onNodeWithText("تم حذف الإشعار").assertIsDisplayed()
    }

    @Test
    fun rendersDarkModeAndUpdateFilterCleanly() {
        val repo = FakeNotificationRepository(sampleNotifications())
        val vm = NotificationsViewModel(repo)

        composeRule.setContent {
            KdachTheme(darkTheme = true) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.size(width = 400.dp, height = 850.dp)) {
                        NotificationsScreen(
                            viewModel = vm,
                            onNavigateTo = {},
                            onBack = {}
                        )
                    }
                }
            }
        }

        waitForNotificationsLoaded()

        // Tap "التحديثات" filter chip and verify update card remains visible while reminder disappears
        composeRule.onNodeWithTag("filter_chip_التحديثات").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithTag("notification_item_2").fetchSemanticsNodes().isEmpty()
        }
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notification_item_1").assertIsDisplayed()
        composeRule.onNodeWithTag("notification_item_2").assertDoesNotExist()

        // Tap an empty category ("الديون"), verify empty state + reset button restores all items
        composeRule.onNodeWithTag("notification_filter_row").performScrollToKey("الديون")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("filter_chip_الديون").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithTag("notifications_empty").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notifications_empty").assertIsDisplayed()

        composeRule.onNodeWithTag("reset_filter_button").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithTag("notification_item_1").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notification_item_1").assertIsDisplayed()
        composeRule.onNodeWithTag("notification_item_2").assertIsDisplayed()
    }
}
