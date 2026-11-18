package com.qdash.data.local.seeder

import com.qdash.data.local.AppDatabase
import com.qdash.data.local.entities.CategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Standalone seeder module constructing primary parent categories and nested subcategories
 * specifically tailored to Algerian daily financial habits (both EXPENSE and INCOME).
 * Runs on Dispatchers.IO, resolves parent IDs dynamically for safe upgrades on existing
 * installations, and never overwrites user-created custom entries.
 */
object AlgerianCategoriesSeeder {

    private data class SubcategorySeed(
        val parentName: String,
        val type: String,
        val name: String,
        val icon: String,
        val color: String,
        val sortOrder: Int
    )

    suspend fun seedCategories(database: AppDatabase) = withContext(Dispatchers.IO) {
        try {
            val categoryDao = database.categoryDao()

            val incomeParentCategories = listOf(
                CategoryEntity(id = 11, name = "راتب", type = "INCOME", icon = "work", color = "#22C55E", isSystem = true, sortOrder = 1),
                CategoryEntity(id = 12, name = "مكافآت وهدايا", type = "INCOME", icon = "redeem", color = "#EC4899", isSystem = true, sortOrder = 2),
                CategoryEntity(id = 13, name = "مبيعات", type = "INCOME", icon = "storefront", color = "#F59E0B", isSystem = true, sortOrder = 3),
                CategoryEntity(id = 14, name = "عمل إضافي", type = "INCOME", icon = "schedule", color = "#3B82F6", isSystem = true, sortOrder = 4),
                CategoryEntity(name = "منح وتعويضات", type = "INCOME", icon = "account_balance_wallet", color = "#10B981", isSystem = true, sortOrder = 5),
                CategoryEntity(name = "إيجار واستثمار", type = "INCOME", icon = "trending_up", color = "#8B5CF6", isSystem = true, sortOrder = 6),
                CategoryEntity(name = "استرداد سلف وأمانات", type = "INCOME", icon = "sync_alt", color = "#06B6D4", isSystem = true, sortOrder = 7),
                CategoryEntity(id = 15, name = "أخرى", type = "INCOME", icon = "monetization_on", color = "#14B8A6", isSystem = true, sortOrder = 8)
            )

            val expenseParentCategories = listOf(
                CategoryEntity(id = 1, name = "شخصي", type = "EXPENSE", icon = "person", color = "#6C63FF", isSystem = true, sortOrder = 1),
                CategoryEntity(id = 2, name = "عائلي وكفالة", type = "EXPENSE", icon = "ic_cat_family", color = "#22C55E", isSystem = true, sortOrder = 2),
                CategoryEntity(id = 3, name = "منزلي وبناء", type = "EXPENSE", icon = "home", color = "#EF4444", isSystem = true, sortOrder = 3),
                CategoryEntity(id = 4, name = "طعام ومأكولات", type = "EXPENSE", icon = "restaurant", color = "#F59E0B", isSystem = true, sortOrder = 4),
                CategoryEntity(id = 5, name = "مواصلات وتاكسي", type = "EXPENSE", icon = "ic_cat_transport", color = "#3B82F6", isSystem = true, sortOrder = 5),
                CategoryEntity(id = 6, name = "فواتير وشحن", type = "EXPENSE", icon = "ic_cat_flexy", color = "#EE5F5B", isSystem = true, sortOrder = 6),
                CategoryEntity(id = 7, name = "تسوق وملابس", type = "EXPENSE", icon = "shopping_bag", color = "#A770EF", isSystem = true, sortOrder = 7),
                CategoryEntity(id = 8, name = "صحة وصيدلية", type = "EXPENSE", icon = "ic_cat_health", color = "#06B6D4", isSystem = true, sortOrder = 8),
                CategoryEntity(id = 9, name = "تعليم ودروس", type = "EXPENSE", icon = "ic_cat_education", color = "#4FACFE", isSystem = true, sortOrder = 9),
                CategoryEntity(id = 10, name = "ترفيه وألعاب", type = "EXPENSE", icon = "sports_esports", color = "#F35588", isSystem = true, sortOrder = 10),
                CategoryEntity(id = 16, name = "مناسبات وأعياد", type = "EXPENSE", icon = "celebration", color = "#FF5722", isSystem = true, sortOrder = 11),
                CategoryEntity(id = 17, name = "الخدمات البريدية (CCP)", type = "EXPENSE", icon = "account_balance", color = "#EAB308", isSystem = true, sortOrder = 12),
                CategoryEntity(name = "صدقة وزكاة وخير", type = "EXPENSE", icon = "volunteer_activism", color = "#14B8A6", isSystem = true, sortOrder = 13),
                CategoryEntity(name = "مشاريع وتجارة ومهنة", type = "EXPENSE", icon = "storefront", color = "#6366F1", isSystem = true, sortOrder = 14)
            )

            // Phase 1: Insert any missing parent categories safely
            val initialCategories = categoryDao.getAllCategories().first()
            val existingParentKeys = initialCategories
                .filter { it.parentId == null }
                .map { it.name.trim() to it.type }
                .toSet()
            val usedIds = initialCategories.map { it.id }.toMutableSet()

            for (parent in incomeParentCategories + expenseParentCategories) {
                if ((parent.name.trim() to parent.type) in existingParentKeys) continue
                val entityToInsert = if (parent.id > 0 && parent.id in usedIds) {
                    parent.copy(id = 0)
                } else {
                    parent
                }
                val insertedId = categoryDao.insertCategoryIgnoreConflict(entityToInsert)
                if (insertedId > 0) usedIds.add(insertedId)
            }

            // Phase 2: Resolve actual parent IDs dynamically by (name, type)
            val refreshedCategories = categoryDao.getAllCategories().first()
            val parentLookup = refreshedCategories
                .filter { it.parentId == null }
                .associateBy { it.name.trim() to it.type }

            fun resolveParentId(parentName: String, type: String): Long? {
                parentLookup[parentName.trim() to type]?.let { return it.id }
                return refreshedCategories.firstOrNull {
                    it.parentId == null && it.type == type &&
                        (it.name.contains(parentName) || parentName.contains(it.name))
                }?.id
            }

            val subcategorySeeds = listOf(
                // ── INCOME Subcategories ──
                // راتب
                SubcategorySeed("راتب", "INCOME", "راتب شهري (شهرية)", "work", "#22C55E", 1),
                SubcategorySeed("راتب", "INCOME", "مردودية ومنح العمل (Prime / Rappel)", "trending_up", "#16A34A", 2),
                SubcategorySeed("راتب", "INCOME", "تقاعد ومعاش (CNR / CASNOS)", "account_balance", "#15803D", 3),

                // مكافآت وهدايا
                SubcategorySeed("مكافآت وهدايا", "INCOME", "عيدية وهدايا نقدية", "redeem", "#EC4899", 1),
                SubcategorySeed("مكافآت وهدايا", "INCOME", "مساعدة عائلية وحوالة (Mandat)", "ic_cat_family", "#F43F5E", 2),

                // مبيعات
                SubcategorySeed("مبيعات", "INCOME", "تجارة وسلعة", "storefront", "#F59E0B", 1),
                SubcategorySeed("مبيعات", "INCOME", "بيع مستعمل (Ouedkniss / Marketplace)", "smartphone", "#D97706", 2),
                SubcategorySeed("مبيعات", "INCOME", "تجارة إلكترونية وتوصيل", "local_shipping", "#EA580C", 3),

                // عمل إضافي
                SubcategorySeed("عمل إضافي", "INCOME", "عمل حر وفريلانس (Freelance)", "smartphone", "#3B82F6", 1),
                SubcategorySeed("عمل إضافي", "INCOME", "بريكول وحرفة يومية", "build", "#2563EB", 2),
                SubcategorySeed("عمل إضافي", "INCOME", "دروس خصوصية وتكوين", "ic_cat_education", "#1D4ED8", 3),

                // منح وتعويضات
                SubcategorySeed("منح وتعويضات", "INCOME", "منحة البطالة ومنحة جامعية", "school", "#10B981", 1),
                SubcategorySeed("منح وتعويضات", "INCOME", "تعويض الضمان الاجتماعي (CNAS / شيفة)", "ic_cat_health", "#059669", 2),

                // إيجار واستثمار
                SubcategorySeed("إيجار واستثمار", "INCOME", "مدخول كراء عقار أو محل", "apartment", "#8B5CF6", 1),
                SubcategorySeed("إيجار واستثمار", "INCOME", "أرباح شراكة ومشاريع", "trending_up", "#7C3AED", 2),

                // ── EXPENSE Subcategories ──
                // شخصي
                SubcategorySeed("شخصي", "EXPENSE", "ملابس وقش", "checkroom", "#6C63FF", 1),
                SubcategorySeed("شخصي", "EXPENSE", "كوسميتيك وعناية", "spa", "#E91E63", 2),
                SubcategorySeed("شخصي", "EXPENSE", "حلاقة وتجميل", "content_cut", "#8B5CF6", 3),
                SubcategorySeed("شخصي", "EXPENSE", "رياضة ولاصال (Gym)", "fitness_center", "#10B981", 4),

                // عائلي وكفالة
                SubcategorySeed("عائلي وكفالة", "EXPENSE", "مصروف عائلي وكفالة", "ic_cat_family", "#22C55E", 1),
                SubcategorySeed("عائلي وكفالة", "EXPENSE", "مستلزمات الرضع والأطفال", "child_care", "#06B6D4", 2),
                SubcategorySeed("عائلي وكفالة", "EXPENSE", "إعانة ومصروف الوالدين", "volunteer_activism", "#16A34A", 3),

                // منزلي وبناء
                SubcategorySeed("منزلي وبناء", "EXPENSE", "إيجار", "house", "#EF4444", 1),
                SubcategorySeed("منزلي وبناء", "EXPENSE", "كهرباء وغاز", "bolt", "#F59E0B", 2),
                SubcategorySeed("منزلي وبناء", "EXPENSE", "ماء", "water_drop", "#3B82F6", 3),
                SubcategorySeed("منزلي وبناء", "EXPENSE", "إنترنت و ADSL / 4G Box", "ic_cat_flexy", "#6C63FF", 4),
                SubcategorySeed("منزلي وبناء", "EXPENSE", "خردوات ودروغري", "ic_cat_bricolage", "#FF9800", 5),
                SubcategorySeed("منزلي وبناء", "EXPENSE", "خدمات محلية وتصليح", "build", "#795548", 6),
                SubcategorySeed("منزلي وبناء", "EXPENSE", "أثاث وكهرومنزلي", "kitchen", "#8B5CF6", 7),
                SubcategorySeed("منزلي وبناء", "EXPENSE", "أقساط سكن (AADL / LPP)", "apartment", "#DC2626", 8),

                // طعام ومأكولات
                SubcategorySeed("طعام ومأكولات", "EXPENSE", "مارشي وخضار", "ic_cat_marche", "#22C55E", 1),
                SubcategorySeed("طعام ومأكولات", "EXPENSE", "أكل سريع وبلاطو", "ic_cat_fastfood", "#FF5722", 2),
                SubcategorySeed("طعام ومأكولات", "EXPENSE", "بقالة ومواد غذائية", "shopping_cart", "#4CAF50", 3),
                SubcategorySeed("طعام ومأكولات", "EXPENSE", "مطاعم ومقاهي", "restaurant", "#F59E0B", 4),
                SubcategorySeed("طعام ومأكولات", "EXPENSE", "جزار ولحوم ودواجن", "restaurant", "#EF4444", 5),
                SubcategorySeed("طعام ومأكولات", "EXPENSE", "مخبزة وحلويات (قاطو)", "bakery_dining", "#D97706", 6),
                SubcategorySeed("طعام ومأكولات", "EXPENSE", "قهوة وأتاي", "coffee", "#92400E", 7),

                // مواصلات وتاكسي
                SubcategorySeed("مواصلات وتاكسي", "EXPENSE", "وقود ونفطال", "local_gas_station", "#3B82F6", 1),
                SubcategorySeed("مواصلات وتاكسي", "EXPENSE", "تاكسي وتطبيقات (Yassir/Heetch)", "ic_cat_transport", "#F59E0B", 2),
                SubcategorySeed("مواصلات وتاكسي", "EXPENSE", "حافلة وترامواي", "directions_bus", "#22C55E", 3),
                SubcategorySeed("مواصلات وتاكسي", "EXPENSE", "صيانة السيارة وقطع غيار", "build", "#64748B", 4),
                SubcategorySeed("مواصلات وتاكسي", "EXPENSE", "تأمين ووثائق المركبة", "verified_user", "#0EA5E9", 5),
                SubcategorySeed("مواصلات وتاكسي", "EXPENSE", "سفر وبين الولايات", "flight", "#6366F1", 6),

                // فواتير وشحن
                SubcategorySeed("فواتير وشحن", "EXPENSE", "فليكسي ورصيد هاتف", "ic_cat_flexy", "#EE5F5B", 1),
                SubcategorySeed("فواتير وشحن", "EXPENSE", "اشتراكات رقمية ومنصات", "subscriptions", "#8B5CF6", 2),
                SubcategorySeed("فواتير وشحن", "EXPENSE", "ضرائب ورسوم إدارية (CNAS / CASNOS)", "description", "#F97316", 3),

                // تسوق وملابس
                SubcategorySeed("تسوق وملابس", "EXPENSE", "إلكترونيات وهواتف وإكسسوارات", "smartphone", "#3B82F6", 1),
                SubcategorySeed("تسوق وملابس", "EXPENSE", "تسوق إلكتروني وتوصيل", "local_shipping", "#A855F7", 2),
                SubcategorySeed("تسوق وملابس", "EXPENSE", "أواني ومستلزمات منزلية", "shopping_bag", "#EC4899", 3),

                // صحة وصيدلية
                SubcategorySeed("صحة وصيدلية", "EXPENSE", "صيدلية ودواء", "ic_cat_health", "#00BCD4", 1),
                SubcategorySeed("صحة وصيدلية", "EXPENSE", "عيادة وتحاليل", "medical_services", "#0EA5E9", 2),
                SubcategorySeed("صحة وصيدلية", "EXPENSE", "طبيب أسنان ونظارات", "medical_services", "#14B8A6", 3),

                // تعليم ودروس
                SubcategorySeed("تعليم ودروس", "EXPENSE", "دروس خصوصية (لي كور)", "ic_cat_education", "#3B82F6", 1),
                SubcategorySeed("تعليم ودروس", "EXPENSE", "كتب وأدوات مدرسية وجامعية", "backpack", "#6366F1", 2),
                SubcategorySeed("تعليم ودروس", "EXPENSE", "روضة وحضانة (كراش)", "child_care", "#EC4899", 3),
                SubcategorySeed("تعليم ودروس", "EXPENSE", "دورات تكوينية ولغات", "school", "#0EA5E9", 4),

                // ترفيه وألعاب
                SubcategorySeed("ترفيه وألعاب", "EXPENSE", "ألعاب وتسلية (Gaming)", "sports_esports", "#F35588", 1),
                SubcategorySeed("ترفيه وألعاب", "EXPENSE", "نزهات وعطل وسفر", "flight", "#F59E0B", 2),

                // مناسبات وأعياد
                SubcategorySeed("مناسبات وأعياد", "EXPENSE", "رمضان وقفة رمضان", "nights_stay", "#10B981", 1),
                SubcategorySeed("مناسبات وأعياد", "EXPENSE", "عيد الفطر وكسوة العيد", "celebration", "#F59E0B", 2),
                SubcategorySeed("مناسبات وأعياد", "EXPENSE", "عيد الأضحى والأضحية", "celebration", "#EF4444", 3),
                SubcategorySeed("مناسبات وأعياد", "EXPENSE", "أعراس وهدايا ومهيبة", "cake", "#EC4899", 4),
                SubcategorySeed("مناسبات وأعياد", "EXPENSE", "الدخول المدرسي", "backpack", "#3B82F6", 5),

                // الخدمات البريدية (CCP)
                SubcategorySeed("الخدمات البريدية (CCP)", "EXPENSE", "مستحقات البريد (CCP)", "account_balance", "#EAB308", 1),
                SubcategorySeed("الخدمات البريدية (CCP)", "EXPENSE", "البطاقة الذهبية", "credit_card", "#F97316", 2),
                SubcategorySeed("الخدمات البريدية (CCP)", "EXPENSE", "عمولات تحويل وبريدي موب", "sync_alt", "#F59E0B", 3),

                // صدقة وزكاة وخير
                SubcategorySeed("صدقة وزكاة وخير", "EXPENSE", "صدقة وتبرعات", "volunteer_activism", "#14B8A6", 1),
                SubcategorySeed("صدقة وزكاة وخير", "EXPENSE", "زكاة المال وزكاة الفطر", "volunteer_activism", "#0D9488", 2),

                // مشاريع وتجارة ومهنة
                SubcategorySeed("مشاريع وتجارة ومهنة", "EXPENSE", "سلعة ورأس مال تجاري", "storefront", "#6366F1", 1),
                SubcategorySeed("مشاريع وتجارة ومهنة", "EXPENSE", "شحن وتوصيل طلبيات (Yalidine / ZR)", "local_shipping", "#8B5CF6", 2),
                SubcategorySeed("مشاريع وتجارة ومهنة", "EXPENSE", "إعلانات ممولة وتسويق (Sponsoring)", "campaign", "#EC4899", 3)
            )

            val existingSubKeys = refreshedCategories
                .filter { it.parentId != null }
                .map { Triple(it.name.trim(), it.type, it.parentId) }
                .toSet()

            for (sub in subcategorySeeds) {
                val parentId = resolveParentId(sub.parentName, sub.type) ?: continue
                if (Triple(sub.name.trim(), sub.type, parentId) in existingSubKeys) continue
                categoryDao.insertCategoryIgnoreConflict(
                    CategoryEntity(
                        name = sub.name,
                        type = sub.type,
                        icon = sub.icon,
                        color = sub.color,
                        isSystem = true,
                        parentId = parentId,
                        sortOrder = sub.sortOrder
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
