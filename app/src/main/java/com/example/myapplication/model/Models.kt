package com.example.myapplication.model

import com.google.gson.annotations.SerializedName

// User Model
data class User(
    val id: String? = null,
    val name: String,
    val email: String,
    val currency: String = "BDT"
)

// Auth Requests and Responses
data class AuthResponse(
    val token: String,
    val user: User
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class UpdateProfileRequest(
    val name: String,
    val currency: String = "BDT"
)

data class UpdatePasswordRequest(
    val newPassword: String
)

// Transaction Models
data class Transaction(
    @SerializedName("_id")
    val id: String? = null,
    val userId: String? = null,
    val type: String, // "income" or "expense"
    val amount: Double,
    val category: String,
    val note: String? = "",
    val method: String = "cash", // "card", "cash", "bank", "mobile", "other"
    val date: String // yyyy-MM-dd
)

data class TransactionRequest(
    val type: String,
    val amount: Double,
    val category: String,
    val note: String? = "",
    val method: String = "cash",
    val date: String
)

// Salary Models
data class Salary(
    @SerializedName("_id")
    val id: String? = null,
    val userId: String? = null,
    val source: String,
    val amount: Double,
    val bonus: Double = 0.0,
    val deduction: Double = 0.0,
    val month: String, // yyyy-MM
    val note: String? = ""
)

data class SalaryRequest(
    val source: String,
    val amount: Double,
    val bonus: Double = 0.0,
    val deduction: Double = 0.0,
    val month: String,
    val note: String? = ""
)

// Budget Models
data class Budget(
    @SerializedName("_id")
    val id: String? = null,
    val category: String, // "Overall" or category name
    val month: String, // yyyy-MM
    val limit: Double,
    val spent: Double = 0.0
)

data class BudgetRequest(
    val category: String,
    val month: String,
    val limit: Double
)

// Dashboard Models
data class DashboardData(
    val balance: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val remaining: Double = 0.0,
    val monthlySalary: Double = 0.0,
    val last6Months: List<MonthStat> = emptyList(),
    val categoryBreakdown: List<CategoryBreakdown> = emptyList(),
    val recent: List<Transaction> = emptyList()
)

data class MonthStat(
    val label: String,
    val income: Double,
    val expense: Double
)

data class CategoryBreakdown(
    val category: String,
    val total: Double
)

// Reports Models
data class ReportsData(
    val series: List<SeriesPoint> = emptyList(),
    val byCategory: List<CategoryBreakdown> = emptyList()
)

data class SeriesPoint(
    val label: String,
    val income: Double,
    val expense: Double
)
