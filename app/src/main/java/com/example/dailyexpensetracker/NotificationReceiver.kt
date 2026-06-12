package com.example.dailyexpensetracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val helper = NotificationHelper(context)
        when (intent?.action) {
            "DAILY_REMINDER" -> helper.showDailyReminder()
            "MONTHLY_REMINDER" -> helper.showBudgetReminder()
            "BILL_REMINDER" -> {
                val billName = intent.getStringExtra("bill_name") ?: "Bill"
                helper.showBillReminder(billName)
            }
        }
    }
}