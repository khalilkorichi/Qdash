package com.qdash.core.data

import com.qdash.data.local.AppDatabase
import com.qdash.data.local.entities.AccountEntity
import com.qdash.data.local.entities.CategoryEntity
import kotlinx.coroutines.flow.first

/**
 * Database seeder — prepopulates default categories and accounts on first launch.
 * Extracted from AppContainerImpl — exact same logic, zero changes.
 */
object DatabaseSeeder {

    suspend fun prepopulateSystemDefaults(database: AppDatabase) {
        try {
            val categoryDao = database.categoryDao()
            val accountDao = database.accountDao()

            val expectedIncomeCategories = listOf(
                CategoryEntity(id = 11, name = "راتب", type = "INCOME", icon = "work", color = "#22C55E", isSystem = true, sortOrder = 1),
                CategoryEntity(id = 12, name = "مكافآت وهدايا", type = "INCOME", icon = "redeem", color = "#EF4444", isSystem = true, sortOrder = 2),
                CategoryEntity(id = 13, name = "مبيعات", type = "INCOME", icon = "storefront", color = "#F59E0B", isSystem = true, sortOrder = 3),
                CategoryEntity(id = 14, name = "عمل إضافي", type = "INCOME", icon = "schedule", color = "#3B82F6", isSystem = true, sortOrder = 4),
                CategoryEntity(id = 15, name = "أخرى", type = "INCOME", icon = "monetization_on", color = "#00F2FE", isSystem = true, sortOrder = 5)
            )

            val expectedCategories = listOf(
                // Root expense categories
                CategoryEntity(id = 1, name = "شخصي", type = "EXPENSE", icon = "person", color = "#6C63FF", isSystem = true, sortOrder = 1),
                CategoryEntity(id = 2, name = "عائلي", type = "EXPENSE", icon = "groups", color = "#22C55E", isSystem = true, sortOrder = 2),
                CategoryEntity(id = 3, name = "منزلي", type = "EXPENSE", icon = "home", color = "#EF4444", isSystem = true, sortOrder = 3),
                CategoryEntity(id = 4, name = "طعام", type = "EXPENSE", icon = "restaurant", color = "#F59E0B", isSystem = true, sortOrder = 4),
                CategoryEntity(id = 5, name = "مواصلات", type = "EXPENSE", icon = "directions_car", color = "#3B82F6", isSystem = true, sortOrder = 5),
                CategoryEntity(id = 6, name = "فواتير", type = "EXPENSE", icon = "receipt_long", color = "#EE5F5B", isSystem = true, sortOrder = 6),
                CategoryEntity(id = 7, name = "تسوق", type = "EXPENSE", icon = "shopping_bag", color = "#A770EF", isSystem = true, sortOrder = 7),
                CategoryEntity(id = 8, name = "صحة", type = "EXPENSE", icon = "medical_services", color = "#00F2FE", isSystem = true, sortOrder = 8),
                CategoryEntity(id = 9, name = "تعليم", type = "EXPENSE", icon = "school", color = "#4FACFE", isSystem = true, sortOrder = 9),
                CategoryEntity(id = 10, name = "ترفيه", type = "EXPENSE", icon = "sports_esports", color = "#F35588", isSystem = true, sortOrder = 10),
                CategoryEntity(id = 16, name = "مناسبات وأعياد", type = "EXPENSE", icon = "celebration", color = "#FF5722", isSystem = true, sortOrder = 11),
                CategoryEntity(id = 17, name = "الخدمات البريدية", type = "EXPENSE", icon = "mail", color = "#FFEB3B", isSystem = true, sortOrder = 12),

                // Income categories
                expectedIncomeCategories[0],
                expectedIncomeCategories[1],
                expectedIncomeCategories[2],
                expectedIncomeCategories[3],
                expectedIncomeCategories[4],

                // Subcategories for منزلي (id=3)
                CategoryEntity(name = "إيجار", type = "EXPENSE", icon = "house", color = "#EF4444", isSystem = true, parentId = 3, sortOrder = 1),
                CategoryEntity(name = "كهرباء وغاز", type = "EXPENSE", icon = "bolt", color = "#F59E0B", isSystem = true, parentId = 3, sortOrder = 2),
                CategoryEntity(name = "ماء", type = "EXPENSE", icon = "water_drop", color = "#3B82F6", isSystem = true, parentId = 3, sortOrder = 3),
                CategoryEntity(name = "إنترنت", type = "EXPENSE", icon = "wifi", color = "#6C63FF", isSystem = true, parentId = 3, sortOrder = 4),
                CategoryEntity(name = "أثاث", type = "EXPENSE", icon = "chair", color = "#A770EF", isSystem = true, parentId = 3, sortOrder = 5),
                
                // Subcategories for طعام (id=4)
                CategoryEntity(name = "بقالة", type = "EXPENSE", icon = "shopping_cart", color = "#22C55E", isSystem = true, parentId = 4, sortOrder = 1),
                CategoryEntity(name = "مطاعم", type = "EXPENSE", icon = "restaurant", color = "#F59E0B", isSystem = true, parentId = 4, sortOrder = 2),
                CategoryEntity(name = "قهوة", type = "EXPENSE", icon = "coffee", color = "#8B4513", isSystem = true, parentId = 4, sortOrder = 3),
                
                // Subcategories for مواصلات (id=5)
                CategoryEntity(name = "وقود", type = "EXPENSE", icon = "local_gas_station", color = "#3B82F6", isSystem = true, parentId = 5, sortOrder = 1),
                CategoryEntity(name = "تاكسي", type = "EXPENSE", icon = "local_taxi", color = "#F59E0B", isSystem = true, parentId = 5, sortOrder = 2),
                CategoryEntity(name = "حافلة", type = "EXPENSE", icon = "directions_bus", color = "#22C55E", isSystem = true, parentId = 5, sortOrder = 3),
                
                // Subcategories for شخصي (id=1)
                CategoryEntity(name = "ملابس", type = "EXPENSE", icon = "checkroom", color = "#6C63FF", isSystem = true, parentId = 1, sortOrder = 1),
                CategoryEntity(name = "عناية شخصية", type = "EXPENSE", icon = "spa", color = "#F35588", isSystem = true, parentId = 1, sortOrder = 2),
                
                // Subcategories for ترفيه (id=10)
                CategoryEntity(name = "ألعاب", type = "EXPENSE", icon = "sports_esports", color = "#F35588", isSystem = true, parentId = 10, sortOrder = 1),
                CategoryEntity(name = "بث مباشر", type = "EXPENSE", icon = "live_tv", color = "#EF4444", isSystem = true, parentId = 10, sortOrder = 2),
                CategoryEntity(name = "فعاليات", type = "EXPENSE", icon = "event", color = "#A770EF", isSystem = true, parentId = 10, sortOrder = 3),

                // Subcategories for مناسبات وأعياد (id=16)
                CategoryEntity(name = "عيد الأضحى", type = "EXPENSE", icon = "pets", color = "#FF5722", isSystem = true, parentId = 16, sortOrder = 1),
                CategoryEntity(name = "رمضان", type = "EXPENSE", icon = "nights_stay", color = "#9C27B0", isSystem = true, parentId = 16, sortOrder = 2),
                CategoryEntity(name = "الدخول المدرسي", type = "EXPENSE", icon = "backpack", color = "#4CAF50", isSystem = true, parentId = 16, sortOrder = 3),
                CategoryEntity(name = "أعراس وحفلات", type = "EXPENSE", icon = "cake", color = "#E91E63", isSystem = true, parentId = 16, sortOrder = 4),

                // Subcategories for الخدمات البريدية (id=17)
                CategoryEntity(name = "مستحقات البريد (CCP)", type = "EXPENSE", icon = "account_balance", color = "#FFEB3B", isSystem = true, parentId = 17, sortOrder = 1),
                CategoryEntity(name = "البطاقة الذهبية", type = "EXPENSE", icon = "credit_card", color = "#FF9800", isSystem = true, parentId = 17, sortOrder = 2)
            )

            val existingCategories = categoryDao.getAllCategories().first()
            for (expected in expectedCategories) {
                val exists = existingCategories.any { 
                    it.name == expected.name && 
                    it.type == expected.type && 
                    it.parentId == expected.parentId 
                }
                if (!exists) {
                    categoryDao.insertCategory(expected)
                }
            }

            val existingAccounts = accountDao.getAllAccounts().first()
            if (existingAccounts.isEmpty()) {
                val defaultAccounts = listOf(
                    AccountEntity(name = "بريدي موب", type = "BARIDIMOB", balance = 45000.0, color = "#8A2387", icon = "phonelink_ring", isDefault = true),
                    AccountEntity(name = "نقدي / كاش", type = "CASH", balance = 5000.0, color = "#11998e", icon = "payments", isDefault = false),
                    AccountEntity(name = "حساب التوفير", type = "SAVINGS", balance = 15000.0, color = "#4facfe", icon = "savings", isDefault = false)
                )
                for (account in defaultAccounts) {
                    accountDao.insertAccount(account)
                }
            }

            // Database Mock Seeding removed as requested by the user for a clean starting experience
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
