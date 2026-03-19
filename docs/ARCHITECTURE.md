# Architecture Documentation

This document describes the architecture of the Finance Tracker Android app.

## Overview

The app follows **MVVM (Model-View-ViewModel)** combined with **Clean Architecture** principles, ensuring separation of concerns, testability, and maintainability.

## Architecture Layers

```
┌─────────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                           │
│  ┌─────────────────┐    ┌─────────────────────────────────────┐ │
│  │  Compose UI     │◄───│  ViewModels (StateFlow)             │ │
│  │  (Screens)      │    │  - DashboardViewModel               │ │
│  └─────────────────┘    │  - LedgerViewModel                  │ │
│                         │  - WalletViewModel                  │ │
│                         │  - InsightsViewModel                │ │
│                         │  - NotificationViewModel            │ │
│                         │  - GroupViewModel                   │ │
│                         └─────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│                      DOMAIN LAYER                               │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  Repository Interfaces    │  Business Logic                ││
│  │  - FinanceRepository      │  - SubscriptionLogic           ││
│  │  - TransactionRepository  │  - SmsParser                   ││
│  │  - UserPrefsRepository    │  - Domain Models               ││
│  │  - NotificationRepository │                                ││
│  │  - GroupRepository        │                                ││
│  └─────────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────────┤
│                       DATA LAYER                                │
│  ┌───────────────────┐  ┌───────────────────┐  ┌─────────────┐ │
│  │  Repository Impl  │  │  Room Database    │  │  DataStore  │ │
│  │  - FinanceRepoImpl│  │  - Entities       │  │  - Prefs    │ │
│  │  - TransactionImpl│  │  - DAOs           │  └─────────────┘ │
│  │  - GroupRepoImpl  │  │  - Mappers        │                  │
│  └───────────────────┘  └───────────────────┘                  │
└─────────────────────────────────────────────────────────────────┘
```

## Layer Responsibilities

### Presentation Layer
**Location**: `presentation/`

- **Screens**: Jetpack Compose UI components
- **ViewModels**: Hold UI state as `StateFlow`, handle user actions
- **UI Models**: Display-ready data with formatted strings

**Key Principles**:
- ViewModels don't know about Compose
- UI observes StateFlow and recomposes automatically
- No business logic in UI code

### Domain Layer
**Location**: `domain/`

- **Repository Interfaces**: Define contracts for data operations
- **Business Logic**: SMS parsing, subscription detection
- **Domain Models**: Core data structures

**Key Principles**:
- Pure Kotlin, no Android dependencies
- Defines what the app does, not how
- Repository interfaces, not implementations

### Data Layer
**Location**: `data/`

- **Repository Implementations**: Concrete data operations
- **Room Database**: SQLite with type-safe queries
- **DataStore**: User preferences storage
- **Mappers**: Entity to domain model conversion

**Key Principles**:
- Single source of truth
- Handles data persistence and retrieval
- Abstracts data sources from domain layer

---

## Dependency Injection (Hilt)

### Module Structure

```
di/
├── AppModule.kt           # WorkManager
├── DatabaseModule.kt      # Room database, DAOs
├── RepositoryModule.kt    # Repository bindings
├── CoroutinesModule.kt    # Application-scoped coroutines
└── ApplicationScope.kt    # Custom qualifier annotation
```

