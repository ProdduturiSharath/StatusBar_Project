package com.sharath.statusbartotal.core

enum class BudgetLevel { OK, WARN, OVER }

object Budget {
    /** Over means strictly above the budget. */
    fun isOver(total: Long, budget: Long): Boolean = total > budget

    /** Amber from 80% of the budget, red when over. */
    fun level(total: Long, budget: Long): BudgetLevel = when {
        total > budget -> BudgetLevel.OVER
        total * 100 >= budget * 80 -> BudgetLevel.WARN
        else -> BudgetLevel.OK
    }

    /** Progress bar fill, 0.0 to 1.0. */
    fun fraction(total: Long, budget: Long): Float =
        if (budget <= 0) 0f else (total.toDouble() / budget).coerceIn(0.0, 1.0).toFloat()

    /** Home hero line. */
    fun heroLine(total: Long, budget: Long): String =
        if (total > budget)
            "${RupeeFormat.exact(total)} of ${RupeeFormat.exact(budget)} · " +
                "${RupeeFormat.exact(total - budget)} over budget"
        else
            "${RupeeFormat.exact(total)} of ${RupeeFormat.exact(budget)} · " +
                "${RupeeFormat.exact(budget - total)} left"

    /** First notification line when there is no message. */
    fun notificationLine(total: Long, budget: Long): String =
        if (total > budget) "${RupeeFormat.exact(total - budget)} over budget"
        else "${RupeeFormat.exact(budget - total)} left of ${RupeeFormat.exact(budget)}"

    /** Title of the over-budget alert. */
    fun alertText(total: Long, budget: Long): String =
        "Over budget: ${RupeeFormat.exact(total)} of ${RupeeFormat.exact(budget)}"

    /** Note key: one alert per cycle and budget amount, so changing the budget re-arms it. */
    fun alertKey(cycleKey: Int, budget: Long): String = "budget@$cycleKey:$budget"
}
