package com.qdash.data.categorization

/**
 * Isolated dictionary of Algerian financial terms, dialect keywords (Darja), Franco-Algerian (Arabizi),
 * French loanwords, and daily transaction phrases mapped to category/subcategory names with strict
 * transaction-type awareness (EXPENSE vs INCOME).
 */
object AlgerianKeywordDictionary {

    data class KeywordCategoryMapping(
        val keywords: List<String>,
        val categoryName: String,
        val subcategoryName: String? = null,
        val transactionType: String = "EXPENSE",
        val seededCategoryTarget: String? = null,
        val seededSubcategoryTarget: String? = null
    )

    private val matcher = KeywordMatcher()

    val mappings: List<KeywordCategoryMapping> = listOf(
        // ═══════════════════════════════════════════════════════════════════════
        // EXPENSE MAPPINGS (المصاريف والنفقات بالدارجة الجزائرية والفرنسية)
        // ═══════════════════════════════════════════════════════════════════════

        // 1. فواتير وشحن — فليكسي ورصيد هاتف
        KeywordCategoryMapping(
            keywords = listOf(
                "فلإكسي", "فليكسي", "موبيليس", "جيزي", "اوريدو", "أوريدو", "بيكس", "شحن هاتف", "رصيد هاتف",
                "تعبئة رصيد", "شحن", "كارت موبيليس", "كارت جيزي", "كارت اوريدو", "تعبئه",
                "flexy", "fliksy", "mobilis", "djezzy", "ooredoo", "pixx", "dima", "hayla", "yooz", "recharge"
            ),
            categoryName = "فواتير",
            subcategoryName = "إنترنت",
            transactionType = "EXPENSE",
            seededCategoryTarget = "فواتير وشحن",
            seededSubcategoryTarget = "فليكسي ورصيد هاتف"
        ),

        // 2. منزلي وبناء — إنترنت و ADSL / 4G Box
        KeywordCategoryMapping(
            keywords = listOf(
                "انترنت", "ادسل", "أدسل", "ايدوم", "إيدوم", "فايبر", "فيبر", "4g", "4جي", "مودام", "موديم",
                "اتصالات الجزائر", "شحن انترنت", "كرت ايدوم", "اشتراك انترنت", "ويفي",
                "idoom", "adsl", "ftth", "fibre", "algerie telecom", "4g box", "wifi", "modem"
            ),
            categoryName = "منزلي",
            subcategoryName = "إنترنت",
            transactionType = "EXPENSE",
            seededCategoryTarget = "منزلي وبناء",
            seededSubcategoryTarget = "إنترنت و ADSL / 4G Box"
        ),

        // 3. منزلي وبناء — كهرباء وغاز (سونلغاز)
        KeywordCategoryMapping(
            keywords = listOf(
                "سونلغاز", "كهرباء", "غاز", "تريسيتي", "فواتير سونلغاز", "فاتورة الكهرباء", "فاتورة الغاز",
                "فاتورة الضوء", "قرعة غاز", "قارورة غاز", "بوطاغاز",
                "sonelgaz", "sonalgaz", "electricite", "trisiti", "butagaz"
            ),
            categoryName = "منزلي",
            subcategoryName = "كهرباء وغاز",
            transactionType = "EXPENSE",
            seededCategoryTarget = "منزلي وبناء",
            seededSubcategoryTarget = "كهرباء وغاز"
        ),

        // 4. منزلي وبناء — ماء (سيال / ADE)
        KeywordCategoryMapping(
            keywords = listOf(
                "سيال", "الجزائرية للمياه", "فاتورة الماء", "فاتورة المياه", "ماء", "سيتيرنة", "صهريج ماء",
                "seaal", "ade", "facture eau", "citerna"
            ),
            categoryName = "منزلي",
            subcategoryName = "ماء",
            transactionType = "EXPENSE",
            seededCategoryTarget = "منزلي وبناء",
            seededSubcategoryTarget = "ماء"
        ),

        // 5. طعام ومأكولات — مارشي وخضار وبقالة
        KeywordCategoryMapping(
            keywords = listOf(
                "خضرة", "خضار", "فواكه", "فاكهة", "مارشي", "سوق", "بطاطا", "طماطم", "بصل", "ثوم", "سلطة",
                "حشيش", "تمور", "دقلة", "قضيان السوق", "قضيان الخضرة",
                "marche", "khodra", "khodhra", "legumes", "fruits", "souk"
            ),
            categoryName = "طعام",
            subcategoryName = "بقالة",
            transactionType = "EXPENSE",
            seededCategoryTarget = "طعام ومأكولات",
            seededSubcategoryTarget = "مارشي وخضار"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "قضيان", "حليب", "شكارة حليب", "سميد", "فرينة", "زيت", "سكر", "قهوة علبة", "مواد غذائية",
                "سوبيرات", "سوبرات", "حانوت", "بقالة", "ياغورت", "فرماج", "جبن", "زبدة", "عجائن", "مقرونة",
                "كسكس", "طماطم مصبرة", "تونة", "ماء معدني", "سعيدة", "افري",
                "superette", "supérette", "hanout", "epicerie", "qadyan", "9adyan", "alimentation"
            ),
            categoryName = "طعام",
            subcategoryName = "بقالة",
            transactionType = "EXPENSE",
            seededCategoryTarget = "طعام ومأكولات",
            seededSubcategoryTarget = "بقالة ومواد غذائية"
        ),

