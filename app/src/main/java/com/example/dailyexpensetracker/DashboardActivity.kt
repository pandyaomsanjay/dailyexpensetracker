package com.example.dailyexpensetracker

import android.app.AlarmManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailyexpensetracker.databinding.ActivityDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: TransactionAdapter
    private val transactionList = mutableListOf<Transaction>()
    private lateinit var prefs: SharedPreferences

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
        if (prefs.getBoolean("dark_mode", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        requestNotificationPermission()

        val email = auth.currentUser?.email ?: "User"
        val name = email.substringBefore("@")
        binding.tvWelcome.text = "Good ${getTimeGreeting()},\n$name 👋"

        setupRecyclerView()
        loadRecentTransactions()
        calculateBalance()
        loadBudgetInfo()
        initReminders()

        binding.btnAddExpense.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }
        binding.btnAddIncome.setOnClickListener {
            startActivity(Intent(this, AddIncomeActivity::class.java))
        }

        binding.bottomNavigation.selectedItemId = R.id.nav_dashboard
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 1, 0, "Test Firestore")
            ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            testFirestoreWrite()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun testFirestoreWrite() {
        val testDoc = hashMapOf(
            "test" to "value",
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("test").add(testDoc)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Test write OK", Toast.LENGTH_SHORT).show()
                db.collection("test").limit(1).get()
                    .addOnSuccessListener { snap ->
                        if (!snap.isEmpty) {
                            Toast.makeText(this, "✅ Read also works", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Read failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Test failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun getTimeGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..11 -> "Morning"
            in 12..16 -> "Afternoon"
            else -> "Evening"
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(transactionList) {}
        binding.rvRecentTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvRecentTransactions.adapter = adapter
    }

    private fun loadRecentTransactions() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("transactions")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val tempList = mutableListOf<Transaction>()
                for (doc in snapshot.documents) {
                    val t = doc.toObject(Transaction::class.java)
                    t?.documentId = doc.id
                    t?.let { tempList.add(it) }
                }
                // Sort locally to avoid index requirement
                tempList.sortByDescending { it.date.toDate() }
                transactionList.clear()
                transactionList.addAll(tempList.take(5))
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Failed to load transactions", e)
            }
    }

    private fun calculateBalance() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("transactions").whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("Dashboard", "Balance calculation failed", e)
                    return@addSnapshotListener
                }
                var totalIncome = 0.0
                var totalExpense = 0.0
                snapshot?.forEach { doc ->
                    val t = doc.toObject(Transaction::class.java)
                    if (t.type == "Income") totalIncome += t.amount
                    else totalExpense += t.amount
                }
                val balance = totalIncome - totalExpense
                binding.tvBalance.text = "₹$balance"
                binding.tvMonthlyIncome.text = "₹$totalIncome"
                binding.tvMonthlyExpense.text = "₹$totalExpense"
            }
    }

    private fun loadBudgetInfo() {
        val userId = auth.currentUser?.uid ?: return
        val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        db.collection("budgets")
            .whereEqualTo("userId", userId)
            .whereEqualTo("monthYear", monthYear)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val budget = docs.documents[0].toObject(Budget::class.java)
                    if (budget != null && budget.amount > 0) {
                        updateBudgetCard(budget.amount)
                    } else {
                        binding.tvBudgetRemaining.text = "No budget set"
                        binding.budgetProgressBar.progress = 0
                    }
                } else {
                    binding.tvBudgetRemaining.text = "No budget set"
                    binding.budgetProgressBar.progress = 0
                }
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Load budget failed", e)
            }
    }

    private fun updateBudgetCard(budgetAmount: Double) {
        val userId = auth.currentUser?.uid ?: return
        val (start, end) = getCurrentMonthRange()
        
        // Use a simple query on userId only to avoid needing a composite index for (type, userId, date)
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
                binding.tvBudgetRemaining.text = "Remaining: ₹$remaining"
                val percent = if (budgetAmount > 0) ((totalExpense / budgetAmount) * 100).toInt() else 0
                binding.budgetProgressBar.progress = percent.coerceIn(0, 100)

                if (percent >= 80 && percent < 100) {
                    Toast.makeText(this, "Budget warning: You have used $percent% of your budget!", Toast.LENGTH_LONG).show()
                } else if (percent >= 100) {
                    Toast.makeText(this, "Budget exceeded! You have overspent.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Budget update failed", e)
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

    private fun initReminders() {
        val dailyEnabled = prefs.getBoolean("daily_reminder", true)
        val monthlyEnabled = prefs.getBoolean("monthly_reminder", true)
        if (dailyEnabled || monthlyEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = getSystemService(AlarmManager::class.java)
                if (!alarmManager.canScheduleExactAlarms()) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(intent)
                    return
                }
            }
            if (dailyEnabled) ReminderScheduler(this).scheduleDailyReminder()
            if (monthlyEnabled) ReminderScheduler(this).scheduleMonthlyBudgetReminder()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) initReminders()
    }
}