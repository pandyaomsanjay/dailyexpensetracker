package com.example.dailyexpensetracker

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.dailyexpensetracker.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        prefs = getSharedPreferences("app_settings", MODE_PRIVATE)

        loadSettings()
        setupListeners()

        // Fix: Use R.id.nav_settings directly and ensure bottomNavigation is accessed via binding
        binding.bottomNavigation.selectedItemId = R.id.nav_settings
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
                R.id.nav_budget -> {
                    startActivity(Intent(this, BudgetActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> true
                else -> false
            }
        }
    }

    private fun loadSettings() {
        // Dark Mode
        val isDark = prefs.getBoolean("dark_mode", false)
        binding.switchDarkMode.isChecked = isDark
        if (isDark) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Currency Spinner
        val currencies = arrayOf("₹ (INR)", "$ (USD)", "€ (EUR)", "£ (GBP)", "¥ (JPY)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        binding.spinnerCurrency.adapter = adapter
        val savedCurrency = prefs.getString("currency", "₹ (INR)")
        val pos = currencies.indexOf(savedCurrency)
        if (pos >= 0) binding.spinnerCurrency.setSelection(pos)

        // Notification Switches
        binding.switchDailyReminder.isChecked = prefs.getBoolean("daily_reminder", true)
        binding.switchMonthlyReminder.isChecked = prefs.getBoolean("monthly_reminder", true)
    }

    private fun setupListeners() {
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            if (isChecked) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        binding.spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val currency = parent?.getItemAtPosition(position).toString()
                prefs.edit().putString("currency", currency).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        binding.switchDailyReminder.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("daily_reminder", isChecked).apply()
            if (isChecked) ReminderScheduler(this).scheduleDailyReminder()
        }

        binding.switchMonthlyReminder.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("monthly_reminder", isChecked).apply()
            if (isChecked) ReminderScheduler(this).scheduleMonthlyBudgetReminder()
        }

        binding.btnReports.setOnClickListener {
            // Ensure ReportsActivity exists or handles the intent
            try {
                startActivity(Intent(this, ReportsActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Reports feature not available", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBackup.setOnClickListener { backupData() }
        binding.btnRestore.setOnClickListener { restoreData() }
        binding.btnLogout.setOnClickListener { logout() }
        binding.btnDeleteAccount.setOnClickListener { deleteAccount() }
    }

    private fun backupData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("transactions").whereEqualTo("userId", userId).get()
            .addOnSuccessListener { transactions ->
                db.collection("budgets").whereEqualTo("userId", userId).get()
                    .addOnSuccessListener { budgets ->
                        val backupMap = mapOf(
                            "transactions" to transactions.toObjects(Transaction::class.java),
                            "budgets" to budgets.toObjects(Budget::class.java)
                        )
                        val json = Gson().toJson(backupMap)
                        val file = File(getExternalFilesDir(null), "backup_${System.currentTimeMillis()}.json")
                        FileOutputStream(file).use { it.write(json.toByteArray()) }
                        Toast.makeText(this, "Backup saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
                    }
            }
    }

    private fun restoreData() {
        Toast.makeText(this, "Restore feature: select a JSON backup file", Toast.LENGTH_SHORT).show()
    }


    private fun logout() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun deleteAccount() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure? This action is permanent.")
            .setPositiveButton("Yes") { _, _ ->
                auth.currentUser?.delete()
                    ?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }
}
