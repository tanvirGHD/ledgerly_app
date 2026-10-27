# Ledgerly App Overview & Architecture Guide

Ledgerly is a modern Personal Finance Tracker Android application built with **Kotlin**, **Jetpack Compose**, **Retrofit + OkHttp**, and **Coroutines / StateFlow**, backed by a **Node.js, Express, & MongoDB** REST API.

---

## 1. All Pages & Screens

The application consists of the following primary screens and authentication flows:
1. **Login Page** (`LoginScreen`)
2. **Sign Up Page** (`SignupScreen`)
3. **Main App Scaffold** (`MainScaffoldScreen` - container hosting the bottom navigation bar and top app bar)
4. **Dashboard Tab** (`DashboardScreen`)
5. **Transactions Tab** (`TransactionsScreen`)
6. **Salary Tab** (`SalaryScreen`)
7. **Budget Tab** (`BudgetScreen`)
8. **Reports Tab** (`ReportsScreen`)
9. **Settings Tab** (`SettingsScreen`)

---

## 2. Folder Structure & File Paths

All UI screens and components are organized within the `app` module under `com.example.myapplication`:

### UI Screens (`app/src/main/java/com/example/myapplication/ui/screens/`)
- [LoginScreen.kt](file:///C:/Tanvir/Apps/ledgerly_app/app/src/main/java/com/example/myapplication/ui/screens/LoginScreen.kt)
- [SignupScreen.kt](file:///C:/Tanvir/Apps/ledgerly_app/app/src/main/java/com/example/myapplication/ui/screens/SignupScreen.kt)
- [MainScaffoldScreen.kt](file:///C:/Tanvir/Apps/ledgerly_app/app/src/main/java/com/example/myapplication/ui/screens/MainScaffoldScreen.kt)
- [DashboardScreen.kt](file:///C:/Tanvir/Apps/ledgerly_app/app/src/main/java/com/example/myapplication/ui/screens/DashboardScreen.kt)
- [TransactionsScreen.kt](file:///C:/Tanvir/Apps/ledgerly_app/app/src/main/java/com/example/myapplication/ui/screens/TransactionsScreen.kt)
- [SalaryScreen.kt](file:///C:/Tanvir/Apps/ledgerly_app/app/src/main/java/com/example/myapplication/ui/screens/SalaryScreen.kt)
- [BudgetScreen.kt](file:///C:/Tanvir/Apps/ledgerly_app/app/src/main/java/com/example/myapplication/ui/screens/BudgetScreen.kt)
- [ReportsScreen.kt](file:///C:/Tanvir/Apps/ledgerly_app/app/src/main/java/com/example/myapplication/ui/screens/ReportsScreen.kt)
- [SettingsScreen.kt](file:///C:/Tanvir/Apps/ledgerly_app/app/src/main/java/com/example/myapplication/ui/screens/SettingsScreen.kt)

### UI Components (`app/src/main/java/com/example/myapplication/ui/components/`)
- [Charts.kt](file:///C:/Tanvir/Apps/ledgerly_app/app/src/main/java/com/example/myapplication/ui/components/Charts.kt) - Custom Compose charts for financial trends and category breakdowns.
- [Shimmer.kt](file:///C:/Tanvir/Apps/ledgerly_app/app/src/main/java/com/example/myapplication/ui/components/Shimmer.kt) - Loading placeholders.

### ViewModels (`app/src/main/java/com/example/myapplication/viewmodel/`)
- [AuthViewModel.kt](file:///C:/Tanvir/Apps/ledgerly_app/app/src/main/java/com/example/myapplication/viewmodel/AuthViewModel.kt) - Manages user authentication, registration, profile, and theme state.
- [LedgerlyViewModel.kt](file:///C:/Tanvir/Apps/ledgerly_app/app/src/main/java/com/example/myapplication/viewmodel/LedgerlyViewModel.kt) - Manages financial data (transactions, salaries, budgets, dashboard, reports) with offline caching and optimistic updates.

### Networking & Data (`app/src/main/java/com/example/myapplication/api/` & `data/`)
- [LedgerlyApiService.kt](file:///C:/Tanvir/Apps/ledgerly_app/app/src/main/java/com/example/myapplication/api/LedgerlyApiService.kt) - Retrofit interface and OkHttp client with Bearer token header injection.
- [TokenStore.kt](file:///C:/Tanvir/Apps/ledgerly_app/app/src/main/java/com/example/myapplication/data/local/TokenStore.kt) - SharedPreferences wrapper for persistent JWT tokens and user settings.

---

## 3. Tab Functionality & Navigation

Once logged in, users navigate through the bottom navigation bar in [MainScaffoldScreen.kt](file:///C:/Tanvir/Apps/ledgerly_app/app/src/main/java/com/example/myapplication/ui/screens/MainScaffoldScreen.kt):

1. **Dashboard Tab** (`DashboardScreen`)
   - **Purpose**: Financial command center providing an instant overview of total net balance, monthly income, monthly expenses, remaining budget, and monthly salary.
   - **Features**: Displays recent transactions, category breakdown charts, and a 6-month historical trend.

2. **Transactions Tab** (`TransactionsScreen`)
   - **Purpose**: Comprehensive log of all income and expense entries.
   - **Features**: Search bar, filters (All, Income, Expense, Category), floating action button (FAB) to add new transactions, edit existing transactions, and swipe-to-delete functionality.

3. **Salary Tab** (`SalaryScreen`)
   - **Purpose**: Manage monthly earnings, sources, bonuses, and deductions.
   - **Features**: Calculate net salary per month (`amount + bonus - deduction`) and add/edit/delete salary entries.

4. **Budget Tab** (`BudgetScreen`)
   - **Purpose**: Control spending by setting category-wise budget limits for any given month.
   - **Features**: Visual progress bars showing spent vs. limit for each category with real-time updates.

5. **Reports Tab** (`ReportsScreen`)
   - **Purpose**: Detailed visual analytics and graphical reports.
   - **Features**: Filterable time ranges (7d, 4w, 6m, year) showing line/bar charts for income/expense trends and category distribution.

6. **Settings Tab** (`SettingsScreen`)
   - **Purpose**: Account management, personalization, and security.
   - **Features**: Update user profile name, select currency symbol (BDT, USD, EUR, etc.), toggle Dark/Light mode, change password, and log out.

---

## 4. How Everything Works

### Architecture & Data Flow
- **Declarative UI**: Built entirely with Jetpack Compose following Material 3 guidelines.
- **Reactive State**: ViewModels expose `StateFlow` properties. UI components collect these flows (`collectAsState()`) to react instantly to state changes.
- **Backend Communication**: Retrofit connects to the Node.js/Express backend running on MongoDB. All authenticated endpoints automatically attach the JWT Bearer token via OkHttp interceptors.
- **Optimistic Updates**: When adding, updating, or deleting items (transactions, salaries, budgets), Ledgerly updates the UI instantly and performs the network request in the background, rolling back gracefully if an error occurs.
