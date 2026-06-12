package com.example.dailyexpensetracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dailyexpensetracker.databinding.ActivityBudgetBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class BudgetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        loadCurrentBudget()
        binding.btnSaveBudget.setOnClickListener { saveBudget() }

        // Bottom navigation
        binding.bottomNavigation.selectedItemId = R.id.nav_budget
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, TransactionHistoryActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_analytics -> {
                    startActivity(Intent(this, AnalyticsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_budget -> true
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadCurrentBudget() {
        val userId = auth.currentUser?.uid ?: return
        val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        db.collection("budgets")
            .whereEqualTo("userId", userId)
            .whereEqualTo("monthYear", monthYear)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val budget = docs.documents[0].toObject(Budget::class.java)
                    budget?.amount?.let {
                        binding.etBudgetAmount.setText(it.toString())
                        updateBudgetProgress(it)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Budget", "Load budget failed", e)
                Toast.makeText(this, "Could not load budget: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveBudget() {
        val amountStr = binding.etBudgetAmount.text.toString()
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Enter a valid positive budget amount", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = auth.currentUser?.uid
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val budget = Budget(userId, monthYear, amount)

        db.collection("budgets").document("${userId}_$monthYear").set(budget)
            .addOnSuccessListener {
                Toast.makeText(this, "Budget saved", Toast.LENGTH_SHORT).show()
                updateBudgetProgress(amount)
            }
            .addOnFailureListener { e ->
                Log.e("Budget", "Save budget failed", e)
                Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // FIX: Simplified Firestore query to avoid index requirements. Filtering by type and date is now done locally.
    private fun updateBudgetProgress(budgetAmount: Double) {
        val userId = auth.currentUser?.uid ?: return
        val (start, end) = getCurrentMonthRange()
        db.collection("transactions")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val totalExpense = result.documents.mapNotNull { doc ->
                    doc.toObject(Transaction::class.java)
                }.filter { 
                    it.type == "Expense" && 
                    it.date.toDate().time >= start.toDate().time && 
                    it.date.toDate().time <= end.toDate().time 
                }.sumOf { it.amount }

                val remaining = budgetAmount - totalExpense
                binding.tvRemainingBudget.text = "Remaining: ₹$remaining"
                val percent = if (budgetAmount > 0) ((totalExpense / budgetAmount) * 100).toInt() else 0
                binding.progressBudget.progress = percent.coerceIn(0, 100)

                if (percent >= 80 && percent < 100) {
                    Toast.makeText(this, "Warning: You have used $percent% of your budget!", Toast.LENGTH_LONG).show()
                } else if (percent >= 100) {
                    Toast.makeText(this, "Overspending! You have exceeded your budget.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Budget", "Failed to calculate expenses", e)
            }
    }

    private fun getCurrentMonthRange(): Pair<com.google.firebase.Timestamp, com.google.firebase.Timestamp> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val start = com.google.firebase.Timestamp(cal.time)
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.SECOND, -1)
        val end = com.google.firebase.Timestamp(cal.time)
        return Pair(start, end)
    }
}