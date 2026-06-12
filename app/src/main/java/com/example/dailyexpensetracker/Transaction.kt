package com.example.dailyexpensetracker

import com.google.firebase.Timestamp

data class Transaction(
    var documentId: String = "",   // Firestore document ID
    val userId: String = "",
    val amount: Double = 0.0,
    val type: String = "",          // "Income" or "Expense"
    val category: String = "",
    val note: String = "",
    val date: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now()
)