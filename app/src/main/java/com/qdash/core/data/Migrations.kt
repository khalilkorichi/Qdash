package com.qdash.core.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database migrations for kdach_database.
 *
 * NOTE: Versions 1–3 are legacy/local debug states and are not supported upgrade sources.
 * The oldest supported upgrade source version is version 4.
 */

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `notifications` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `title` TEXT NOT NULL, 
                `message` TEXT NOT NULL, 
                `type` TEXT NOT NULL, 
                `isRead` INTEGER NOT NULL DEFAULT 0, 
                `timestamp` INTEGER NOT NULL, 
                `deepLinkRoute` TEXT, 
                `relatedEntityId` INTEGER
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `financial_plans` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `title` TEXT NOT NULL, 
                `type` TEXT NOT NULL, 
                `targetAmount` REAL NOT NULL, 
                `currentAmount` REAL NOT NULL DEFAULT 0.0, 
                `linkedAccountIds` TEXT NOT NULL, 
                `linkedCategoryIds` TEXT NOT NULL, 
                `startDate` INTEGER NOT NULL, 
                `endDate` INTEGER, 
                `status` TEXT NOT NULL DEFAULT 'ACTIVE', 
                `notes` TEXT, 
                `color` TEXT NOT NULL DEFAULT '#6C63FF', 
                `icon` TEXT NOT NULL DEFAULT 'flag', 
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("ALTER TABLE `accounts` ADD COLUMN `isDefault` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `accounts` ADD COLUMN `isArchived` INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE `categories` ADD COLUMN `isSystem` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `categories` ADD COLUMN `parentId` INTEGER")
        db.execSQL("ALTER TABLE `categories` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_date` ON `transactions` (`date`)")
        
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `savings_contributions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `savingGoalId` INTEGER NOT NULL, 
                `accountId` INTEGER NOT NULL, 
                `amount` REAL NOT NULL, 
                `type` TEXT NOT NULL, 
                `note` TEXT, 
                `date` INTEGER NOT NULL, 
                `linkedTransactionId` INTEGER, 
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `debts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `title` TEXT NOT NULL, 
                `creditorName` TEXT NOT NULL, 
                `totalAmount` REAL NOT NULL, 
                `remainingAmount` REAL NOT NULL, 
                `interestRate` REAL, 
                `dueDate` INTEGER, 
                `minimumPayment` REAL NOT NULL, 
                `recommendedPayment` REAL, 
                `paymentFrequency` TEXT NOT NULL, 
                `linkedAccountId` INTEGER, 
                `priority` INTEGER NOT NULL, 
                `notes` TEXT, 
                `color` TEXT NOT NULL, 
                `icon` TEXT NOT NULL, 
                `createdAt` INTEGER NOT NULL, 
                `isClosed` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `debt_payments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `debtId` INTEGER NOT NULL, 
                `accountId` INTEGER NOT NULL, 
                `amount` REAL NOT NULL, 
                `paymentDate` INTEGER NOT NULL, 
                `paymentType` TEXT NOT NULL, 
                `note` TEXT, 
                `linkedTransactionId` INTEGER, 
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `transfers` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `fromAccountId` INTEGER NOT NULL, 
                `toAccountId` INTEGER NOT NULL, 
                `amount` REAL NOT NULL, 
                `feeAmount` REAL, 
                `note` TEXT, 
                `date` INTEGER NOT NULL, 
                `referenceId` TEXT NOT NULL, 
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_parentId` ON `categories` (`parentId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_income_sources_accountId` ON `income_sources` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_saving_goals_accountId` ON `saving_goals` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscriptions_accountId` ON `subscriptions` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscriptions_categoryId` ON `subscriptions` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_contributions_savingGoalId` ON `savings_contributions` (`savingGoalId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_contributions_accountId` ON `savings_contributions` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_contributions_date` ON `savings_contributions` (`date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_linkedAccountId` ON `debts` (`linkedAccountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_debtId` ON `debt_payments` (`debtId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_accountId` ON `debt_payments` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transfers_fromAccountId` ON `transfers` (`fromAccountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transfers_toAccountId` ON `transfers` (`toAccountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transfers_date` ON `transfers` (`date`)")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `daily_financial_aggregates` (
                `localDateTimestamp` INTEGER NOT NULL, 
                `totalExpense` REAL NOT NULL, 
                `totalIncome` REAL NOT NULL, 
                `transactionCount` INTEGER NOT NULL, 
                `netCashflow` REAL NOT NULL, 
                `activityScore` REAL NOT NULL, 
                PRIMARY KEY(`localDateTimestamp`)
            )
        """.trimIndent())
        
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_financial_aggregates_localDateTimestamp` ON `daily_financial_aggregates` (`localDateTimestamp`)")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `transaction_templates` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `name` TEXT NOT NULL, 
                `amount` REAL NOT NULL, 
                `transactionType` TEXT NOT NULL, 
                `accountId` INTEGER NOT NULL, 
                `targetAccountId` INTEGER, 
                `categoryId` INTEGER, 
                `subcategoryId` INTEGER, 
                `notes` TEXT, 
                `iconEmoji` TEXT, 
                `colorHex` TEXT, 
                `isPinned` INTEGER NOT NULL DEFAULT 0, 
                `usageCount` INTEGER NOT NULL DEFAULT 0, 
                `lastUsedAt` INTEGER, 
                `createdAt` INTEGER NOT NULL, 
                `updatedAt` INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_templates_isPinned` ON `transaction_templates` (`isPinned`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_templates_usageCount` ON `transaction_templates` (`usageCount`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_templates_lastUsedAt` ON `transaction_templates` (`lastUsedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_templates_transactionType` ON `transaction_templates` (`transactionType`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_templates_categoryId` ON `transaction_templates` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_templates_accountId` ON `transaction_templates` (`accountId`)")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Schema v8 and v9 are identical — only version bumped.
        // No structural changes required. Room still needs this
        // migration path so it does NOT fall back to destructive.
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_budget_goals_linkedCategoryId ON budget_goals (linkedCategoryId)")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_chat_messages_sessionTitle` ON `ai_chat_messages` (`sessionTitle`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_paymentDate` ON `debt_payments` (`paymentDate`)")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN tags TEXT")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_user_category_mappings_normalizedText` ON `user_category_mappings` (`normalizedText`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_category_rules_keyword` ON `category_rules` (`keyword`)")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `postal_profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profileName` TEXT NOT NULL, `firstName` TEXT NOT NULL, `lastName` TEXT NOT NULL, `fullName` TEXT NOT NULL, `accountNumber` TEXT NOT NULL, `accountKey` TEXT NOT NULL, `phone` TEXT, `address` TEXT, `city` TEXT, `defaultRole` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `kind` TEXT NOT NULL DEFAULT 'INCOME'")
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `transferId` TEXT")
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `isDebit` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("UPDATE `transactions` SET `kind` = `type`")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `accounts` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `transactions_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` REAL NOT NULL, `type` TEXT NOT NULL, `categoryId` INTEGER, `accountId` INTEGER NOT NULL, `toAccountId` INTEGER, `note` TEXT, `date` INTEGER NOT NULL, `isRecurring` INTEGER NOT NULL, `recurringPeriod` TEXT, `attachmentPath` TEXT, `tags` TEXT, `suggestedCategoryId` INTEGER, `suggestionSource` TEXT, `confidenceScore` REAL, `userAcceptedSuggestion` INTEGER, `kind` TEXT NOT NULL, `transferId` TEXT, `isDebit` INTEGER NOT NULL)")
        db.execSQL("INSERT INTO `transactions_new` (`id`, `amount`, `type`, `categoryId`, `accountId`, `toAccountId`, `note`, `date`, `isRecurring`, `recurringPeriod`, `attachmentPath`, `tags`, `suggestedCategoryId`, `suggestionSource`, `confidenceScore`, `userAcceptedSuggestion`, `kind`, `transferId`, `isDebit`) SELECT `id`, `amount`, `type`, `categoryId`, `accountId`, `toAccountId`, `note`, `date`, `isRecurring`, `recurringPeriod`, `attachmentPath`, `tags`, `suggestedCategoryId`, `suggestionSource`, `confidenceScore`, `userAcceptedSuggestion`, `kind`, `transferId`, `isDebit` FROM `transactions`")
        db.execSQL("DROP TABLE `transactions`")
        db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_date` ON `transactions` (`date`)")
        db.execSQL("UPDATE `transactions` SET `categoryId` = NULL WHERE `type` = 'TRANSFER'")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `subscriptions` ADD COLUMN `isAutoShiftableBySalary` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `salary_delays` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `salaryId` INTEGER NOT NULL,
                `delayDays` INTEGER NOT NULL,
                `originalDate` INTEGER NOT NULL,
                `newDate` INTEGER NOT NULL,
                `severityScore` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_salary_delays_salaryId` ON `salary_delays` (`salaryId`)")
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `salary_distributions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `salaryId` INTEGER NOT NULL,
                `isEnabled` INTEGER NOT NULL DEFAULT 0,
                `needsPercentage` INTEGER NOT NULL DEFAULT 50,
                `wantsPercentage` INTEGER NOT NULL DEFAULT 30,
                `savingsPercentage` INTEGER NOT NULL DEFAULT 20,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_salary_distributions_salaryId` ON `salary_distributions` (`salaryId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `salary_envelopes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `distributionId` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `percentage` INTEGER NOT NULL,
                `allocatedAmount` REAL NOT NULL,
                `spentAmount` REAL NOT NULL DEFAULT 0.0,
                `linkedCategoryIds` TEXT NOT NULL DEFAULT '',
                `color` TEXT NOT NULL,
                `icon` TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_salary_envelopes_distributionId` ON `salary_envelopes` (`distributionId`)")
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `salary_envelopes` ADD COLUMN `linkedAccountId` INTEGER")
    }
}

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `debts` ADD COLUMN `debtType` TEXT NOT NULL DEFAULT 'INSTALLMENT'")
    }
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `user_profiles` (
                `id` INTEGER NOT NULL, 
                `name` TEXT NOT NULL, 
                `email` TEXT, 
                `birthDate` TEXT, 
                `avatarUrl` TEXT, 
                `isGoogleLinked` INTEGER NOT NULL, 
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
    }
}

val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_isArchived_sortOrder_createdAt` ON `accounts` (`isArchived`, `sortOrder`, `createdAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_timestamp` ON `notifications` (`timestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_isRead_timestamp` ON `notifications` (`isRead`, `timestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_type_timestamp` ON `notifications` (`type`, `timestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_postal_profiles_isFavorite_updatedAt` ON `postal_profiles` (`isFavorite`, `updatedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_postal_profiles_defaultRole_isFavorite_updatedAt` ON `postal_profiles` (`defaultRole`, `isFavorite`, `updatedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_plans_createdAt` ON `financial_plans` (`createdAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_plans_status_createdAt` ON `financial_plans` (`status`, `createdAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_plans_type_createdAt` ON `financial_plans` (`type`, `createdAt`)")
    }
}

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_profiles_name` ON `user_profiles` (`name`)")
    }
}

/**
 * All migrations in order, for passing to Room's addMigrations().
 */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
    MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11,
    MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15,
    MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20,
    MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24
)


