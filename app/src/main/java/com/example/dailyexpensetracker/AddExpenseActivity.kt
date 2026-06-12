package com.example.dailyexpensetracker

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dailyexpensetracker.databinding.ActivityAddExpenseBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var selectedDate = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Default chip: Food
        binding.chipFood.isChecked = true

        binding.etDate.setOnClickListener { showDatePicker() }

        binding.btnSave.setOnClickListener {
            val amountStr = binding.etAmount.text.toString()
            val amount = amountStr.toDoubleOrNull()
            val note = binding.etNote.text.toString()

            if (amount == null || amount <= 0.0) {
                Toast.makeText(this, "Enter a valid positive amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedCategory = when (binding.chipGroupCategory.checkedChipId) {
                R.id.chipFood -> "Food"
                R.id.chipTransport -> "Transport"
                R.id.chipShopping -> "Shopping"
                R.id.chipEntertainment -> "Entertainment"
                R.id.chipBills -> "Bills"
                R.id.chipHealthcare -> "Healthcare"
                else -> "Other"
            }

            val userId = auth.currentUser?.uid
            if (userId.isNullOrEmpty()) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val transaction = Transaction(
                userId = userId,
                amount = amount,
                type = "Expense",
                category = selectedCategory,
                note = note,
                date = Timestamp(selectedDate.time),
                createdAt = Timestamp.now()
            )

            db.collection("transactions").add(transaction)
                .addOnSuccessListener {
                    Toast.makeText(this, "Expense added", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e("AddExpense", "Firestore write failed", e)
                    Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate.set(year, month, day)
                binding.etDate.setText("$day/${month + 1}/$year")
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}