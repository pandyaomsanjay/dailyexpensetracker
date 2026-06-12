package com.example.dailyexpensetracker

import android.graphics.Color
import android.os.Bundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.dailyexpensetracker.databinding.ActivityAnalyticsBinding
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalyticsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        loadAllData()

        binding.bottomNavigation.selectedItemId = R.id.nav_analytics
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
                R.id.nav_analytics -> true
                R.id.nav_budget -> {
                    startActivity(Intent(this, BudgetActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadAllData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("transactions")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val transactions = result.toObjects(Transaction::class.java)
                showPieChart(transactions.filter { it.type == "Expense" })
                showMonthlyExpenseChart(transactions.filter { it.type == "Expense" })
                showIncomeExpenseBarChart(transactions)
            }
    }

    private fun showPieChart(expenses: List<Transaction>) {
        val categoryMap = expenses.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }
        val entries = categoryMap.map { PieEntry(it.value.toFloat(), it.key) }
        val dataSet = PieDataSet(entries, "Spending by Category")
        dataSet.colors = listOf(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.GRAY)
        binding.pieChart.data = PieData(dataSet)
        binding.pieChart.invalidate()
    }

    private fun showMonthlyExpenseChart(expenses: List<Transaction>) {
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val monthlyMap = expenses.groupBy { monthFormat.format(it.date.toDate()) }
            .mapValues { it.value.sumOf { t -> t.amount } }
            .toSortedMap(compareBy { monthFormat.parse(it) })
        val entries = monthlyMap.values.mapIndexed { index, amount -> Entry(index.toFloat(), amount.toFloat()) }
        val dataSet = LineDataSet(entries, "Monthly Expenses")
        dataSet.color = Color.RED
        dataSet.setCircleColor(Color.RED)
        binding.lineChart.data = LineData(dataSet)
        binding.lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(monthlyMap.keys.toList())
        binding.lineChart.invalidate()
    }

    private fun showIncomeExpenseBarChart(transactions: List<Transaction>) {
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val grouped = transactions.groupBy { monthFormat.format(it.date.toDate()) }
        val months = grouped.keys.toList().sortedBy { monthFormat.parse(it) }
        val incomeEntries = months.mapIndexed { index, month ->
            val income = grouped[month]?.filter { it.type == "Income" }?.sumOf { it.amount } ?: 0.0
            BarEntry(index.toFloat(), income.toFloat())
        }
        val expenseEntries = months.mapIndexed { index, month ->
            val expense = grouped[month]?.filter { it.type == "Expense" }?.sumOf { it.amount } ?: 0.0
            BarEntry(index.toFloat(), expense.toFloat())
        }
        val incomeDataSet = BarDataSet(incomeEntries, "Income")
        incomeDataSet.color = Color.GREEN
        val expenseDataSet = BarDataSet(expenseEntries, "Expense")
        expenseDataSet.color = Color.RED
        val barData = BarData(incomeDataSet, expenseDataSet)
        binding.barChart.data = barData
        binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(months)
        binding.barChart.invalidate()
    }
}