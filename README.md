# 💰 Daily Expense Tracker – Android App

A modern, feature‑rich personal finance manager built with **Kotlin**, **Firebase**, and **Material Design 3**.  
Track your income & expenses, set budgets, visualize spending, and export reports – all in one beautiful app.

---

## 📱 Overview

Daily Expense Tracker helps you take control of your money.  
With a clean dashboard, real‑time balance updates, category‑wise spending analytics, and smart reminders, it turns financial tracking into a seamless daily habit.

Whether you're a student, professional, or family, this app makes personal finance simple, intuitive, and productive.

---

## ✨ Key Features

### 🔐 Authentication
- Sign up / Login with email & password
- Secure user sessions (Firebase Auth)
- Logout & account deletion

### 📊 Dashboard
- **Total Balance** (Income – Expense)
- Monthly **Income** & **Expense** summary
- **Budget** progress card with remaining amount
- **Recent Transactions** (last 5)
- Quick **Add Expense** / **Add Income** buttons

### 💸 Expenses
- Add, edit, delete expenses
- Category selection via **chips** (Food, Transport, Shopping, Entertainment, Bills, Healthcare, Other)
- Optional note & date picker

### 💵 Income
- Add, edit, delete incomes
- Income source chips (Salary, Business, Gift, Refund, Investment, Other)
- Optional note & date picker

### 📜 Transaction History
- Full list of all transactions
- **Search** by note or category
- **Filter** by category
- **Sort** by date (ascending / descending)
- Edit amount or delete transactions

### 📈 Analytics
- **Pie chart** – expense breakdown by category
- **Line chart** – monthly expenses trend
- **Bar chart** – income vs expense per month

### 🎯 Budget Management
- Set a **monthly budget** (e.g., ₹20,000)
- Track **remaining** amount in real time
- Visual **progress bar**
- Warnings at **80%** usage and **overspending** alerts

### 📄 Reports
- Generate reports for **Daily**, **Weekly**, **Monthly**, or **Custom Range**
- Summary shows total income, expense, and net balance
- Export report as **PDF** (saved to device)

### 🔔 Reminders
- **Daily expense reminder** at 7 PM (reminds you to log expenses)
- **Monthly budget reminder** on the 28th (check your budget progress)
- Permission handling for exact alarms (Android 12+) and notifications (Android 13+)

### ⚙️ Settings
- **Dark Mode** toggle (applies system‑wide)
- **Currency** selection (INR, USD, EUR, GBP, JPY)
- Enable/disable **daily** and **monthly** reminders
- **Backup** data to JSON (transactions + budgets)
- **Restore** data (coming soon)
- **Logout** & **Delete Account**

### 🎨 Modern UI/UX
- **Material Design 3** with rounded cards, soft shadows, and smooth animations
- **Inter** font for a premium look
- **Gradient** cards and accent colours
- **Bottom navigation** (Home, History, Stats, Budget, Profile) on all main screens
- **Toolbar** with back button on auxiliary screens

---

## 🛠️ Tech Stack

| Component               | Technology / Library                 |
| ----------------------- | ------------------------------------ |
| Language                | Kotlin                               |
| IDE                     | Android Studio (latest)              |
| Architecture            | MVC / Activity‑based                 |
| Backend & Database      | Firebase Firestore (NoSQL)           |
| Authentication          | Firebase Authentication (email/pass) |
| Charts                  | MPAndroidChart (v3.1.0)              |
| UI Components           | Material Design 3 (com.google.android.material) |
| Networking              | Firebase SDK                         |
| Local Storage           | SharedPreferences (settings)         |
| Adapter                 | RecyclerView + custom adapter        |
| Date/Time               | java.util.Calendar + Timestamp       |
| PDF Generation          | android.graphics.pdf.PdfDocument     |
| JSON Handling           | Gson (for backup)                    |
| Font                    | Inter (bundled)                      |

---

## 📂 Project Structure

```
app/src/main/java/com/example/dailyexpensetracker/
├── MainActivity.kt                   (Splash screen)
├── LoginActivity.kt
├── SignupActivity.kt
├── DashboardActivity.kt
├── AddExpenseActivity.kt
├── AddIncomeActivity.kt
├── TransactionHistoryActivity.kt
├── AnalyticsActivity.kt
├── BudgetActivity.kt
├── ReportsActivity.kt
├── SettingsActivity.kt
├── Transaction.kt                   (data class)
├── Budget.kt                        (data class)
├── TransactionAdapter.kt
├── NotificationHelper.kt
├── ReminderScheduler.kt
├── NotificationReceiver.kt
└── (all in one package, no sub‑packages)

res/
├── layout/                          (all activity layouts + toolbar.xml + item_transaction.xml)
├── drawable/                         (gradient, icons, avatar background)
├── menu/                             (bottom_nav_menu.xml)
├── values/                           (colors, themes, strings)
├── font/                             (Inter fonts)
└── xml/                              (backup rules, data extraction rules)
```

---

## 🚀 Installation & Setup

### 1. Clone the Repository
```bash
git clone https://github.com/pandyaomsanjay/dailyexpensetracker.git
cd dailyexpensetracker
```

### 2. Open in Android Studio
- Open Android Studio → **Open an existing project**
- Select the project folder

### 3. Firebase Configuration
- Go to [Firebase Console](https://console.firebase.google.com/)
- Create a new project (or use an existing one)
- **Add an Android app** – package name must be `com.example.dailyexpensetracker`
- Download `google-services.json` and place it in the **`app/`** folder
- Enable **Email/Password** authentication in Firebase Console
- Create a **Firestore Database** (start in **test mode** for development)

### 4. Build & Run
- Click **Sync Now** in Android Studio (if prompted)
- Connect an Android device or start an emulator
- Press **Run** ▶️

> ⚠️ **Important**: If you change the package name, update it in Firebase as well.

---

## 🔧 Build Configuration

The app uses **Gradle** (Kotlin DSL). Key dependencies are listed below – ensure your `app/build.gradle` matches:

```kotlin
plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    compileSdk = 36
    // ...
    buildFeatures { viewBinding = true }
}

dependencies {
    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Material Design & AndroidX
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // ... (testing etc.)
}
```

---

## 📱 Permissions

The app requests the following permissions at runtime:

- **POST_NOTIFICATIONS** (Android 13+) – to show reminder notifications
- **SCHEDULE_EXACT_ALARM** (Android 12+) – to schedule exact‑time reminders (fallback to inexact if denied)

They are declared in `AndroidManifest.xml`.

---

## 🧪 Testing & Debugging

- Use the **Test Firestore** menu item (top‑right on Dashboard) to verify Firestore connectivity.
- All Firestore operations include error logging and user‑friendly Toast messages.

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is open‑source and available under the **MIT License**.  
Feel free to use, modify, and distribute it for personal or commercial purposes.

---

## ⭐ Support

If you find this project useful, please give it a ⭐ on GitHub – it helps others discover it too!

---

### Daily Expense Tracker

**Track Smart. Save More. Grow Better. 💰📈**
