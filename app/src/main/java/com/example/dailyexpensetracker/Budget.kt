package com.example.dailyexpensetracker

data class Budget(
    val userId: String = "",
    val monthYear: String = "",
    val amount: Double = 0.0
)