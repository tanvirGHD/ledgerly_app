package com.example.myapplication.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.TokenStore
import com.example.myapplication.model.Budget
import com.example.myapplication.model.BudgetRequest
import com.example.myapplication.model.CategoryBreakdown
import com.example.myapplication.model.DashboardData
import com.example.myapplication.model.MonthStat
import com.example.myapplication.model.ReportsData
import com.example.myapplication.model.Salary
import com.example.myapplication.model.SalaryRequest
import com.example.myapplication.model.Transaction
import com.example.myapplication.model.TransactionRequest
import com.example.myapplication.repository.LedgerlyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.coroutineScope

class LedgerlyViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: LedgerlyRepository = LedgerlyRepository()

    // Shared StateFlow Cache across all tabs
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _salaries = MutableStateFlow<List<Salary>>(emptyList())
    val salaries: StateFlow<List<Salary>> = _salaries.asStateFlow()

    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()

    private val _dashboard = MutableStateFlow<DashboardData?>(null)
    val dashboard: StateFlow<DashboardData?> = _dashboard.asStateFlow()

    private val _reports = MutableStateFlow<ReportsData?>(null)
    val reports: StateFlow<ReportsData?> = _reports.asStateFlow()

    // Loading & Feedback
    val isInitialLoading = MutableStateFlow(true)
    val isRefreshing = MutableStateFlow(false)
    val snackbarMessage = MutableStateFlow<String?>(null)

    // Filters for Transactions screen
    val selectedType = MutableStateFlow("All")
    val selectedCategory = MutableStateFlow("All")
    val searchQuery = MutableStateFlow("")

    val budgetMonth = MutableStateFlow(getCurrentMonth())
    val reportRange = MutableStateFlow("7d")
    val chartRange = MutableStateFlow("6m") // "1d", "1w", "1m", "6m", "1y"

    fun getChartDataForRange(range: String): List<MonthStat> {
        val allTxs = _transactions.value
        val cal = Calendar.getInstance()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val result = mutableListOf<MonthStat>()

        when (range) {
            "1d" -> {
                val todayStr = getTodayDate()
                var inc = 0.0
                var exp = 0.0
                allTxs.filter { it.date == todayStr }.forEach {
                    if (it.type.equals("income", ignoreCase = true)) inc += it.amount
                    else if (it.type.equals("expense", ignoreCase = true)) exp += it.amount
                }
                result.add(MonthStat("Today", inc, exp))
            }
            "1w" -> {
                for (i in 6 downTo 0) {
                    val tempCal = Calendar.getInstance()
                    tempCal.add(Calendar.DAY_OF_YEAR, -i)
                    val dStr = format.format(tempCal.time)
                    val label = SimpleDateFormat("EEE", Locale.getDefault()).format(tempCal.time)

                    var inc = 0.0
                    var exp = 0.0
                    allTxs.filter { it.date == dStr }.forEach {
                        if (it.type.equals("income", ignoreCase = true)) inc += it.amount
                        else if (it.type.equals("expense", ignoreCase = true)) exp += it.amount
                    }
                    result.add(MonthStat(label, inc, exp))
                }
            }
            "1m" -> {
                for (i in 3 downTo 0) {
                    val tempCal = Calendar.getInstance()
                    tempCal.add(Calendar.WEEK_OF_MONTH, -i)
                    val label = "W${4 - i}"

                    var inc = 0.0
                    var exp = 0.0
                    result.add(MonthStat(label, inc, exp))
                }
            }
            "6m" -> {
                val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                for (i in 5 downTo 0) {
                    val tempCal = cal.clone() as Calendar
                    tempCal.add(Calendar.MONTH, -i)
                    val mStr = monthFormat.format(tempCal.time)
                    val label = monthNames[tempCal.get(Calendar.MONTH)]

                    var inc = 0.0
                    var exp = 0.0
                    allTxs.filter { it.date.startsWith(mStr) }.forEach {
                        if (it.type.equals("income", ignoreCase = true)) inc += it.amount
                        else if (it.type.equals("expense", ignoreCase = true)) exp += it.amount
                    }
                    result.add(MonthStat(label, inc, exp))
                }
            }
            "1y" -> {
                val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                for (i in 11 downTo 0) {
                    val tempCal = cal.clone() as Calendar
                    tempCal.add(Calendar.MONTH, -i)
                    val mStr = monthFormat.format(tempCal.time)
                    val label = monthNames[tempCal.get(Calendar.MONTH)]

                    var inc = 0.0
                    var exp = 0.0
                    allTxs.filter { it.date.startsWith(mStr) }.forEach {
                        if (it.type.equals("income", ignoreCase = true)) inc += it.amount
                        else if (it.type.equals("expense", ignoreCase = true)) exp += it.amount
                    }
                    result.add(MonthStat(label, inc, exp))
                }
            }
        }
        return result
    }

    val categories = listOf("All", "Food", "Transport", "Rent", "Medical", "Shopping", "Bills", "Salary", "Others")
    val methods = listOf("card", "cash", "bank", "mobile", "other")

    init {
        loadDataInParallel(isInitial = true)
    }

    // Parallel Initial & Silent Refresh Loading
    fun loadDataInParallel(isInitial: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isInitial && _transactions.value.isEmpty()) {
                isInitialLoading.value = true
            } else {
                isRefreshing.value = true
            }

            try {
                coroutineScope {
                    val tJob = async { repository.getTransactions() }
                    val sJob = async { repository.getSalaries() }
                    val bJob = async { repository.getBudgets(budgetMonth.value) }
                    val dJob = async { repository.getDashboard() }
                    val rJob = async { repository.getReports(reportRange.value) }

                    _transactions.value = tJob.await()
                    _salaries.value = sJob.await()
                    _budgets.value = bJob.await()
                    _dashboard.value = dJob.await()
                    _reports.value = rJob.await()
                }
            } catch (e: Exception) {
                Log.e("LedgerlyVM", "Parallel load error: ${e.message}", e)
                if (isInitial) {
                    snackbarMessage.value = "Cannot connect to server. Please check if backend is running."
                }
            } finally {
                isInitialLoading.value = false
                isRefreshing.value = false
            }
        }
    }

    // Local Dashboard Calculation (Runs on Dispatchers.Default for Instant Updates)
    private suspend fun recalculateDashboardLocally() = withContext(Dispatchers.Default) {
        val allTxs = _transactions.value
        val allSals = _salaries.value
        val currentM = getCurrentMonth()

        var balance = 0.0
        var monthInc = 0.0
        var monthExp = 0.0

        allTxs.forEach { tx ->
            if (tx.type.equals("income", ignoreCase = true)) balance += tx.amount
            else if (tx.type.equals("expense", ignoreCase = true)) balance -= tx.amount

            if (tx.date.startsWith(currentM)) {
                if (tx.type.equals("income", ignoreCase = true)) monthInc += tx.amount
                else if (tx.type.equals("expense", ignoreCase = true)) monthExp += tx.amount
            }
        }

        val remaining = monthInc - monthExp
        val monthSalary = allSals.filter { it.month == currentM }
            .sumOf { it.amount + it.bonus - it.deduction }

        // Last 6 Months Calculation
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val cal = Calendar.getInstance()
        val last6 = mutableListOf<MonthStat>()

        for (i in 5 downTo 0) {
            val tempCal = cal.clone() as Calendar
            tempCal.add(Calendar.MONTH, -i)
            val mStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(tempCal.time)
            val label = monthNames[tempCal.get(Calendar.MONTH)]

            var inc = 0.0
            var exp = 0.0
            allTxs.filter { it.date.startsWith(mStr) }.forEach {
                if (it.type.equals("income", ignoreCase = true)) inc += it.amount
                else if (it.type.equals("expense", ignoreCase = true)) exp += it.amount
            }
            last6.add(MonthStat(label, inc, exp))
        }

        // Category Breakdown
        val catMap = mutableMapOf<String, Double>()
        allTxs.filter { it.type.equals("expense", ignoreCase = true) && it.date.startsWith(currentM) }
            .forEach { catMap[it.category] = (catMap[it.category] ?: 0.0) + it.amount }

        val categoryBreakdown = catMap.map { CategoryBreakdown(it.key, it.value) }
        val recent = allTxs.take(5)

        _dashboard.value = DashboardData(
            balance = balance,
            monthIncome = monthInc,
            monthExpense = monthExp,
            remaining = remaining,
            monthlySalary = monthSalary,
            last6Months = last6,
            categoryBreakdown = categoryBreakdown,
            recent = recent
        )
    }

    // OPTIMISTIC UPDATE: Add Transaction
    fun addTransaction(
        type: String,
        amount: Double,
        category: String,
        method: String,
        date: String,
        note: String
    ) {
        val tempId = "temp_${UUID.randomUUID()}"
        val tempTx = Transaction(
            id = tempId,
            type = type,
            amount = amount,
            category = category,
            method = method,
            date = date,
            note = note
        )

        // 1. Instant local update
        val updatedList = listOf(tempTx) + _transactions.value
        _transactions.value = updatedList

        viewModelScope.launch {
            recalculateDashboardLocally()

            // 2. Background Network Request
            try {
                val realTx = repository.createTransaction(TransactionRequest(type, amount, category, note, method, date))
                // Replace temp item with real item from server
                _transactions.value = _transactions.value.map { if (it.id == tempId) realTx else it }
                recalculateDashboardLocally()
                snackbarMessage.value = "Transaction added successfully!"
            } catch (e: Exception) {
                // Rollback on failure
                _transactions.value = _transactions.value.filter { it.id != tempId }
                recalculateDashboardLocally()
                snackbarMessage.value = "Failed to save transaction: ${e.localizedMessage}"
            }
        }
    }

    // OPTIMISTIC UPDATE: Update Transaction
    fun updateTransaction(
        id: String,
        type: String,
        amount: Double,
        category: String,
        method: String,
        date: String,
        note: String
    ) {
        val oldList = _transactions.value
        val updatedTx = Transaction(id = id, type = type, amount = amount, category = category, method = method, date = date, note = note)

        // 1. Instant local update
        _transactions.value = oldList.map { if (it.id == id) updatedTx else it }

        viewModelScope.launch {
            recalculateDashboardLocally()

            try {
                repository.updateTransaction(id, TransactionRequest(type, amount, category, note, method, date))
                snackbarMessage.value = "Transaction updated!"
            } catch (e: Exception) {
                // Rollback
                _transactions.value = oldList
                recalculateDashboardLocally()
                snackbarMessage.value = "Failed to update: ${e.localizedMessage}"
            }
        }
    }

    // OPTIMISTIC UPDATE: Delete Transaction
    fun deleteTransaction(transaction: Transaction) {
        val oldList = _transactions.value
        _transactions.value = oldList.filter { it.id != transaction.id }

        viewModelScope.launch {
            recalculateDashboardLocally()

            try {
                transaction.id?.let { repository.deleteTransaction(it) }
                snackbarMessage.value = "Transaction deleted!"
            } catch (e: Exception) {
                // Rollback
                _transactions.value = oldList
                recalculateDashboardLocally()
                snackbarMessage.value = "Failed to delete: ${e.localizedMessage}"
            }
        }
    }

    // OPTIMISTIC UPDATE: Add Salary
    fun addSalary(source: String, amount: Double, bonus: Double, deduction: Double, month: String, note: String) {
        val tempId = "temp_${UUID.randomUUID()}"
        val tempSal = Salary(id = tempId, source = source, amount = amount, bonus = bonus, deduction = deduction, month = month, note = note)

        val oldList = _salaries.value
        _salaries.value = listOf(tempSal) + oldList

        viewModelScope.launch {
            recalculateDashboardLocally()
            try {
                val realSal = repository.createSalary(SalaryRequest(source, amount, bonus, deduction, month, note))
                _salaries.value = _salaries.value.map { if (it.id == tempId) realSal else it }
                recalculateDashboardLocally()
                snackbarMessage.value = "Salary record added!"
            } catch (e: Exception) {
                _salaries.value = oldList
                recalculateDashboardLocally()
                snackbarMessage.value = "Failed to add salary: ${e.localizedMessage}"
            }
        }
    }

    // OPTIMISTIC UPDATE: Update Salary
    fun updateSalary(id: String, source: String, amount: Double, bonus: Double, deduction: Double, month: String, note: String) {
        val oldList = _salaries.value
        val updatedSal = Salary(id = id, source = source, amount = amount, bonus = bonus, deduction = deduction, month = month, note = note)

        _salaries.value = oldList.map { if (it.id == id) updatedSal else it }

        viewModelScope.launch {
            recalculateDashboardLocally()
            try {
                repository.updateSalary(id, SalaryRequest(source, amount, bonus, deduction, month, note))
                snackbarMessage.value = "Salary record updated!"
            } catch (e: Exception) {
                _salaries.value = oldList
                recalculateDashboardLocally()
                snackbarMessage.value = "Failed to update salary: ${e.localizedMessage}"
            }
        }
    }

    // OPTIMISTIC UPDATE: Delete Salary
    fun deleteSalary(salary: Salary) {
        val oldList = _salaries.value
        _salaries.value = oldList.filter { it.id != salary.id }

        viewModelScope.launch {
            recalculateDashboardLocally()
            try {
                salary.id?.let { repository.deleteSalary(it) }
                snackbarMessage.value = "Salary record deleted!"
            } catch (e: Exception) {
                _salaries.value = oldList
                recalculateDashboardLocally()
                snackbarMessage.value = "Failed to delete salary: ${e.localizedMessage}"
            }
        }
    }

    // OPTIMISTIC UPDATE: Save Budget
    fun saveBudget(category: String, limit: Double) {
        val currentM = budgetMonth.value
        val tempId = "temp_${UUID.randomUUID()}"
        val tempB = Budget(id = tempId, category = category, month = currentM, limit = limit, spent = 0.0)

        val oldList = _budgets.value
        _budgets.value = _budgets.value.filter { !(it.category == category && it.month == currentM) } + tempB

        viewModelScope.launch {
            try {
                val realB = repository.upsertBudget(BudgetRequest(category, currentM, limit))
                _budgets.value = _budgets.value.map { if (it.category == category && it.month == currentM) realB else it }
                snackbarMessage.value = "Budget saved!"
            } catch (e: Exception) {
                _budgets.value = oldList
                snackbarMessage.value = "Failed to save budget: ${e.localizedMessage}"
            }
        }
    }

    // OPTIMISTIC UPDATE: Delete Budget
    fun deleteBudget(budget: Budget) {
        val oldList = _budgets.value
        _budgets.value = oldList.filter { it.id != budget.id }

        viewModelScope.launch {
            try {
                budget.id?.let { repository.deleteBudget(it) }
                snackbarMessage.value = "Budget rule deleted!"
            } catch (e: Exception) {
                _budgets.value = oldList
                snackbarMessage.value = "Failed to delete budget: ${e.localizedMessage}"
            }
        }
    }

    // Reports Refresh
    fun fetchReports() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = repository.getReports(reportRange.value)
                _reports.value = data
            } catch (e: Exception) {
                Log.e("LedgerlyVM", "fetchReports error: ${e.message}")
            }
        }
    }

    // CSV Export
    fun exportTransactionsCsv(context: Context) {
        val list = _transactions.value
        if (list.isNotEmpty()) {
            val sb = StringBuilder()
            sb.append("Date,Type,Category,Amount,Method,Note\n")
            list.forEach { tx ->
                sb.append("${tx.date},${tx.type},${tx.category},${tx.amount},${tx.method},\"${tx.note ?: ""}\"\n")
            }

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, sb.toString())
                type = "text/csv"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Transactions CSV"))
        }
    }

    // Date Helpers
    fun getTodayDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    fun getCurrentMonth(): String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
}
