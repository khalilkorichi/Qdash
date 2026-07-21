package com.qdash.core.data

import com.qdash.data.local.AppDatabase
import com.qdash.data.local.seeder.AlgerianCategoriesSeeder

/**
 * Database seeder — prepopulates default categories on first launch.
 * Delegates category seeding to AlgerianCategoriesSeeder.
 */
object DatabaseSeeder {

    suspend fun prepopulateSystemDefaults(database: AppDatabase) {
        try {
            AlgerianCategoriesSeeder.seedCategories(database)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
