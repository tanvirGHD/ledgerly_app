package com.example.myapplication.model

import com.google.gson.annotations.SerializedName

// Model representing an expense item from MongoDB
data class Expense(
    @SerializedName("_id")
    val id: String? = null,
    val title: String,
    val amount: Double,
    val category: String,
    val date: String,
    val note: String? = ""
)

// Request body model for creating or updating an expense (without _id)
data class ExpenseRequest(
    val title: String,
    val amount: Double,
    val category: String,
    val date: String,
    val note: String? = ""
)
