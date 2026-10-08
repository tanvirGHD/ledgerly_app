package com.example.myapplication.repository

import android.util.Log
import com.example.myapplication.api.LedgerlyApiService
import com.example.myapplication.api.RetrofitInstance
import com.example.myapplication.model.AuthResponse
import com.example.myapplication.model.Budget
import com.example.myapplication.model.BudgetRequest
import com.example.myapplication.model.DashboardData
import com.example.myapplication.model.LoginRequest
import com.example.myapplication.model.RegisterRequest
import com.example.myapplication.model.ReportsData
import com.example.myapplication.model.Salary
import com.example.myapplication.model.SalaryRequest
import com.example.myapplication.model.Transaction
import com.example.myapplication.model.TransactionRequest
import com.example.myapplication.model.UpdatePasswordRequest
import com.example.myapplication.model.UpdateProfileRequest
import com.example.myapplication.model.User

// Repository managing API calls for Ledgerly
class LedgerlyRepository(
    private val api: LedgerlyApiService = RetrofitInstance.api
) {
    private val tag = "LedgerlyRepository"

    // Auth
    suspend fun register(req: RegisterRequest): AuthResponse {
        return try {
            api.register(req)
        } catch (e: Exception) {
            Log.e(tag, "register error: ${e.message}", e)
            throw e
        }
    }

    suspend fun login(req: LoginRequest): AuthResponse {
        return try {
            api.login(req)
        } catch (e: Exception) {
            Log.e(tag, "login error: ${e.message}", e)
            throw e
        }
    }

    suspend fun getMe(): User = api.getMe()

    suspend fun updateProfile(req: UpdateProfileRequest): User =
        api.updateProfile(req)

    suspend fun updatePassword(req: UpdatePasswordRequest): Map<String, String> =
        api.updatePassword(req)

    // Transactions
    suspend fun getTransactions(
        type: String? = null,
        category: String? = null,
        search: String? = null,
        from: String? = null,
        to: String? = null
    ): List<Transaction> = api.getTransactions(type, category, search, from, to)

    suspend fun createTransaction(req: TransactionRequest): Transaction =
        api.createTransaction(req)

    suspend fun updateTransaction(id: String, req: TransactionRequest): Transaction =
        api.updateTransaction(id, req)

    suspend fun deleteTransaction(id: String) =
        api.deleteTransaction(id)

    // Salary
    suspend fun getSalaries(): List<Salary> = api.getSalaries()

    suspend fun createSalary(req: SalaryRequest): Salary =
        api.createSalary(req)

    suspend fun updateSalary(id: String, req: SalaryRequest): Salary =
        api.updateSalary(id, req)

    suspend fun deleteSalary(id: String) =
        api.deleteSalary(id)

    // Budget
    suspend fun getBudgets(month: String? = null): List<Budget> =
        api.getBudgets(month)

    suspend fun upsertBudget(req: BudgetRequest): Budget =
        api.upsertBudget(req)

    suspend fun deleteBudget(id: String) =
        api.deleteBudget(id)

    // Dashboard & Reports
    suspend fun getDashboard(): DashboardData = api.getDashboard()

    suspend fun getReports(range: String = "30d"): ReportsData =
        api.getReports(range)
}
