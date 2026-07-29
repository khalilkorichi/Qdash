package com.qdash.domain.model

enum class MarketType(val labelArabic: String) {
    PARALLEL("السوق الموازي"),
    OFFICIAL("السوق الرسمي")
}

enum class TradeDirection(val labelArabic: String) {
    BUY("شراء"),
    SELL("بيع")
}
