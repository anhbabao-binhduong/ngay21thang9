package com.example.myapplication

data class Expense(
    val id: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val category: String = "",
    val date: String = "",
    val monthYear: String = "",
    val timestamp: Long = 0
)
