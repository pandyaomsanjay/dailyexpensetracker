package com.example.dailyexpensetracker

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailyexpensetracker.databinding.ActivityTransactionHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TransactionHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionHistoryBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: TransactionAdapter
    private val allTransactions = mutableListOf<Transaction>()
    private var currentFilterCategory = "All"
    private var currentSortAscending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        adapter = TransactionAdapter(allTransactions) { transaction -> showEditDeleteDialog(transaction) }
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = adapter

        setupFilterSpinner()
        setupSearchView()
        setupSortButton()
        loadAllTransactions()

        binding.bottomNavigation.selectedItemId = R.id.nav_history
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_history -> true
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

    private fun setupFilterSpinner() {
        val categories = listOf("All", "Food", "Transport", "Shopping", "Entertainment", "Bills", "Healthcare", "Other", "Salary", "Business", "Gift", "Refund", "Investment")
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFilterCategory.adapter = adapterSpinner
        binding.spinnerFilterCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentFilterCategory = categories[position]
                applyFilterAndSort(binding.searchView.query.toString())
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                applyFilterAndSort(newText ?: "")
                return true
            }
        })
    }

    private fun setupSortButton() {
        binding.btnSortDate.setOnClickListener {
            currentSortAscending = !currentSortAscending
            binding.btnSortDate.text = if (currentSortAscending) "Sort ↑" else "Sort ↓"
            applyFilterAndSort(binding.searchView.query.toString())
        }
    }

    private fun loadAllTransactions() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("transactions")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    allTransactions.clear()
                    for (doc in snapshot.documents) {
                        val t = doc.toObject(Transaction::class.java)
                        t?.documentId = doc.id
                        t?.let { allTransactions.add(it) }
                    }
                    applyFilterAndSort(binding.searchView.query.toString())
                }
            }
    }

    private fun applyFilterAndSort(searchText: String = "") {
        var filtered = allTransactions.toList()
        if (currentFilterCategory != "All") filtered = filtered.filter { it.category == currentFilterCategory }
        if (searchText.isNotEmpty()) {
            filtered = filtered.filter { it.note.contains(searchText, true) || it.category.contains(searchText, true) }
        }
        filtered = if (currentSortAscending) filtered.sortedBy { it.date.toDate() } else filtered.sortedByDescending { it.date.toDate() }
        adapter.updateList(filtered)
    }

    private fun showEditDeleteDialog(transaction: Transaction) {
        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(this)
            .setTitle("Transaction")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editTransaction(transaction)
                    1 -> deleteTransaction(transaction)
                }
            }
            .show()
    }

    private fun editTransaction(transaction: Transaction) {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.setText(transaction.amount.toString())
        AlertDialog.Builder(this)
            .setTitle("Edit Amount")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newAmount = input.text.toString().toDoubleOrNull()
                if (newAmount != null && newAmount > 0 && transaction.documentId.isNotEmpty()) {
                    db.collection("transactions").document(transaction.documentId)
                        .update("amount", newAmount)
                        .addOnSuccessListener { Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show() }
                        .addOnFailureListener { Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show() }
                } else {
                    Toast.makeText(this, "Invalid amount or missing ID", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTransaction(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Are you sure?")
            .setPositiveButton("Yes") { _, _ ->
                if (transaction.documentId.isNotEmpty()) {
                    db.collection("transactions").document(transaction.documentId).delete()
                        .addOnSuccessListener { Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show() }
                        .addOnFailureListener { Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show() }
                } else {
                    Toast.makeText(this, "Document ID missing", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}