        // 6. طعام ومأكولات — جزار ولحوم ودواجن
        KeywordCategoryMapping(
            keywords = listOf(
                "الجزار", "جزار", "بوشي", "لحم", "دجاج", "جاج", "سكالوب", "كفتة", "فيونداشي", "مرقاز",
                "كبدة", "دوارة", "حوت", "سمك", "سردين", "جمبري", "بيض", "بلاطو بيض",
                "boucherie", "boucher", "viande", "poulet", "djaj", "lham", "escalope", "merguez", "poisson", "sardine"
            ),
            categoryName = "طعام",
            subcategoryName = "بقالة",
            transactionType = "EXPENSE",
            seededCategoryTarget = "طعام ومأكولات",
            seededSubcategoryTarget = "جزار ولحوم ودواجن"
        ),

        // 7. طعام ومأكولات — مخبزة وحلويات
        KeywordCategoryMapping(
            keywords = listOf(
                "خبز", "مخبزة", "باقيط", "مطلوع", "كسرة", "كرواسون", "بتي بان", "بريوش", "باتيسري",
                "قاطو", "حلويات", "مقروط", "بقلاوة", "تشاراك", "كعك", "تارت", "ميلفاي",
                "boulangerie", "patisserie", "pain", "khobz", "croissant", "gateau"
            ),
            categoryName = "طعام",
            subcategoryName = "بقالة",
            transactionType = "EXPENSE",
            seededCategoryTarget = "طعام ومأكولات",
            seededSubcategoryTarget = "مخبزة وحلويات (قاطو)"
        ),

