package com.qdash.data.local.seeder

import com.qdash.data.local.AppDatabase
import com.qdash.data.local.dao.CategoryDao
import com.qdash.data.local.entities.CategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Standalone seeder module constructing primary parent categories and nested subcategories
 * specifically tailored to Algerian daily financial habits.
 * Runs on Dispatchers.IO and avoids overwriting user-created custom entries.
 */
object AlgerianCategoriesSeeder {

    suspend fun seedCategories(database: AppDatabase) = withContext(Dispatchers.IO) {
        try {
            val categoryDao = database.categoryDao()

            val incomeCategories = listOf(
                CategoryEntity(id = 11, name = "راتب", type = "INCOME", icon = "work", color = "#22C55E", isSystem = true, sortOrder = 1),
                CategoryEntity(id = 12, name = "مكافآت وهدايا", type = "INCOME", icon = "redeem", color = "#EF4444", isSystem = true, sortOrder = 2),
                CategoryEntity(id = 13, name = "مبيعات", type = "INCOME", icon = "storefront", color = "#F59E0B", isSystem = true, sortOrder = 3),
                CategoryEntity(id = 14, name = "عمل إضافي", type = "INCOME", icon = "schedule", color = "#3B82F6", isSystem = true, sortOrder = 4),
                CategoryEntity(id = 15, name = "أخرى", type = "INCOME", icon = "monetization_on", color = "#00F2FE", isSystem = true, sortOrder = 5)
            )

            val expenseCategories = listOf(
                // Parent categories
                CategoryEntity(id = 1, name = "شخصي", type = "EXPENSE", icon = "person", color = "#6C63FF", isSystem = true, sortOrder = 1),
                CategoryEntity(id = 2, name = "عائلي وكفالة", type = "EXPENSE", icon = "ic_cat_family", color = "#22C55E", isSystem = true, sortOrder = 2),
                CategoryEntity(id = 3, name = "منزلي وبناء", type = "EXPENSE", icon = "home", color = "#EF4444", isSystem = true, sortOrder = 3),
                CategoryEntity(id = 4, name = "طعام ومأكولات", type = "EXPENSE", icon = "restaurant", color = "#F59E0B", isSystem = true, sortOrder = 4),
                CategoryEntity(id = 5, name = "مواصلات وتاكسي", type = "EXPENSE", icon = "ic_cat_transport", color = "#3B82F6", isSystem = true, sortOrder = 5),
                CategoryEntity(id = 6, name = "فواتير وشحن", type = "EXPENSE", icon = "ic_cat_flexy", color = "#EE5F5B", isSystem = true, sortOrder = 6),
                CategoryEntity(id = 7, name = "تسوق وملابس", type = "EXPENSE", icon = "shopping_bag", color = "#A770EF", isSystem = true, sortOrder = 7),
                CategoryEntity(id = 8, name = "صحة وصيدلية", type = "EXPENSE", icon = "ic_cat_health", color = "#00F2FE", isSystem = true, sortOrder = 8),
                CategoryEntity(id = 9, name = "تعليم ودروس", type = "EXPENSE", icon = "ic_cat_education", color = "#4FACFE", isSystem = true, sortOrder = 9),
                CategoryEntity(id = 10, name = "ترفيه وألعاب", type = "EXPENSE", icon = "sports_esports", color = "#F35588", isSystem = true, sortOrder = 10),
                CategoryEntity(id = 16, name = "مناسبات وأعياد", type = "EXPENSE", icon = "celebration", color = "#FF5722", isSystem = true, sortOrder = 11),
                CategoryEntity(id = 17, name = "الخدمات البريدية (CCP)", type = "EXPENSE", icon = "account_balance", color = "#FFEB3B", isSystem = true, sortOrder = 12),

                // Subcategories for منزلي وبناء (id=3)
                CategoryEntity(name = "إيجار", type = "EXPENSE", icon = "house", color = "#EF4444", isSystem = true, parentId = 3, sortOrder = 1),
                CategoryEntity(name = "كهرباء وغاز", type = "EXPENSE", icon = "bolt", color = "#F59E0B", isSystem = true, parentId = 3, sortOrder = 2),
                CategoryEntity(name = "ماء", type = "EXPENSE", icon = "water_drop", color = "#3B82F6", isSystem = true, parentId = 3, sortOrder = 3),
                CategoryEntity(name = "إنترنت و ADSL / 4G Box", type = "EXPENSE", icon = "ic_cat_flexy", color = "#6C63FF", isSystem = true, parentId = 3, sortOrder = 4),
                CategoryEntity(name = "خردوات ودروغري", type = "EXPENSE", icon = "ic_cat_bricolage", color = "#FF9800", isSystem = true, parentId = 3, sortOrder = 5),
                CategoryEntity(name = "خدمات محلية وتصليح", type = "EXPENSE", icon = "build", color = "#795548", isSystem = true, parentId = 3, sortOrder = 6),

                // Subcategories for طعام ومأكولات (id=4)
                CategoryEntity(name = "مارشي وخضار", type = "EXPENSE", icon = "ic_cat_marche", color = "#22C55E", isSystem = true, parentId = 4, sortOrder = 1),
                CategoryEntity(name = "أكل سريع وبلاطو", type = "EXPENSE", icon = "ic_cat_fastfood", color = "#FF5722", isSystem = true, parentId = 4, sortOrder = 2),
                CategoryEntity(name = "بقالة ومواد غذائية", type = "EXPENSE", icon = "shopping_cart", color = "#4CAF50", isSystem = true, parentId = 4, sortOrder = 3),
                CategoryEntity(name = "مطاعم ومقاهي", type = "EXPENSE", icon = "restaurant", color = "#F59E0B", isSystem = true, parentId = 4, sortOrder = 4),

                // Subcategories for مواصلات وتاكسي (id=5)
                CategoryEntity(name = "وقود ونفطال", type = "EXPENSE", icon = "local_gas_station", color = "#3B82F6", isSystem = true, parentId = 5, sortOrder = 1),
                CategoryEntity(name = "تاكسي وتطبيقات (Yassir/Heetch)", type = "EXPENSE", icon = "ic_cat_transport", color = "#F59E0B", isSystem = true, parentId = 5, sortOrder = 2),
                CategoryEntity(name = "حافلة وترامواي", type = "EXPENSE", icon = "directions_bus", color = "#22C55E", isSystem = true, parentId = 5, sortOrder = 3),

                // Subcategories for شخصي (id=1)
                CategoryEntity(name = "ملابس وقش", type = "EXPENSE", icon = "checkroom", color = "#6C63FF", isSystem = true, parentId = 1, sortOrder = 1),
                CategoryEntity(name = "كوسميتيك وعناية", type = "EXPENSE", icon = "spa", color = "#E91E63", isSystem = true, parentId = 1, sortOrder = 2),

                // Subcategories for عائلي وكفالة (id=2)
                CategoryEntity(name = "مصروف عائلي وكفالة", type = "EXPENSE", icon = "ic_cat_family", color = "#8BC34A", isSystem = true, parentId = 2, sortOrder = 1),

                // Subcategories for صحة وصيدلية (id=8)
                CategoryEntity(name = "صيدلية ودواء", type = "EXPENSE", icon = "ic_cat_health", color = "#00BCD4", isSystem = true, parentId = 8, sortOrder = 1),
                CategoryEntity(name = "عيادة وتحاليل", type = "EXPENSE", icon = "medical_services", color = "#00F2FE", isSystem = true, parentId = 8, sortOrder = 2),

                // Subcategories for الخدمات البريدية (id=17)
                CategoryEntity(name = "مستحقات البريد (CCP)", type = "EXPENSE", icon = "account_balance", color = "#FFEB3B", isSystem = true, parentId = 17, sortOrder = 1),
                CategoryEntity(name = "البطاقة الذهبية", type = "EXPENSE", icon = "credit_card", color = "#FF9800", isSystem = true, parentId = 17, sortOrder = 2)
            )

            val allExpected = incomeCategories + expenseCategories

            val existingCategories = categoryDao.getAllCategories().first()
            val existingKeySet = existingCategories
                .map { Triple(it.name, it.type, it.parentId) }
                .toSet()
            val existingIds = existingCategories.map { it.id }.toSet()

            for (expected in allExpected) {
                val existsByName = existingKeySet.contains(
                    Triple(expected.name, expected.type, expected.parentId)
                )
                if (existsByName) continue

                val idConflict = expected.id > 0 && existingIds.contains(expected.id)
                if (idConflict) continue

                categoryDao.insertCategoryIgnoreConflict(expected)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