### DatabaseModule
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(app: Application): AppDatabase {
        return Room.databaseBuilder(app, AppDatabase::class.java, "finance_tracker.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideFinanceDao(db: AppDatabase): FinanceDao = db.financeDao()
}
```

### RepositoryModule
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindFinanceRepository(impl: FinanceRepositoryImpl): FinanceRepository

    @Binds
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    abstract fun bindUserPrefsRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository

    @Binds
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    abstract fun bindGroupRepository(impl: GroupRepositoryImpl): GroupRepository
}
```

---

## Navigation

The app uses **Jetpack Compose Navigation** with a single-activity architecture.

### Navigation Graph

```
MainActivity
    └── MainScreen (NavHost)
            ├── HomeScreen ──────────► AddTransactionSheet
            ├── LedgerScreen ────────► EditTransactionSheet / GroupDetailScreen
            ├── WalletScreen ────────► AddWalletSheet / AddPersonSheet
            ├── InsightsScreen ──────► AddBudgetSheet / AddSubscriptionSheet
            ├── ProfileScreen ───────► CurrencyPickerSheet
            ├── CategoryListScreen
            ├── NotificationScreen
            ├── UserProfileScreen
            └── GroupDetailScreen ───► AddGroupExpenseSheet / SettleUpSheet / AddContributionSheet
```

### Route Constants
```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Ledger : Screen("ledger")
    object Wallet : Screen("wallet")
    object Insights : Screen("insights")
    object Profile : Screen("profile")
    object Categories : Screen("categories")
    object Notifications : Screen("notifications")
    object GroupDetail : Screen("groupDetail/{groupId}")
}
```

### Bottom Navigation
- 4 main tabs: Home, Ledger, Wallet, Insights
- Floating Action Button (FAB) overlays center for quick transaction entry
- Profile accessible from Home screen header

---

## State Management

### Pattern: Unidirectional Data Flow (UDF)

```
┌──────────────────────────────────────────────────────────┐
│                     ViewModel                            │
│  ┌─────────────────────────────────────────────────────┐ │
│  │                                                     │ │
│  │   Repository Flows ──► combine() ──► StateFlow     │ │
│  │                                                     │ │
│  └─────────────────────────────────────────────────────┘ │
│              ▲                           │               │
│              │                           ▼               │
│         User Events              UI State (immutable)    │
│              │                           │               │
└──────────────┼───────────────────────────┼───────────────┘
               │                           │
               │                           ▼
         ┌─────┴─────┐              ┌─────────────┐
         │  Compose  │◄─────────────│  Recompose  │
         │    UI     │              │             │
         └───────────┘              └─────────────┘
```

### StateFlow Example

```kotlin
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: FinanceRepository,
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getNetWorth(),
        repository.getMonthlyCashFlow(),
        repository.getAllWallets(),
        repository.getRecentTransactions(),
        userPrefs.currency
    ) { netWorth, cashFlow, wallets, transactions, currency ->
        DashboardUiState(
            netWorth = netWorth.formatAsCurrency(currency),
            // ... map other fields
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}
```

### UI State Classes

```kotlin
data class DashboardUiState(
    val netWorth: String = "₹0",
    val netWorthChange: String = "+0%",
    val monthlyIncome: String = "₹0",
    val monthlyExpense: String = "₹0",
    val accounts: List<AccountUiModel> = emptyList(),
    val recentTransactions: List<TransactionUiModel> = emptyList(),
    val isLoading: Boolean = false
)
```

---

## Data Flow

### Complete Flow: Adding a Transaction

```
1. User Input (AddTransactionSheet)
   └── User fills form, selects wallet, clicks "Add"
           │
           ▼
2. ViewModel (DashboardViewModel)
   └── addTransaction(amount, category, type, walletName, ...)
       └── Resolves wallet by name: wallets.find { it.name == walletName }
           │
           ▼
3. Repository (FinanceRepositoryImpl)
   └── database.withTransaction {
           │
           ├── dao.insertTransaction(entity)
           ├── dao.updateWalletBalance(walletId, -amount)  // for expense
           └── dao.updatePersonBalance(personId, amount)   // if linked
       }
           │
           ▼
4. Database (Room)
   └── Transaction committed, triggers Flow emission
           │
           ▼
5. Flow Updates
   └── dao.getRecentTransactions() emits new list
           │
           ▼
6. ViewModel StateFlow
   └── combine() recalculates, emits new UiState
           │
           ▼
7. Compose UI
   └── Collects StateFlow, recomposes with new data
```

### Reactive Data Pipeline

```kotlin
// DAO Layer - Returns Flow
@Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT 5")
fun getRecentTransactions(): Flow<List<TransactionEntity>>

// Repository Layer - Transforms and combines
override fun getRecentTransactions(): Flow<List<TransactionUiModel>> {
    return combine(
        financeDao.getRecentTransactions(),
        userPrefs.currency
    ) { entities, currency ->
        entities.map { it.toTransactionUiModel(currency) }
    }
}

// ViewModel Layer - Exposes as StateFlow
val transactions: StateFlow<List<TransactionUiModel>> =
    repository.getRecentTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

// UI Layer - Observes
@Composable
fun TransactionList(viewModel: DashboardViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    LazyColumn {
        items(transactions) { transaction ->
            TransactionItem(transaction)
        }
    }
}
```

---

## Background Processing

### WorkManager for SMS Sync

```kotlin
@HiltWorker
class SmsSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: FinanceRepository,
    private val smsParser: SmsParserEngine
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val smsMessages = readSmsMessages()
        val transactions = smsMessages.mapNotNull { smsParser.parse(it) }
        repository.batchInsertTransactions(transactions)
        return Result.success()
    }
}
```

### Periodic SMS Sync Worker

```kotlin
@HiltWorker
class PeriodicSmsSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val financeRepository: FinanceRepository,
    private val notificationRepository: NotificationRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Check if periodic sync is enabled
        if (!userPreferencesRepository.isPeriodicSyncEnabled.first()) {
            return Result.success()
        }
        // Check Privacy Mode
        if (userPreferencesRepository.isPrivacyMode.first()) {
            return Result.success()
        }
        // Sync SMS and generate notifications
        financeRepository.syncTransactions(lastSyncTimestamp)
        notificationRepository.generateNotifications()
        return Result.success()
    }
}
```

### Scheduling

```kotlin
// One-time sync
WorkManager.getInstance(context)
    .enqueueUniqueWork(
        "sms_sync",
        ExistingWorkPolicy.REPLACE,
        OneTimeWorkRequestBuilder<SmsSyncWorker>().build()
    )

// Periodic sync (every 15 minutes)
PeriodicSmsSyncWorker.schedule(context)  // Called in FinanceApp.onCreate()
```

---

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Single Activity** | Simpler navigation, Compose-native |
| **Room over SQLite** | Type safety, compile-time verification, Flow support |
| **Hilt over Koin** | Compile-time DI, better Android integration |
| **StateFlow over LiveData** | Kotlin-first, better null safety, Flow operators |
| **Paisa storage** | Avoid floating-point precision issues |
| **Flow everywhere** | Consistent reactive programming model |
| **Database transactions** | ACID compliance for balance updates |

---

## Error Handling

### Repository Layer
- Catches database exceptions
- Returns default values for failed reads
- Logs errors for debugging

### ViewModel Layer
- Exposes error state in UiState
- Handles loading states
- Provides user feedback

### UI Layer
- Shows error messages via Snackbar
- Graceful degradation with empty states

---

## Input Validation

**Location**: `core/util/ValidationUtils.kt`

Centralized validation utilities ensure consistent input handling:

- `isValidAmount()` - Currency amount validation with bounds checking
- `isValidCategoryName()` - Category name format and length
- `isValidEmail()` - Email pattern matching
- `sanitizeMerchantName()` - Merchant name normalization
- `isValidColorHex()` - Hex color string validation

---

## Date Formatting

**Location**: `core/util/DateUtil.kt`

Centralized date formatting ensures consistent display across all screens:

- `formatDateWithTime(timestamp, context)` - Full date with time, respects device 24-hour setting
- `formatDateOnly(timestamp)` - Date only for grouping headers
- Uses `java.time` APIs for thread safety
- Output format: "14 Mar 2026, 02:47 PM" (12h) or "14 Mar 2026, 14:47" (24h)

---

## Theme Utilities

**Location**: `presentation/theme/Elevation.kt`

Centralized elevation and border utilities ensure consistent styling across light/dark themes:

### Dark Mode Strategy
- **Light mode**: Cards use elevation (shadows) for depth
- **Dark mode**: Cards use borders instead (no shadows) for visual separation

### Utility Functions

| Function | Light Mode | Dark Mode |
|----------|------------|-----------|
| `heroCardStyling()` | 2.dp elevation | 0.dp + border |
| `cardStyling()` | 1.dp elevation | 0.dp + border |
| `listItemCardStyling()` | 0.5.dp elevation | 0.dp + border |
| `isDarkTheme()` | false | true |
| `inputBorderColor()` | LightGray | DarkSurfaceHighlight |
| `selectionBorder()` | 2.dp accent | 2.dp accent |

### Usage Pattern

```kotlin
// Before (repeated 50+ times)
val isDark = MaterialTheme.colorScheme.surface == DarkSurface
val border = if (isDark) BorderStroke(1.dp, ...) else null
val elevation = if (isDark) 0.dp else 1.dp

// After (single line)
val styling = cardStyling()
Card(
    elevation = CardDefaults.cardElevation(defaultElevation = styling.elevation),
    border = styling.border
)
```

---

## Logging

**Location**: `core/util/AppLogger.kt`

Centralized logging utility for consistent debug logging across the app:

### Log Levels

| Method | Level | Usage |
|--------|-------|-------|
| `d(tag, message)` | DEBUG | Development debugging |
| `e(tag, message, throwable?)` | ERROR | Error conditions |
| `w(tag, message)` | WARN | Warning conditions |
| `i(tag, message)` | INFO | Informational messages |

### Usage Pattern

```kotlin
// In Repository or ViewModel
try {
    // operation
} catch (e: Exception) {
    AppLogger.e("FinanceRepo", "Failed to sync transactions", e)
}
```

Logging is only active in DEBUG builds - production builds compile out log calls. All logs are prefixed with "FinanceTracker" tag for easy filtering. The utility is designed for easy integration with Firebase Crashlytics or Sentry.

---

## Battery Optimization

**Location**: `core/util/BatteryOptimizationUtil.kt`

Manages battery optimization exemptions for reliable background SMS sync:

### Key Methods

| Method | Purpose |
|--------|---------|
| `isIgnoringBatteryOptimizations()` | Check if app is exempt from battery optimization |
| `openBatteryOptimizationSettings()` | Opens system battery optimization dialog |
| `shouldShowBatteryOptimizationPrompt()` | Determines when to prompt user (Android M+) |

### Usage

The app prompts users to disable battery optimization when:
- Running Android 6.0 (Marshmallow) or higher
- Auto SMS sync is enabled
- App is not already exempt from battery optimization

This ensures the `PeriodicSmsSyncWorker` can reliably run every 15 minutes even when the app is in the background.

---

## Build Configuration

### Release Build Hardening

The release build is configured for security and optimization:

```kotlin
release {
    isMinifyEnabled = true      // ProGuard/R8 code minification
    isShrinkResources = true    // Remove unused resources
    proguardFiles(...)
}
```

**ProGuard Rules** (`proguard-rules.pro`):
- Room entities preserved for database operations
- Kotlin metadata for reflection
- Hilt ViewModels kept for DI
- Compose runtime classes preserved
- Enum values kept for type converters
