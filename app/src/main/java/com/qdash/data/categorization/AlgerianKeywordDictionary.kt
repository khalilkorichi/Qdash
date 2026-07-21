package com.qdash.data.categorization

import java.util.Locale

/**
 * Isolated dictionary of Algerian financial terms, dialect keywords (Darja), French,
 * and local daily transaction keywords mapped to category/subcategory names.
 */
object AlgerianKeywordDictionary {

    data class KeywordCategoryMapping(
        val keywords: List<String>,
        val categoryName: String,
        val subcategoryName: String? = null
    )

    private val matcher = KeywordMatcher()

    val mappings: List<KeywordCategoryMapping> = listOf(
        KeywordCategoryMapping(
            keywords = listOf("فلإكسي", "فليكسي", "موبيليس", "جيزي", "أوريدو", "شحن هاتف", "شحن", "flexy", "mobilis", "djezzy", "ooredoo"),
            categoryName = "فواتير",
            subcategoryName = "إنترنت"
        ),
        KeywordCategoryMapping(
            keywords = listOf("خضرة", "خضار", "فواكه", "مارشي", "سوق", "بطاطا", "طماطم", "بصل", "الجزار", "لحم", "دجاج", "قضيان", "حليب", "خبز", "سوبيرات", "حانوت"),
            categoryName = "طعام",
            subcategoryName = "بقالة"
        ),
        KeywordCategoryMapping(
            keywords = listOf("فاست فود", "أكل سريع", "محاجب", "كاران", "كرنطيطا", "شواية", "بيتزا", "ساندويتش", "طاغكوص", "تاكوس", "شاورما", "فريت", "ماكلة", "مطعم", "قهوة", "أتاي", "شاي"),
            categoryName = "طعام",
            subcategoryName = "مطاعم"
        ),
        KeywordCategoryMapping(
            keywords = listOf("حافلة", "تاكسي", "قطار", "تراموا", "ترامواي", "كلوندستان", "بيس", "كار", "ياسير", "وجيز", "سوبرين", "yassir", "heetch", "weshare", "كورسا", "كورس"),
            categoryName = "مواصلات",
            subcategoryName = "تاكسي"
        ),
        KeywordCategoryMapping(
            keywords = listOf("وقود", "بنزين", "مازوت", "نفطال", "gasoil", "essence", "naftal"),
            categoryName = "مواصلات",
            subcategoryName = "وقود"
        ),
        KeywordCategoryMapping(
            keywords = listOf("كوسميتيك", "حلاقة", "شعر", "كوسميتيكس", "حجام", "دوش", "حمام", "عطر", "شامبو", "عناية"),
            categoryName = "شخصي",
            subcategoryName = "عناية شخصية"
        ),
        KeywordCategoryMapping(
            keywords = listOf("دروغري", "دروغيري", "خردوات", "عقاقير", "عتاد", "بريكولاج", "سيال", "طلاء", "مسامير", "بريكول"),
            categoryName = "منزلي",
            subcategoryName = "خردوات وعتاد"
        ),
        KeywordCategoryMapping(
            keywords = listOf("انترنت", "ادسل", "أدسل", "4g", "4جي", "مودام", "موديم", "اتصالات الجزائر", "فايبر", "شحن انترنت"),
            categoryName = "منزلي",
            subcategoryName = "إنترنت"
        ),
        KeywordCategoryMapping(
            keywords = listOf("سونلغاز", "كهرباء", "غاز", "تريسيتي", "فواتير سونلغاز", "sonelgaz", "ade"),
            categoryName = "منزلي",
            subcategoryName = "كهرباء وغاز"
        ),
        KeywordCategoryMapping(
            keywords = listOf("كفالة", "مصروف", "عائلة", "مصروف الدار", "الوالدين", "نفقة", "اولاد", "زوجة", "اطفال"),
            categoryName = "عائلي",
            subcategoryName = "مصروف عائلي وكفالة"
        ),
        KeywordCategoryMapping(
            keywords = listOf("تريسيان", "بلومبي", "صباغ", "ميكانيسيان", "ميكانيكي", "تصليح", "غسيل", "ديباناج", "خدمات محلية"),
            categoryName = "منزلي",
            subcategoryName = "خدمات محلية"
        ),
        KeywordCategoryMapping(
            keywords = listOf("صيدلية", "فرماسي", "فرماسيا", "دواء", "طبيب", "كلينيك", "ليزاناليز", "راديو", "تحاليل", "دونتيست", "طبيب الاسنان", "صحة"),
            categoryName = "صحة"
        ),
        KeywordCategoryMapping(
            keywords = listOf("مدرسة", "جامعة", "تعليم", "روضة", "كراش", "ليكوش", "كور", "كتاب", "ادوات مدرسية", "مدرسة خاصة"),
            categoryName = "تعليم"
        ),
        KeywordCategoryMapping(
            keywords = listOf("حوايج", "قش", "صباط", "جلابة", "قاطو", "لباس", "حذاء", "ملابس", "boutique", "هدايا", "كادو", "تسوق"),
            categoryName = "شخصي",
            subcategoryName = "ملابس"
        ),
        KeywordCategoryMapping(
            keywords = listOf("إيجار", "اجار", "كراء", "اثاث", "منزل", "كرية"),
            categoryName = "منزلي",
            subcategoryName = "إيجار"
        ),
        KeywordCategoryMapping(
            keywords = listOf("راتب", "سيرك", "salary", "paye", "خلصة", "الرصيد", "الراتب", "الشهري"),
            categoryName = "راتب"
        ),
        KeywordCategoryMapping(
            keywords = listOf("كبش", "علف", "اضحية", "عيد الكبير", "الاضحى", "كبش العيد"),
            categoryName = "مناسبات وأعيad",
            subcategoryName = "عيد الأضحى"
        ),
        KeywordCategoryMapping(
            keywords = listOf("رمضان", "زلابية", "قلب اللوز", "ديول", "سحور"),
            categoryName = "مناسبات وأعياد",
            subcategoryName = "رمضان"
        ),
        KeywordCategoryMapping(
            keywords = listOf("طابلي", "مئزر", "محفظة", "كراس", "كتب مدرسية"),
            categoryName = "مناسبات وأعياد",
            subcategoryName = "الدخول المدرسي"
        ),
        KeywordCategoryMapping(
            keywords = listOf("ذهبية", "الذهبية", "حقوق ccp", "سي سي بي", "دروا سي سي بي", "كارني شيك", "شيك", "اقتطاع"),
            categoryName = "الخدمات البريدية",
            subcategoryName = "البطاقة الذهبية"
        ),
        KeywordCategoryMapping(
            keywords = listOf("netflix", "spotify", "نتفلكس", "نتفليكس", "سبوتيفاي", "steam", "playstation", "pubg", "gaming", "العاب", "ترفيه"),
            categoryName = "ترفيه",
            subcategoryName = "ألعاب"
        )
    )

    fun findMatchingCategory(normalizedTitle: String): KeywordCategoryMapping? {
        if (normalizedTitle.isBlank()) return null
        return mappings.firstOrNull { mapping ->
            mapping.keywords.any { kw -> matcher.containsKeyword(normalizedTitle, kw) }
        }
    }
}