        // 8. طعام ومأكولات — أكل سريع ومطاعم ومقاهي
        KeywordCategoryMapping(
            keywords = listOf(
                "فاست فود", "أكل سريع", "محاجب", "كاران", "كرنطيطا", "قرنطيطة", "شواية", "بيتزا", "ساندويتش",
                "طاغكوص", "تاكوس", "طاكوس", "شاورما", "فريت", "فريت اومليت", "برغر", "هامبرغر", "بانيني", "كراب",
                "بلاطو", "سوفلي", "بورك", "بوراك",
                "mhajeb", "garantita", "karantika", "pizza", "tacos", "chawarma", "shawarma", "fast food", "sandwich", "burger"
            ),
            categoryName = "طعام",
            subcategoryName = "مطاعم",
            transactionType = "EXPENSE",
            seededCategoryTarget = "طعام ومأكولات",
            seededSubcategoryTarget = "أكل سريع وبلاطو"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "ماكلة", "مطعم", "غداء برا", "عشاء برا", "فطور", "شخشوخة", "رشتة", "مشوي", "طاجين",
                "restaurant", "resto", "dejeuner", "diner", "makla"
            ),
            categoryName = "طعام",
            subcategoryName = "مطاعم",
            transactionType = "EXPENSE",
            seededCategoryTarget = "طعام ومأكولات",
            seededSubcategoryTarget = "مطاعم ومقاهي"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "قهوة", "قهيوه", "أتاي", "اتاي", "شاي", "كافيتيريا", "قاعة شاي", "عصير", "كابوتشينو", "اكسبريسو", "كريبوني", "مثلجات", "لاص",
                "qahwa", "9ahwa", "kahwa", "cafe", "atay", "salon de the", "glace", "creponne"
            ),
            categoryName = "طعام",
            subcategoryName = "مطاعم",
            transactionType = "EXPENSE",
            seededCategoryTarget = "طعام ومأكولات",
            seededSubcategoryTarget = "قهوة وأتاي"
        ),

        // 9. مواصلات وتاكسي — تاكسي، حافلة، وقود، صيانة، وثائق، سفر
        KeywordCategoryMapping(
            keywords = listOf(
                "تاكسي", "طاكسي", "كلوندستان", "فرود", "ياسير", "هيتش", "اندرايف", "وجيز", "كورسا", "كورس",
                "yassir", "heetch", "indrive", "weshare", "taxi", "clando", "course"
            ),
            categoryName = "مواصلات",
            subcategoryName = "تاكسي",
            transactionType = "EXPENSE",
            seededCategoryTarget = "مواصلات وتاكسي",
            seededSubcategoryTarget = "تاكسي وتطبيقات (Yassir/Heetch)"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "حافلة", "قطار", "تراموا", "ترامواي", "ميترو", "مترو", "تيليفيريك", "بيس", "كوستر", "ايتوزا", "سوبرين", "نقل جامعي",
                "bus", "tramway", "tram", "metro", "etusa", "telepherique"
            ),
            categoryName = "مواصلات",
            subcategoryName = "تاكسي",
            transactionType = "EXPENSE",
            seededCategoryTarget = "مواصلات وتاكسي",
            seededSubcategoryTarget = "حافلة وترامواي"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "وقود", "بنزين", "مازوت", "نفطال", "ليسونس", "سيرغاز", "غازوال", "تعميرة وقود",
                "gasoil", "essence", "naftal", "sirghaz", "gpl", "mazout", "carburant"
            ),
            categoryName = "مواصلات",
            subcategoryName = "وقود",
            transactionType = "EXPENSE",
            seededCategoryTarget = "مواصلات وتاكسي",
            seededSubcategoryTarget = "وقود ونفطال"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "ميكانيسيان", "ميكانيكي", "فيدونج", "زيت محرك", "فيلتر", "عجلات", "بنو", "باتري", "بطارية السيارة",
                "لافاج", "غسيل السيارة", "طولي", "كهرباء السيارات", "قطع غيار", "لاكاس", "باركينغ", "حظيرة",
                "vidange", "mecanicien", "lavage", "pneu", "batterie", "parking", "piece auto"
            ),
            categoryName = "مواصلات",
            subcategoryName = "صيانة السيارة وقطع غيار",
            transactionType = "EXPENSE",
            seededCategoryTarget = "مواصلات وتاكسي",
            seededSubcategoryTarget = "صيانة السيارة وقطع غيار"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "لاسيرونس", "تأمين السيارة", "تامين السيارة", "فينيات", "قسيمة السيارات", "سكانير السيارة",
                "كونترول تكنيك", "رخصة السياقة", "بروسي", "غرامة مرورية",
                "assurance auto", "vignette", "controle technique"
            ),
            categoryName = "مواصلات",
            subcategoryName = "تأمين ووثائق المركبة",
            transactionType = "EXPENSE",
            seededCategoryTarget = "مواصلات وتاكسي",
            seededSubcategoryTarget = "تأمين ووثائق المركبة"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "لواج", "سوغرال", "سفر بين الولايات", "تذكرة قطار", "تذكرة طيران", "الخطوط الجوية", "طائرة",
                "sogral", "sntf", "air algerie", "billet avion", "louage"
            ),
            categoryName = "مواصلات",
            subcategoryName = "سفر وبين الولايات",
            transactionType = "EXPENSE",
            seededCategoryTarget = "مواصلات وتاكسي",
            seededSubcategoryTarget = "سفر وبين الولايات"
        ),

        // 10. شخصي — عناية، حلاقة، رياضة، ملابس
        KeywordCategoryMapping(
            keywords = listOf(
                "حلاقة", "حفاف", "حجام", "كوافور", "صالون حلاقة", "شعر", "لحية",
                "coiffeur", "barbier", "salon", "7affaf", "haffaf"
            ),
            categoryName = "شخصي",
            subcategoryName = "عناية شخصية",
            transactionType = "EXPENSE",
            seededCategoryTarget = "شخصي",
            seededSubcategoryTarget = "حلاقة وتجميل"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "كوسميتيك", "كوسميتيكس", "دوش", "حمام", "عطر", "بارفان", "ريحة", "شامبو", "عناية", "كريم", "ديودورون", "ماكياج",
                "cosmetique", "parfum", "maquillage", "hammam", "soin"
            ),
            categoryName = "شخصي",
            subcategoryName = "عناية شخصية",
            transactionType = "EXPENSE",
            seededCategoryTarget = "شخصي",
            seededSubcategoryTarget = "كوسميتيك وعناية"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "لاصال", "قاعة رياضة", "سبور", "مسكولاسيون", "اشتراك جيم", "فوتبول", "ملعب", "ماتش", "كراء ستاد", "بروتين",
                "salle de sport", "gym", "musculation", "fitness", "stade", "lasal"
            ),
            categoryName = "شخصي",
            subcategoryName = "رياضة ولاصال (Gym)",
            transactionType = "EXPENSE",
            seededCategoryTarget = "شخصي",
            seededSubcategoryTarget = "رياضة ولاصال (Gym)"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "حوايج", "قش", "صباط", "باسكات", "جلابة", "قندورة", "قميص", "تريكو", "سروال", "فيست", "معطف",
                "لباس", "حذاء", "ملابس", "حجاب", "عباية", "كسوة",
                "boutique", "qach", "9ach", "hwayej", "sabbat", "basket", "vetements"
            ),
            categoryName = "شخصي",
            subcategoryName = "ملابس",
            transactionType = "EXPENSE",
            seededCategoryTarget = "شخصي",
            seededSubcategoryTarget = "ملابس وقش"
        ),

        // 11. تسوق وملابس — إلكترونيات، توصيل، أواني
        KeywordCategoryMapping(
            keywords = listOf(
                "هاتف", "تيليفون", "شارجور", "شاحن", "كابل", "ايربودز", "سماعات", "بوشيت", "انتي شوك",
                "حاسوب", "بيسي", "فلاش ديسك", "باور بانك", "تصليح هاتف", "افيشور",
                "telephone", "chargeur", "ecouteurs", "airpods", "pochette", "pc", "laptop"
            ),
            categoryName = "تسوق وملابس",
            subcategoryName = "إلكترونيات وهواتف وإكسسوارات",
            transactionType = "EXPENSE",
            seededCategoryTarget = "تسوق وملابس",
            seededSubcategoryTarget = "إلكترونيات وهواتف وإكسسوارات"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "طلبية", "كولي", "توصيل طلبية", "الي اكسبريس", "شي ان", "امازون", "تسوق الكتروني",
                "aliexpress", "shein", "temu", "colis", "commande"
            ),
            categoryName = "تسوق وملابس",
            subcategoryName = "تسوق إلكتروني وتوصيل",
            transactionType = "EXPENSE",
            seededCategoryTarget = "تسوق وملابس",
            seededSubcategoryTarget = "تسوق إلكتروني وتوصيل"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "ماعن", "اواني", "كاسرونة", "كوكوط", "طقم صحون", "افرشة", "زربية", "ستائر", "مواد تنظيف", "جافيل", "اومو", "ازيس",
                "vaisselle", "isis", "javel", "nettoyage"
            ),
            categoryName = "تسوق وملابس",
            subcategoryName = "أواني ومستلزمات منزلية",
            transactionType = "EXPENSE",
            seededCategoryTarget = "تسوق وملابس",
            seededSubcategoryTarget = "أواني ومستلزمات منزلية"
        ),

        // 12. منزلي وبناء — خردوات، تصليح، إيجار، أثاث، عدل
        KeywordCategoryMapping(
            keywords = listOf(
                "دروغري", "دروغيري", "خردوات", "عقاقير", "عتاد", "بريكولاج", "طلاء", "صبيغة", "مسامير",
                "اسمنت", "سيما", "رمل", "اجور", "فايونس", "انابيب", "حنفية", "لامبة", "مصباح",
                "droguerie", "quincaillerie", "bricolage", "peinture", "ciment"
            ),
            categoryName = "منزلي",
            subcategoryName = "خردوات وعتاد",
            transactionType = "EXPENSE",
            seededCategoryTarget = "منزلي وبناء",
            seededSubcategoryTarget = "خردوات ودروغري"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "تريسيان", "بلومبي", "رصاص", "صباغ", "ماصو", "بناء", "نجار", "سودور", "تصليح مكيف", "كليماتيزور",
                "تصليح ثلاجة", "ديباناج", "خدمات محلية",
                "plombier", "electricien", "macon", "menuisier", "reparation"
            ),
            categoryName = "منزلي",
            subcategoryName = "خدمات محلية",
            transactionType = "EXPENSE",
            seededCategoryTarget = "منزلي وبناء",
            seededSubcategoryTarget = "خدمات محلية وتصليح"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "أثاث", "اثاث", "صالون مغربي", "خزانة", "سرير", "مطرح", "ثلاجة", "فريجيدار", "غسالة",
                "ماشينة لافي", "كويزينيير", "مكيف", "تلفاز", "كهرومنزلي",
                "electromenager", "frigo", "machine a laver", "climatiseur", "meuble"
            ),
            categoryName = "منزلي",
            subcategoryName = "أثاث وكهرومنزلي",
            transactionType = "EXPENSE",
            seededCategoryTarget = "منزلي وبناء",
            seededSubcategoryTarget = "أثاث وكهرومنزلي"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "عدل", "قسط عدل", "سكن ترقوي", "ديوان الترقية", "اوبجيي", "قسط السكن",
                "aadl", "lpp", "opgi", "lpa"
            ),
            categoryName = "منزلي",
            subcategoryName = "أقساط سكن (AADL / LPP)",
            transactionType = "EXPENSE",
            seededCategoryTarget = "منزلي وبناء",
            seededSubcategoryTarget = "أقساط سكن (AADL / LPP)"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "إيجار", "اجار", "كراء", "كرية", "كرية الدار", "كراء الشقة",
                "loyer", "kerya", "kraya"
            ),
            categoryName = "منزلي",
            subcategoryName = "إيجار",
            transactionType = "EXPENSE",
            seededCategoryTarget = "منزلي وبناء",
            seededSubcategoryTarget = "إيجار"
        ),

        // 13. عائلي وكفالة
        KeywordCategoryMapping(
            keywords = listOf(
                "ليكوش", "حفاضات", "حليب الرضع", "بيبرون", "مستلزمات الرضيع", "بيبي",
                "couches", "les couches", "biberon", "lait bebe"
            ),
            categoryName = "عائلي",
            subcategoryName = "مستلزمات الرضع والأطفال",
            transactionType = "EXPENSE",
            seededCategoryTarget = "عائلي وكفالة",
            seededSubcategoryTarget = "مستلزمات الرضع والأطفال"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "الوالدين", "مصروف يما", "مصروف بابا", "اعانة الوالدين", "امي", "ابي"
            ),
            categoryName = "عائلي",
            subcategoryName = "إعانة ومصروف الوالدين",
            transactionType = "EXPENSE",
            seededCategoryTarget = "عائلي وكفالة",
            seededSubcategoryTarget = "إعانة ومصروف الوالدين"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "كفالة", "مصروف الدار", "عائلة", "نفقة", "مصروف الاولاد", "مصروف الزوجة", "الدار", "مصروف اسبوعي"
            ),
            categoryName = "عائلي",
            subcategoryName = "مصروف عائلي وكفالة",
            transactionType = "EXPENSE",
            seededCategoryTarget = "عائلي وكفالة",
            seededSubcategoryTarget = "مصروف عائلي وكفالة"
        ),

        // 14. صحة وصيدلية
        KeywordCategoryMapping(
            keywords = listOf(
                "دونتيست", "طبيب الاسنان", "طبيب اسنان", "ضرسة", "تقويم اسنان", "نظارات", "اوبتيسيان", "عدسات",
                "dentiste", "opticien", "lunettes"
            ),
            categoryName = "صحة",
            subcategoryName = "طبيب أسنان ونظارات",
            transactionType = "EXPENSE",
            seededCategoryTarget = "صحة وصيدلية",
            seededSubcategoryTarget = "طبيب أسنان ونظارات"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "طبيب", "تبيب", "فيزيتا", "فحص طبي", "عيادة", "كلينيك", "ليزاناليز", "تحاليل", "لابو",
                "مخبر تحاليل", "راديو", "ايكوغرافي", "سكانير طبي", "ام ار اي", "عملية جراحية", "سبيطار",
                "medecin", "tbib", "visite medicale", "clinique", "analyses", "labo", "scanner", "irm", "radio"
            ),
            categoryName = "صحة",
            subcategoryName = "عيادة وتحاليل",
            transactionType = "EXPENSE",
            seededCategoryTarget = "صحة وصيدلية",
            seededSubcategoryTarget = "عيادة وتحاليل"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "صيدلية", "فرماسي", "فرماسيا", "دواء", "ادوية", "دوا", "باراسيتامول", "دوليبران", "فيتامين", "سيروم", "صحة",
                "pharmacie", "dwa", "medicament", "doliprane"
            ),
            categoryName = "صحة",
            subcategoryName = "صيدلية ودواء",
            transactionType = "EXPENSE",
            seededCategoryTarget = "صحة وصيدلية",
            seededSubcategoryTarget = "صيدلية ودواء"
        ),

        // 15. تعليم ودروس
        KeywordCategoryMapping(
            keywords = listOf(
                "روضة", "كراش", "حضانة", "تحضيري",
                "creche", "crèche", "garderie"
            ),
            categoryName = "تعليم",
            subcategoryName = "روضة وحضانة (كراش)",
            transactionType = "EXPENSE",
            seededCategoryTarget = "تعليم ودروس",
            seededSubcategoryTarget = "روضة وحضانة (كراش)"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "لي كور", "ليكور", "دروس خصوصية", "دروس دعم", "فوج دعم", "استاذ خصوصي",
                "les cours", "cours particuliers", "soutien scolaire"
            ),
            categoryName = "تعليم",
            subcategoryName = "دروس خصوصية (لي كور)",
            transactionType = "EXPENSE",
            seededCategoryTarget = "تعليم ودروس",
            seededSubcategoryTarget = "دروس خصوصية (لي كور)"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "دورة تكوينية", "تكوين", "معهد لغات", "تعلم الانجليزية", "تعلم الفرنسية", "شهادة تكوين",
                "formation", "stage", "langues"
            ),
            categoryName = "تعليم",
            subcategoryName = "دورات تكوينية ولغات",
            transactionType = "EXPENSE",
            seededCategoryTarget = "تعليم ودروس",
            seededSubcategoryTarget = "دورات تكوينية ولغات"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "مدرسة", "جامعة", "تعليم", "حقوق التسجيل", "كتاب", "ادوات مدرسية", "فوتوكوبي", "طباعة",
                "تيريلوجي", "مذكرة تخرج", "مدرسة خاصة",
                "ecole", "universite", "photocopie", "impression", "livre"
            ),
            categoryName = "تعليم",
            subcategoryName = "كتب وأدوات مدرسية وجامعية",
            transactionType = "EXPENSE",
            seededCategoryTarget = "تعليم ودروس",
            seededSubcategoryTarget = "كتب وأدوات مدرسية وجامعية"
        ),

        // 16. مناسبات وأعياد
        KeywordCategoryMapping(
            keywords = listOf(
                "كبش", "علف", "اضحية", "أضحية", "عيد الكبير", "الاضحى", "كبش العيد", "ذبيحة"
            ),
            categoryName = "مناسبات وأعياد",
            subcategoryName = "عيد الأضحى",
            transactionType = "EXPENSE",
            seededCategoryTarget = "مناسبات وأعياد",
            seededSubcategoryTarget = "عيد الأضحى والأضحية"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "رمضان", "زلابية", "قلب اللوز", "ديول", "سحور", "قفة رمضان", "شربات", "فريك"
            ),
            categoryName = "مناسبات وأعياد",
            subcategoryName = "رمضان",
            transactionType = "EXPENSE",
            seededCategoryTarget = "مناسبات وأعياد",
            seededSubcategoryTarget = "رمضان وقفة رمضان"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "عيد الفطر", "عيد الصغير", "كسوة العيد", "قاطو العيد", "حلوى العيد"
            ),
            categoryName = "مناسبات وأعياد",
            subcategoryName = "عيد الفطر وكسوة العيد",
            transactionType = "EXPENSE",
            seededCategoryTarget = "مناسبات وأعياد",
            seededSubcategoryTarget = "عيد الفطر وكسوة العيد"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "عرس", "خطوبة", "مهيبة", "سبوع", "عقيقة", "ختان", "هدية عرس", "عيد ميلاد", "كادو", "هدية",
                "mariage", "cadeau", "anniversaire", "kado"
            ),
            categoryName = "مناسبات وأعياد",
            subcategoryName = "أعراس وهدايا ومهيبة",
            transactionType = "EXPENSE",
            seededCategoryTarget = "مناسبات وأعياد",
            seededSubcategoryTarget = "أعراس وهدايا ومهيبة"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "طابلي", "مئزر", "محفظة", "كراس", "كراريس", "كتب مدرسية", "الدخول المدرسي"
            ),
            categoryName = "مناسبات وأعياد",
            subcategoryName = "الدخول المدرسي",
            transactionType = "EXPENSE",
            seededCategoryTarget = "مناسبات وأعياد",
            seededSubcategoryTarget = "الدخول المدرسي"
        ),

        // 17. الخدمات البريدية (CCP) وبريدي موب والضرائب
        KeywordCategoryMapping(
            keywords = listOf(
                "ذهبية", "الذهبية", "تجديد الذهبية", "اشتراك الذهبية",
                "edahabia", "carte edahabia"
            ),
            categoryName = "الخدمات البريدية",
            subcategoryName = "البطاقة الذهبية",
            transactionType = "EXPENSE",
            seededCategoryTarget = "الخدمات البريدية (CCP)",
            seededSubcategoryTarget = "البطاقة الذهبية"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "عمولة بريدي موب", "عمولة تحويل", "حوالة بريدية", "موندات", "رسوم سحب",
                "baridimob", "frais virement"
            ),
            categoryName = "الخدمات البريدية",
            subcategoryName = "عمولات تحويل وبريدي موب",
            transactionType = "EXPENSE",
            seededCategoryTarget = "الخدمات البريدية (CCP)",
            seededSubcategoryTarget = "عمولات تحويل وبريدي موب"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "حقوق ccp", "سي سي بي", "دروا سي سي بي", "كارني شيك", "شيك", "اقتطاع البريد", "مسك الحساب", "بريد الجزائر",
                "algérie poste", "algerie poste", "tenue de compte"
            ),
            categoryName = "الخدمات البريدية",
            subcategoryName = "مستحقات البريد (CCP)",
            transactionType = "EXPENSE",
            seededCategoryTarget = "الخدمات البريدية (CCP)",
            seededSubcategoryTarget = "مستحقات البريد (CCP)"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "ضرائب", "قباضة", "كاسنوس", "اشتراك كاسنوس", "طابع جبائي", "تمبر", "جواز سفر", "بلدية", "توثيق", "نوتير",
                "casnos", "impots", "timbre", "notaire", "passeport"
            ),
            categoryName = "فواتير وشحن",
            subcategoryName = "ضرائب ورسوم إدارية (CNAS / CASNOS)",
            transactionType = "EXPENSE",
            seededCategoryTarget = "فواتير وشحن",
            seededSubcategoryTarget = "ضرائب ورسوم إدارية (CNAS / CASNOS)"
        ),

        // 18. ترفيه وألعاب واشتراكات رقمية
        KeywordCategoryMapping(
            keywords = listOf(
                "netflix", "spotify", "youtube premium", "chatgpt", "icloud", "shahid", "tod", "bein",
                "نتفلكس", "نتفليكس", "سبوتيفاي", "شاهد", "اشتراك شهري", "كانفا", "canva"
            ),
            categoryName = "فواتير وشحن",
            subcategoryName = "اشتراكات رقمية ومنصات",
            transactionType = "EXPENSE",
            seededCategoryTarget = "فواتير وشحن",
            seededSubcategoryTarget = "اشتراكات رقمية ومنصات"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "steam", "playstation", "ps5", "ps4", "xbox", "pubg", "free fire", "gaming",
                "العاب", "ألعاب", "ترفيه", "سينما", "بولينغ", "بلياردو"
            ),
            categoryName = "ترفيه",
            subcategoryName = "ألعاب",
            transactionType = "EXPENSE",
            seededCategoryTarget = "ترفيه وألعاب",
            seededSubcategoryTarget = "ألعاب وتسلية (Gaming)"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "نزهة", "تحويسة", "عطلة", "فندق", "اوتيل", "بحر", "تخييم", "حديقة التسلية",
                "hotel", "vacances", "tahwissa", "sortie", "plage"
            ),
            categoryName = "ترفيه",
            subcategoryName = "نزهات وعطل وسفر",
            transactionType = "EXPENSE",
            seededCategoryTarget = "ترفيه وألعاب",
            seededSubcategoryTarget = "نزهات وعطل وسفر"
        ),

        // 19. صدقة وزكاة وخير
        KeywordCategoryMapping(
            keywords = listOf(
                "زكاة", "زكاة المال", "زكاة الفطر", "فطرة",
                "zakat"
            ),
            categoryName = "صدقة وزكاة وخير",
            subcategoryName = "زكاة المال وزكاة الفطر",
            transactionType = "EXPENSE",
            seededCategoryTarget = "صدقة وزكاة وخير",
            seededSubcategoryTarget = "زكاة المال وزكاة الفطر"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "صدقة", "تبرع", "مسجد", "اعانة محتاج", "عمل خيري", "يتيم", "كفالة يتيم", "افطار صائم",
                "sadaqa", "sadaka", "don", "mosquee"
            ),
            categoryName = "صدقة وزكاة وخير",
            subcategoryName = "صدقة وتبرعات",
            transactionType = "EXPENSE",
            seededCategoryTarget = "صدقة وزكاة وخير",
            seededSubcategoryTarget = "صدقة وتبرعات"
        ),

        // 20. مشاريع وتجارة ومهنة (EXPENSE)
        KeywordCategoryMapping(
            keywords = listOf(
                "شراء سلعة", "سلعة بالجملة", "فورنيسور", "مورد", "امبالاج", "تغليف", "رأس مال",
                "marchandise", "fournisseur", "grossiste", "emballage", "stock"
            ),
            categoryName = "مشاريع وتجارة ومهنة",
            subcategoryName = "سلعة ورأس مال تجاري",
            transactionType = "EXPENSE",
            seededCategoryTarget = "مشاريع وتجارة ومهنة",
            seededSubcategoryTarget = "سلعة ورأس مال تجاري"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "ياليدين", "زاد ار اكسبريس", "شحن طلبيات", "مكتب التوصيل", "توصيل زبائن",
                "yalidine", "zr express", "maystro", "guepex", "ecotrack"
            ),
            categoryName = "مشاريع وتجارة ومهنة",
            subcategoryName = "شحن وتوصيل طلبيات (Yalidine / ZR)",
            transactionType = "EXPENSE",
            seededCategoryTarget = "مشاريع وتجارة ومهنة",
            seededSubcategoryTarget = "شحن وتوصيل طلبيات (Yalidine / ZR)"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "سبونسور", "اعلان ممول", "اعلانات فيسبوك", "اعلانات انستغرام", "تيك توك ادز", "تسويق",
                "sponsoring", "facebook ads", "meta ads", "tiktok ads"
            ),
            categoryName = "مشاريع وتجارة ومهنة",
            subcategoryName = "إعلانات ممولة وتسويق (Sponsoring)",
            transactionType = "EXPENSE",
            seededCategoryTarget = "مشاريع وتجارة ومهنة",
            seededSubcategoryTarget = "إعلانات ممولة وتسويق (Sponsoring)"
        ),

        // ═══════════════════════════════════════════════════════════════════════
        // INCOME MAPPINGS (المداخيل والإيرادات بالدارجة الجزائرية والفرنسية)
        // ═══════════════════════════════════════════════════════════════════════

        // 1. راتب ومردودية وتقاعد
        KeywordCategoryMapping(
            keywords = listOf(
                "مردودية", "بريم", "رابال", "منحة المردودية", "مكافأة العمل", "علاوة", "تعويض الخبرة",
                "prime", "rappel", "rendement", "pri", "bonus"
            ),
            categoryName = "راتب",
            subcategoryName = "مردودية ومنح العمل (Prime / Rappel)",
            transactionType = "INCOME",
            seededCategoryTarget = "راتب",
            seededSubcategoryTarget = "مردودية ومنح العمل (Prime / Rappel)"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "تقاعد", "معاش", "رترات", "صندوق التقاعد", "معاش التقاعد",
                "retraite", "cnr", "pension"
            ),
            categoryName = "راتب",
            subcategoryName = "تقاعد ومعاش (CNR / CASNOS)",
            transactionType = "INCOME",
            seededCategoryTarget = "راتب",
            seededSubcategoryTarget = "تقاعد ومعاش (CNR / CASNOS)"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "راتب", "شهرية", "خلصة", "لاباي", "سيرك", "الراتب", "الشهري", "اجر", "أجر", "صب الراتب", "دخلت الشهرية",
                "salary", "salaire", "paye", "chahria", "shahria", "khalsa", "virement salaire"
            ),
            categoryName = "راتب",
            subcategoryName = null,
            transactionType = "INCOME",
            seededCategoryTarget = "راتب",
            seededSubcategoryTarget = "راتب شهري (شهرية)"
        ),

        // 2. منح وتعويضات (البطالة، المنحة الجامعية، CNAS / شيفة)
        KeywordCategoryMapping(
            keywords = listOf(
                "منحة البطالة", "لانام", "منحة جامعية", "لابورس", "منحة الامومة", "منحة التمدرس", "منحة الاسرة", "منحة المجاهدين", "منحة",
                "allocation chomage", "chomage", "anem", "bourse", "minha"
            ),
            categoryName = "منح وتعويضات",
            subcategoryName = "منحة البطالة ومنحة جامعية",
            transactionType = "INCOME",
            seededCategoryTarget = "منح وتعويضات",
            seededSubcategoryTarget = "منحة البطالة ومنحة جامعية"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "تعويض كناص", "تعويض شيفة", "الضمان الاجتماعي", "استرجاع دواء", "تعويض طبي", "تعويض تامين",
                "cnas", "chifa", "remboursement cnas", "remboursement assurance"
            ),
            categoryName = "منح وتعويضات",
            subcategoryName = "تعويض الضمان الاجتماعي (CNAS / شيفة)",
            transactionType = "INCOME",
            seededCategoryTarget = "منح وتعويضات",
            seededSubcategoryTarget = "تعويض الضمان الاجتماعي (CNAS / شيفة)"
        ),

        // 3. مبيعات وتجارة
        KeywordCategoryMapping(
            keywords = listOf(
                "واد كنيس", "بيع مستعمل", "بعت هاتف", "بعت حاسوب", "بعت سيارة", "ماركت بليس",
                "ouedkniss", "marketplace", "occasion"
            ),
            categoryName = "مبيعات",
            subcategoryName = "بيع مستعمل (Ouedkniss / Marketplace)",
            transactionType = "INCOME",
            seededCategoryTarget = "مبيعات",
            seededSubcategoryTarget = "بيع مستعمل (Ouedkniss / Marketplace)"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "تحصيل ياليدين", "تحصيل طلبيات", "مبيعات الكترونية", "رمبورسمون ياليدين", "ريكوفيرمون",
                "recouvrement", "yalidine", "zr express", "e-commerce", "ecommerce"
            ),
            categoryName = "مبيعات",
            subcategoryName = "تجارة إلكترونية وتوصيل",
            transactionType = "INCOME",
            seededCategoryTarget = "مبيعات",
            seededSubcategoryTarget = "تجارة إلكترونية وتوصيل"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "مبيعات", "بيع", "بعت", "مدخول المحل", "روسيتا", "فائدة التجارة", "تجارة", "ربح بيع",
                "vente", "recette", "commerce", "benefice"
            ),
            categoryName = "مبيعات",
            subcategoryName = "تجارة وسلعة",
            transactionType = "INCOME",
            seededCategoryTarget = "مبيعات",
            seededSubcategoryTarget = "تجارة وسلعة"
        ),

        // 4. عمل إضافي وفريلانس وبريكول
        KeywordCategoryMapping(
            keywords = listOf(
                "فريلانس", "عمل حر", "تصميم", "برمجة", "مونتاج", "ترجمة", "مشروع عميل", "فايفر", "ابوورك", "خمسات", "مستقل",
                "freelance", "upwork", "fiverr", "design", "dev", "client"
            ),
            categoryName = "عمل إضافي",
            subcategoryName = "عمل حر وفريلانس (Freelance)",
            transactionType = "INCOME",
            seededCategoryTarget = "عمل إضافي",
            seededSubcategoryTarget = "عمل حر وفريلانس (Freelance)"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "بريكول", "نهارية", "خدمة يومية", "شغل اضافي", "ساعات اضافية", "عمولة", "كوميسيون", "وساطة", "سمسرة",
                "bricole", "heures sup", "commission", "khedma"
            ),
            categoryName = "عمل إضافي",
            subcategoryName = "بريكول وحرفة يومية",
            transactionType = "INCOME",
            seededCategoryTarget = "عمل إضافي",
            seededSubcategoryTarget = "بريكول وحرفة يومية"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "دروس خصوصية", "تدريس", "تكوين", "مدخول لي كور", "فوج تعليمي",
                "cours particuliers", "formation"
            ),
            categoryName = "عمل إضافي",
            subcategoryName = "دروس خصوصية وتكوين",
            transactionType = "INCOME",
            seededCategoryTarget = "عمل إضافي",
            seededSubcategoryTarget = "دروس خصوصية وتكوين"
        ),

        // 5. إيجار واستثمار (INCOME)
        KeywordCategoryMapping(
            keywords = listOf(
                "كراء", "كرية", "إيجار", "اجار", "مدخول الكراء", "كراء المحل", "كراء الشقة", "كراء الكاراج",
                "loyer", "kerya", "location"
            ),
            categoryName = "إيجار واستثمار",
            subcategoryName = "مدخول كراء عقار أو محل",
            transactionType = "INCOME",
            seededCategoryTarget = "إيجار واستثمار",
            seededSubcategoryTarget = "مدخول كراء عقار أو محل"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "استثمار", "ارباح", "أرباح", "شراكة", "عائد مشروع", "توزيع ارباح", "اسهم",
                "investissement", "dividende", "partenariat"
            ),
            categoryName = "إيجار واستثمار",
            subcategoryName = "أرباح شراكة ومشاريع",
            transactionType = "INCOME",
            seededCategoryTarget = "إيجار واستثمار",
            seededSubcategoryTarget = "أرباح شراكة ومشاريع"
        ),

        // 6. مكافآت وهدايا وحوالات واسترداد ديون (INCOME)
        KeywordCategoryMapping(
            keywords = listOf(
                "عيدية", "هدية", "كادو", "مهيبة", "جائزة", "مكافأة",
                "cadeau", "recompense"
            ),
            categoryName = "مكافآت وهدايا",
            subcategoryName = "عيدية وهدايا نقدية",
            transactionType = "INCOME",
            seededCategoryTarget = "مكافآت وهدايا",
            seededSubcategoryTarget = "عيدية وهدايا نقدية"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "حوالة", "موندات", "بعثولي", "تحويل عائلي", "مساعدة من الوالد", "ويسترن يونيون",
                "mandat", "western union", "transfert familial"
            ),
            categoryName = "مكافآت وهدايا",
            subcategoryName = "مساعدة عائلية وحوالة (Mandat)",
            transactionType = "INCOME",
            seededCategoryTarget = "مكافآت وهدايا",
            seededSubcategoryTarget = "مساعدة عائلية وحوالة (Mandat)"
        ),
        KeywordCategoryMapping(
            keywords = listOf(
                "استرداد دين", "رجعلي دين", "رجعلي سلف", "ردلي دراهمي", "تسديد سلف", "استرجاع امانة",
                "remboursement dette", "slaf"
            ),
            categoryName = "استرداد سلف وأمانات",
            subcategoryName = null,
            transactionType = "INCOME",
            seededCategoryTarget = "استرداد سلف وأمانات",
            seededSubcategoryTarget = null
        )
    )

    /**
     * Finds the highest-scoring Algerian keyword mapping for [normalizedTitle].
     * When [transactionType] ("EXPENSE" or "INCOME") is supplied, strictly restricts
     * matches to categories of that transaction type.
     */
    fun findMatchingCategory(
        normalizedTitle: String,
        transactionType: String? = null
    ): KeywordCategoryMapping? {
        if (normalizedTitle.isBlank()) return null
        val normalizedType = transactionType?.trim()?.uppercase()?.takeIf { it == "EXPENSE" || it == "INCOME" }
        val candidates = if (normalizedType != null) {
            mappings.filter { it.transactionType == normalizedType }
        } else {
            mappings
        }

        var bestMapping: KeywordCategoryMapping? = null
        var bestScore = 0
        for (mapping in candidates) {
            val maxKwScore = mapping.keywords.maxOfOrNull { kw ->
                matcher.scoreKeywordMatch(normalizedTitle, kw)
            } ?: 0
            if (maxKwScore > bestScore) {
                bestScore = maxKwScore
                bestMapping = mapping
            }
        }
        return bestMapping
    }
}
