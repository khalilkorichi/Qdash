package com.qdash.domain.model

sealed interface CardAiContextType {
    object DonutChart : CardAiContextType
    object BarChart : CardAiContextType
    object MonthComparison : CardAiContextType
    object SavingsProgress : CardAiContextType
    object EmergencyFund : CardAiContextType
    object SalaryCycle : CardAiContextType
    object WeekendWeekday : CardAiContextType
    object CashFlowTrend : CardAiContextType
    object DashboardOverview : CardAiContextType
}

data class CardAiContext(
    val cardId: String,
    val cardTitle: String,
    val cardType: CardAiContextType,
    val chartData: String, // serialized chart data (e.g. JSON or key-value list)
    val periodStart: Long,
    val periodEnd: Long,
    val tooltipContent: String
)
