package com.example.dailyexpensetracker

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dailyexpensetracker.databinding.ActivityAddIncomeBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AddIncomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddIncomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var selectedDate = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddIncomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.chipSalary.isChecked = true

        binding.etDate.setOnClickListener { showDatePicker() }

        binding.btnSave.setOnClickListener {
            val amountStr = binding.etAmount.text.toString()
            val amount = amountStr.toDoubleOrNull()
            val note = binding.etNote.text.toString()

            if (amount == null || amount <= 0.0) {
                Toast.makeText(this, "Enter a valid positive amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedSource = when (binding.chipGroupIncomeSource.checkedChipId) {
                R.id.chipSalary -> "Salary"
                R.id.chipBusiness -> "Business"
                R.id.chipGift -> "Gift"
                R.id.chipRefund -> "Refund"
                R.id.chipInvestment -> "Investment"
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
                type = "Income",
                category = selectedSource,
                note = note,
                date = Timestamp(selectedDate.time),
                createdAt = Timestamp.now()
            )

            db.collection("transactions").add(transaction)
                .addOnSuccessListener {
                    Toast.makeText(this, "Income added", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e("AddIncome", "Firestore write failed", e)
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