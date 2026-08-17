package com.qdash

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.qdash.data.local.AppDatabase
import com.qdash.data.local.entities.CategoryEntity
import com.qdash.data.repository.CategoryRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CategoryMoveHierarchyTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: CategoryRepositoryImpl

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = CategoryRepositoryImpl(
            database = db,
            categoryDao = db.categoryDao(),
            transactionDao = db.transactionDao(),
            budgetGoalDao = db.budgetGoalDao()
        )
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testMoveCategory_fromRootToSubcategory_updatesParentIdAndReassignsNestedSubcategories() = runBlocking {
        // Given: Root Category 1 (Food) with Subcategory 11 (FastFood)
        val foodId = db.categoryDao().insertCategory(
            CategoryEntity(id = 1L, name = "طعام ومشروبات", type = "EXPENSE", icon = "restaurant", color = "#6C63FF", parentId = null)
        )
        val fastFoodId = db.categoryDao().insertCategory(
            CategoryEntity(id = 11L, name = "وجبات سريعة", type = "EXPENSE", icon = "fastfood", color = "#6C63FF", parentId = foodId)
        )

        // Given: Root Category 2 (Living Expenses)
        val livingId = db.categoryDao().insertCategory(
            CategoryEntity(id = 2L, name = "مصاريف المعيشة", type = "EXPENSE", icon = "home", color = "#22C55E", parentId = null)
        )

        // When: Moving "Food" under "Living Expenses"
        repository.moveCategory(categoryId = foodId, newParentId = livingId)

        // Then: "Food" is now a subcategory of "Living Expenses"
        val updatedFood = db.categoryDao().getCategoryById(foodId)
        assertNotNull(updatedFood)
        assertEquals(livingId, updatedFood?.parentId)

        // And: "FastFood" which was under "Food" is now reassigned to "Living Expenses"
        val updatedFastFood = db.categoryDao().getCategoryById(fastFoodId)
        assertNotNull(updatedFastFood)
        assertEquals(livingId, updatedFastFood?.parentId)

        // And: Subcategories of "Living Expenses" contain both
        val livingSubs = db.categoryDao().getSubcategories(livingId).first()
        assertEquals(2, livingSubs.size)
        assertTrue(livingSubs.any { it.id == foodId })
        assertTrue(livingSubs.any { it.id == fastFoodId })
    }

    @Test
    fun testMoveCategory_fromSubcategoryToRoot_promotesToStandaloneRootCategory() = runBlocking {
        // Given: Parent Category (Transport) and Subcategory (Taxi)
        val transportId = db.categoryDao().insertCategory(
            CategoryEntity(id = 3L, name = "مواصلات", type = "EXPENSE", icon = "directions_car", color = "#3B82F6", parentId = null)
        )
        val taxiId = db.categoryDao().insertCategory(
            CategoryEntity(id = 31L, name = "تاكسي", type = "EXPENSE", icon = "local_taxi", color = "#3B82F6", parentId = transportId)
        )

        // When: Promoting "Taxi" to root category (newParentId = null)
        repository.moveCategory(categoryId = taxiId, newParentId = null)

        // Then: "Taxi" parentId becomes null
        val updatedTaxi = db.categoryDao().getCategoryById(taxiId)
        assertNotNull(updatedTaxi)
        assertNull(updatedTaxi?.parentId)

        // And: Root categories list contains Taxi
        val rootCategories = db.categoryDao().getRootCategories().first()
        assertTrue(rootCategories.any { it.id == taxiId })
    }

    @Test
    fun testMoveCategory_fromOneParentToAnotherParent() = runBlocking {
        // Given: Parent A (Shopping), Parent B (Entertainment), and Subcategory (Games)
        val shoppingId = db.categoryDao().insertCategory(
            CategoryEntity(id = 4L, name = "تسوق", type = "EXPENSE", icon = "shopping_bag", color = "#EC4899", parentId = null)
        )
        val entertainmentId = db.categoryDao().insertCategory(
            CategoryEntity(id = 5L, name = "ترفيه", type = "EXPENSE", icon = "sports_esports", color = "#8B5CF6", parentId = null)
        )
        val gamesId = db.categoryDao().insertCategory(
            CategoryEntity(id = 41L, name = "ألعاب", type = "EXPENSE", icon = "sports_esports", color = "#EC4899", parentId = shoppingId)
        )

        // When: Moving "Games" from Shopping to Entertainment
        repository.moveCategory(categoryId = gamesId, newParentId = entertainmentId)

        // Then: "Games" is now under Entertainment
        val updatedGames = db.categoryDao().getCategoryById(gamesId)
        assertNotNull(updatedGames)
        assertEquals(entertainmentId, updatedGames?.parentId)

        // Shopping has 0 subcategories, Entertainment has 1
        val shoppingSubs = db.categoryDao().getSubcategories(shoppingId).first()
        val entertainmentSubs = db.categoryDao().getSubcategories(entertainmentId).first()
        assertTrue(shoppingSubs.isEmpty())
        assertEquals(1, entertainmentSubs.size)
        assertEquals(gamesId, entertainmentSubs.first().id)
    }
}
