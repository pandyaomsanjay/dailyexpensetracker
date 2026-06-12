package com.example.dailyexpensetracker

import android.app.DatePickerDialog
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dailyexpensetracker.databinding.ActivityReportsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var selectedStartDate = Calendar.getInstance()
    private var selectedEndDate = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar with back button
        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupSpinner()
        setupDatePickers()
        binding.btnGenerateReport.setOnClickListener { generateReport() }
    }

    private fun setupSpinner() {
        val reportTypes = arrayOf("Daily", "Weekly", "Monthly", "Custom Range")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, reportTypes)
        binding.spinnerReportType.adapter = adapter

        binding.spinnerReportType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val isCustom = (position == 3)
                binding.btnStartDate.isEnabled = isCustom
                binding.btnEndDate.isEnabled = isCustom
                if (!isCustom) {
                    when (position) {
                        0 -> { // Daily
                            selectedStartDate = Calendar.getInstance()
                            selectedEndDate = Calendar.getInstance()
                        }
                        1 -> { // Weekly (last 7 days)
                            selectedEndDate = Calendar.getInstance()
                            selectedStartDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
                        }
                        2 -> { // Monthly (last 30 days)
                            selectedEndDate = Calendar.getInstance()
                            selectedStartDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }
                        }
                    }
                    updateDateButtonTexts()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDatePickers() {
        binding.btnStartDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                selectedStartDate.set(year, month, day)
                updateDateButtonTexts()
            }, selectedStartDate.get(Calendar.YEAR), selectedStartDate.get(Calendar.MONTH), selectedStartDate.get(Calendar.DAY_OF_MONTH)).show()
        }
        binding.btnEndDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                selectedEndDate.set(year, month, day)
                updateDateButtonTexts()
            }, selectedEndDate.get(Calendar.YEAR), selectedEndDate.get(Calendar.MONTH), selectedEndDate.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun updateDateButtonTexts() {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.btnStartDate.text = format.format(selectedStartDate.time)
        binding.btnEndDate.text = format.format(selectedEndDate.time)
    }

    private fun generateReport() {
        val userId = auth.currentUser?.uid ?: return
        val start = com.google.firebase.Timestamp(selectedStartDate.time)
        val end = com.google.firebase.Timestamp(selectedEndDate.time)

        db.collection("transactions")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("date", start)
            .whereLessThanOrEqualTo("date", end)
            .get()
            .addOnSuccessListener { result ->
                val transactions = result.toObjects(Transaction::class.java)
                if (transactions.isEmpty()) {
                    Toast.makeText(this, "No transactions in this period", Toast.LENGTH_SHORT).show()
                } else {
                    showReportSummary(transactions)
                }
            }
    }

    private fun showReportSummary(transactions: List<Transaction>) {
        val totalIncome = transactions.filter { it.type == "Income" }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        val summary = """
            Report: ${binding.spinnerReportType.selectedItem}
            Period: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedStartDate.time)} to ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedEndDate.time)}
            Total Income: ₹$totalIncome
            Total Expense: ₹$totalExpense
            Net Balance: ₹$balance
        """.trimIndent()

        binding.tvReportSummary.text = summary
        binding.btnExportPdf.setOnClickListener {
            exportToPdf(transactions, totalIncome, totalExpense, balance)
        }
        binding.btnExportPdf.isEnabled = true
    }

    private fun exportToPdf(transactions: List<Transaction>, totalIncome: Double, totalExpense: Double, balance: Double) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint()
        paint.textSize = 12f
        var y = 50

        canvas.drawText("Expense Report", 50f, y.toFloat(), paint)
        y += 30
        canvas.drawText("Period: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedStartDate.time)} - ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedEndDate.time)}", 50f, y.toFloat(), paint)
        y += 30
        canvas.drawText("Total Income: ₹$totalIncome", 50f, y.toFloat(), paint)
        y += 30
        canvas.drawText("Total Expense: ₹$totalExpense", 50f, y.toFloat(), paint)
        y += 30
        canvas.drawText("Net Balance: ₹$balance", 50f, y.toFloat(), paint)
        y += 40
        canvas.drawText("Transactions:", 50f, y.toFloat(), paint)
        y += 30
        for (t in transactions) {
            val line = "${t.date.toDate()} | ${t.category} | ${t.type} | ₹${t.amount} | ${t.note}"
            canvas.drawText(line, 50f, y.toFloat(), paint)
            y += 20
            if (y > 800) break
        }

        pdfDocument.finishPage(page)

        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "report_${System.currentTimeMillis()}.pdf")
        try {
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            Toast.makeText(this, "PDF saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        pdfDocument.close()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
