# Features Documentation

This document provides detailed documentation for all features in the Finance Tracker app.

---

## 1. Automatic SMS Transaction Parsing

### Overview
The app automatically parses bank SMS messages to extract transaction details, reducing manual entry.

### How It Works

**Location**: `core/parser/SmsParserEngine.kt`

```
SMS Received → SmsParserEngine.parse() → ParsedTransaction → Database
```

### Parsing Logic

1. **Amount Extraction**
   - Regex patterns for currency: `Rs.`, `INR`, `₹`, `$`
   - Extracts numeric value with decimals
   - Converts to paisa (multiply by 100)

2. **Transaction Type Detection**
   - **DEBIT keywords**: debited, spent, paid, withdrawn, purchase
   - **CREDIT keywords**: credited, received, deposited, refund

3. **Merchant Extraction**
   - Looks for prepositions: to, at, on, via, for, from
   - Extracts text following these prepositions
   - Cleans up merchant name (removes trailing info)

4. **Filtering**
   - Ignores OTP messages
   - Ignores balance inquiry messages
   - Ignores verification codes

### Supported Banks
- HDFC Bank
- ICICI Bank
- SBI (State Bank of India)
- Axis Bank
- Kotak Mahindra
- And other Indian banks with standard SMS formats

### Example

**Input SMS**:
```
INR 1,500.00 debited from A/c **1234 on 10-Mar-25 to AMAZON. Avl Bal: INR 45,000.00
```

**Parsed Output**:
```kotlin
ParsedTransaction(
    amount = 150000L,        // in paisa
    type = EXPENSE,
    merchant = "AMAZON",
    timestamp = 1710028800000L
)
```

### Background Sync

**Location**: `data/worker/SmsSyncWorker.kt`

- Uses WorkManager for reliable background execution
- Triggered manually from Settings or on app launch
- Deduplicates using SMS message ID hash
- Batch inserts new transactions

### Periodic Background Sync

**Location**: `data/worker/PeriodicSmsSyncWorker.kt`

- Runs automatically every **15 minutes**
- User-toggleable via Profile → Preferences → Auto SMS Sync
- Uses WorkManager's `PeriodicWorkRequestBuilder`
- Respects Privacy Mode (skips sync when enabled)
- Also triggers notification generation after each sync
- Persists after app kill and device reboot

**Toggle Control:**

- Storage key: `periodic_sync_enabled` (default: `true`)
- When toggled ON: Worker is scheduled
- When toggled OFF: Worker is cancelled

---

## 2. Transaction Management

### Overview
Core functionality for tracking all financial transactions.

### Transaction Types

```kotlin
enum class TransactionType {
    EXPENSE,   // Money going out
    INCOME,    // Money coming in
    TRANSFER   // Between wallets
}
```

### Transaction Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Auto-generated primary key |
| `amount` | Long | Amount in paisa |
| `note` | String | Description/note |
| `timestamp` | Long | Unix timestamp |
| `type` | TransactionType | EXPENSE/INCOME/TRANSFER |
| `categoryName` | String | Category (Food, Shopping, etc.) |
| `merchant` | String? | Merchant name (optional) |
| `walletId` | Long | Associated wallet |
| `personId` | Long? | Linked person for debts |
| `eventId` | Long? | Linked event/trip |
| `smsId` | String? | SMS hash for deduplication |

### Adding Transactions

When a transaction is added:
1. Transaction entity is inserted
2. Wallet balance is updated atomically
3. Person balance is updated (if linked)

```kotlin
// In FinanceRepositoryImpl
suspend fun addTransaction(transaction: TransactionEntity) {
    database.withTransaction {
        dao.insertTransaction(transaction)

        // Update wallet balance
        val impact = when (transaction.type) {
            EXPENSE -> -transaction.amount
            INCOME -> transaction.amount
            TRANSFER -> -transaction.amount
        }
        dao.updateWalletBalance(transaction.walletId, impact)

        // Update person balance if linked
        transaction.personId?.let { personId ->
            dao.updatePersonBalance(personId, transaction.amount)
        }
    }
}
```

### Transaction Search

**Location**: `presentation/ledger/LedgerViewModel.kt`

- Real-time search with 300ms debounce
- Searches in note and merchant fields
- Filter by type (All, Income, Expense, Transfer)

---

## 3. Multi-Wallet System

### Overview
Manage multiple financial accounts with automatic balance tracking.

### Wallet Types

```kotlin
enum class WalletType {
    BANK,    // Bank accounts (Asset)
    CASH,    // Physical cash (Asset)
    CREDIT   // Credit cards (Liability)
}
```

### Balance Calculation

- **Assets**: BANK + CASH balances (positive contribution)
- **Liabilities**: CREDIT balances (negative contribution)
- **Net Worth**: Total Assets - Total Liabilities

```kotlin
// FinanceDao.kt
@Query("""
    SELECT COALESCE(
        (SELECT SUM(balance) FROM wallets WHERE type != 'CREDIT'),
        0
    ) - COALESCE(
        (SELECT SUM(balance) FROM wallets WHERE type = 'CREDIT'),
        0
    )
""")
fun getTotalNetWorth(): Flow<Long>
```

### Default Wallet

If no wallet exists, the app creates a default "Cash" wallet on first transaction.

### Wallet Selection in Transactions

When adding a transaction, the user selects a wallet by name in the Payment Type field. The `DashboardViewModel` resolves the wallet by matching the name:

```kotlin
val targetWallet = wallets.find { it.name == paymentType } ?: wallets.firstOrNull()
```

If no matching wallet is found, falls back to the first available wallet.

---

## 4. Budget Management

### Overview
Set spending limits per category with rollover support.

### Budget Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `categoryName` | String | Category to track |
| `limitAmount` | Long | Budget limit in paisa |
| `colorHex` | String | Display color |
| `rolloverEnabled` | Boolean | Carry unused budget |
| `createdTimestamp` | Long | Creation date |

### Budget Calculation

```kotlin
// InsightsViewModel.kt
fun calculateBudgetProgress(budget: BudgetEntity): BudgetUiModel {
    val spent = repository.getMonthlySpentForCategory(budget.categoryName)
    val limit = budget.limitAmount

    // Rollover calculation
    val rolloverAmount = if (budget.rolloverEnabled) {
        val lastMonthSpent = repository.getLastMonthSpentForCategory(budget.categoryName)
        max(0, limit - lastMonthSpent)
    } else 0

    val effectiveLimit = limit + rolloverAmount
    val progress = spent.toFloat() / effectiveLimit

    return BudgetUiModel(
        categoryName = budget.categoryName,
        spent = spent.formatAsCurrency(),
        limit = effectiveLimit.formatAsCurrency(),
        progress = progress,
        isOverBudget = progress > 1.0,
        color = when {
            progress > 1.0 -> Red
            progress > 0.8 -> Orange
            else -> Green
        }
    )
}
```

### Color Coding

| Status | Color | Condition |
|--------|-------|-----------|
| Healthy | Green | progress < 80% |
| Warning | Orange | 80% ≤ progress < 100% |
| Over Budget | Red | progress ≥ 100% |

### Special Category

- **"All Expenses"**: Track total spending across all categories

---

## 5. Subscription Detection

### Overview
AI-powered detection of recurring payments from transaction history.

**Location**: `domain/logic/SubscriptionLogic.kt`

### Detection Algorithm

```kotlin
fun detectSubscriptions(transactions: List<TransactionEntity>): List<DetectedSubscription> {
    // 1. Group transactions by merchant and amount
    val groups = transactions
        .filter { it.type == EXPENSE }
        .groupBy { Pair(it.merchant, it.amount) }

    // 2. For each group, check for monthly pattern
    return groups.mapNotNull { (key, txns) ->
        if (txns.size < 2) return@mapNotNull null

        // Sort by date
        val sorted = txns.sortedBy { it.timestamp }

        // Calculate intervals between transactions
        val intervals = sorted.zipWithNext { a, b ->
            (b.timestamp - a.timestamp) / (24 * 60 * 60 * 1000) // days
        }

        // Check if intervals are monthly (25-35 days)
        val avgInterval = intervals.average()
        val isMonthly = avgInterval in 25.0..35.0

        if (isMonthly) {
            DetectedSubscription(
                merchantName = key.first,
                amount = key.second,
                probability = calculateProbability(intervals),
                nextDueDate = predictNextDate(sorted.last(), avgInterval)
            )
        } else null
    }
}
```

### Probability Calculation

Based on:
- Number of occurrences
- Consistency of intervals
- Recency of last payment

### Merging with Manual Subscriptions

```kotlin
// InsightsViewModel.kt
val allSubscriptions = combine(
    repository.getSubscriptions(),        // Manual
    detectSubscriptions(transactions)     // Auto-detected
) { manual, detected ->
    // Deduplicate by merchant name
    val manualMerchants = manual.map { it.merchantName.lowercase() }
    val uniqueDetected = detected.filter {
        it.merchantName.lowercase() !in manualMerchants
    }
    manual + uniqueDetected.map { it.toSubscriptionUiModel(isDetected = true) }
}
```

---

## 6. Debt Tracking (People)

### Overview
Track money owed to and from other people.

### Balance Convention

| Balance | Meaning |
|---------|---------|
| Positive | They owe you money |
| Negative | You owe them money |
| Zero | Settled/No debt |

### UI Display

```kotlin
fun PersonEntity.toPersonUiModel(): PersonUiModel {
    val statusText = when {
        currentBalance > 0 -> "Owes you"
        currentBalance < 0 -> "You owe"
        else -> "Settled"
    }

    val statusColor = when {
        currentBalance > 0 -> Green
        currentBalance < 0 -> Orange
        else -> Grey
    }

    return PersonUiModel(
        name = name,
        balance = abs(currentBalance).formatAsCurrency(),
        statusText = statusText,
        statusColor = statusColor
    )
}
```

### Linking to Transactions

When a transaction is linked to a person:
- EXPENSE to person: Increases their debt (they owe you more)
- INCOME from person: Decreases their debt (settling up)

---

## 7. Data Management (Backup & Restore)

### Overview
Comprehensive data management through a dedicated bottom sheet accessible from Profile > Backup & Restore.

**Location**: `presentation/components/DataManagementSheet.kt`

### Features

| Feature | Icon | Description |
|---------|------|-------------|
| **Import** | ⬇️ (purple) | Load transactions from CSV file |
| **Export** | ⬆️ (blue) | Save transactions to Downloads folder |
| **Clear All Data** | 🗑️ (red) | Delete all data with 2-step confirmation |

### UI Layout

```
┌─────────────────────────────────────┐
│ 📦 Data Management                  │
│    Backup, restore, or reset        │
├─────────────────────────────────────┤
│ ┌───────────────┐ ┌───────────────┐ │
│ │   ⬇️ Import   │ │   ⬆️ Export   │ │
│ │  Load CSV     │ │  Save to CSV  │ │
│ └───────────────┘ └───────────────┘ │
├─────────────────────────────────────┤
│ 🗑️ Clear All Data               >  │
└─────────────────────────────────────┘
```

### Clear Data - 2-Step Confirmation

1. **First Click**: Inline confirmation card appears
   - "Delete all data permanently?"
   - [Cancel] [Delete All]

2. **Second Click (Delete All)**: AlertDialog appears
   - "Final Confirmation"
   - "This is your last chance..."
   - [Cancel] [Yes, Delete Everything]

### Export Details

**Location**: `core/util/CsvExportUtil.kt`

```kotlin
suspend fun saveToDownloads(transactions: List<TransactionEntity>): String?
```

- Saves to device Downloads folder
- Filename: `finance_export_{timestamp}.csv`
- CSV Format: `Date,Merchant,Amount,Type,Category,Source`

### Import Details

**Location**: `core/util/CsvImportUtil.kt`

```kotlin
suspend fun parseCsv(uri: Uri): List<ImportedTransactionData>
```

- Uses Android's document picker (SAF)
- Supports: `text/csv`, `text/comma-separated-values`, `*/*`
- Shows count of imported transactions

### Clear All Data

Deletes from all tables via FinanceDao:
```kotlin
suspend fun deleteAllTransactions()
suspend fun deleteAllWallets()
suspend fun deleteAllPeople()
suspend fun deleteAllBudgets()
suspend fun deleteAllSubscriptions()
suspend fun deleteAllEvents()
```

---

## 8. Multi-Currency Support

### Overview
Display amounts in user's preferred currency.

**Location**: `core/util/CurrencyFormatter.kt`

### Supported Currencies

| Code | Symbol | Name |
|------|--------|------|
| INR | ₹ | Indian Rupee |
| USD | $ | US Dollar |
| EUR | € | Euro |
| GBP | £ | British Pound |

### Formatting

```kotlin
fun Long.formatAsCurrency(currencyCode: String = "INR"): String {
    val amount = this / 100.0  // Convert from paisa
    val format = NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance(currencyCode)
    }
    return format.format(amount)
}
```

### Storage
- All amounts stored in **paisa** (Long)
- Currency preference stored in DataStore
- Formatting applied at display time only

---

## 9. Privacy Mode

### Overview
Blocks SMS transaction sync and permanently skips messages received while enabled.

**Location**: `presentation/profile/SettingsViewModel.kt`

### Four Separate Features

| Setting | Location | Purpose |
|---------|----------|---------|
| **Privacy Mode** | Profile → Privacy & Security | Blocks SMS sync |
| **Hide Amounts** | Home → Eye icon | Hides amounts with ●●●●●● |
| **Trip Mode** | Profile → Privacy & Security | Defaults expense category to "Trip" |
| **Auto SMS Sync** | Profile → Preferences | 15-minute periodic SMS sync |

### Privacy Mode Behavior

When **Privacy Mode is toggled ON**:

- `lastSyncTimestamp` is updated to current time
- App launch does NOT auto-sync SMS
- Pull-to-refresh does NOT sync SMS
- Periodic 15-minute sync skips SMS read
- Manual sync shows "Privacy Mode is enabled. SMS sync is disabled."
- SMS arriving while ON are permanently skipped

When **Privacy Mode is toggled OFF**:

- `lastSyncTimestamp` is updated to current time (fresh start)
- SMS received during Privacy Mode are **permanently skipped**
- Only new SMS from this moment forward will be synced

### Implementation

```kotlin
// SettingsViewModel.kt
fun setPrivacyMode(enabled: Boolean) {
    viewModelScope.launch {
        userPreferencesRepository.setPrivacyMode(enabled)
        // Update timestamp on both ON and OFF
        // Ensures SMS received during Privacy Mode are permanently skipped
        userPreferencesRepository.updateLastSyncTimestamp(System.currentTimeMillis())
    }
}
```

### Hide Amounts (Eye Toggle)

```kotlin
// HomeScreen.kt
var isAmountsHidden by remember { mutableStateOf(false) }

Text(
    text = if (isAmountsHidden) "₹ ●●●●●●" else netWorth,
    style = MaterialTheme.typography.headlineLarge
)
```

### Hidden Elements

- Net worth
- Wallet balances
- Transaction amounts
- Monthly totals

### Trip Mode

**Location**: `presentation/profile/SettingsViewModel.kt`

When **Trip Mode is ON**:
- Manual entries default to "Trip" category for expenses
- SMS synced transactions default to "Trip" category for expenses
- INCOME transactions are NOT affected (still default to "Money Received")
- "Trip" category is a system-provided expense category

When **Trip Mode is OFF**:
- Manual entries default to "Uncategorized" category for expenses
- SMS synced transactions default to "Uncategorized" category for expenses

**Implementation:**

```kotlin
// AddTransactionSheet.kt
val defaultCategory = if (isTripMode) {
    categories.find { it.name == "Trip" && it.type == "EXPENSE" }
} else {
    categories.find { it.name == "Uncategorized" && it.type == "EXPENSE" }
}

// FinanceRepositoryImpl.kt (SMS sync)
val categoryName = if (isIncome) "Money Received"
    else if (isTripMode) "Trip"
    else "Uncategorized"
```

**Existing Users:**
- `ensureTripCategoryExists()` is called on app startup
- Automatically creates "Trip" category if missing

---

## 10. Category Management

### Overview
Organize transactions into expense and income categories.

### Default Categories

**Expense**:
- Uncategorized
- Food & Dining
- Shopping
- Transportation
- Entertainment
- Bills & Utilities
- Health & Wellness
- Money Transfer
- Trip

**Income**:
- Salary
- Money Received

### Category Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `name` | String | Category name (unique) |
| `type` | String | EXPENSE or INCOME |
| `colorHex` | String | Display color |
| `iconId` | Int | Icon identifier (0-25) |
| `isSystemDefault` | Boolean | Prevent deletion |

### Category Icons

Each category has a dynamic icon based on its `iconId` field:

| iconId | Icon | Default Category |
|--------|------|------------------|
| 0 | Receipt | Uncategorized |
| 1 | LocalDining | Food & Dining |
| 2 | ShoppingCart | Shopping |
| 3 | DirectionsCar | Transportation |
| 4 | Receipt | Bills & Utilities |
| 5 | Movie | Entertainment |
| 6 | LocalHospital | Health & Wellness |
| 7 | Flight | Trip |
| 8 | SwapHoriz | Money Transfer |
| 9 | AccountBalance | Salary |
| 10 | Payments | Money Received |
| 11-25 | Various | User-selectable |

**Key Files:**
- `core/util/CategoryIconUtil.kt` - Icon ID mapping
- `presentation/components/CategoryIcon.kt` - Reusable icon composable
- `presentation/components/IconPicker.kt` - Icon selection grid

**Features:**
- System categories get automatic icons based on name
- User-created categories can select from 25+ predefined icons
- Icons appear everywhere: transaction lists, category lists, dropdowns, budget cards

### Dynamic Category Colors

Category colors are applied consistently across all screens in the app:

- **Transaction lists** (Home, Ledger): Icon backgrounds use `CategoryEntity.colorHex`
- **Insights charts**: Expense/Income breakdown charts use actual category colors
- **Category management**: Colors can be edited in the Categories screen

**Implementation:**

The `FinanceRepositoryImpl` combines transaction flows with category flows to provide dynamic color lookup:

```kotlin
return combine(
    financeDao.getAllTransactions(),
    userPreferencesRepository.currency,
    financeDao.getAllCategories()
) { entities, currency, categories ->
    val categoryMap = categories.associateBy { it.name }
    entities.map { mapToTransactionUiModel(it, currency, categoryMap) }
}
```

When you change a category's color in the Categories screen, it reflects everywhere in the app immediately.

### Category Seeding

System categories are auto-seeded on app launch via `seedDefaultCategories()`:

- First cleans up duplicate categories using `deleteDuplicateCategories()`
- Then inserts any missing system categories with proper colors and icons
- System categories (`isSystemDefault = true`) cannot be deleted by users

### Category Breakdown

```kotlin
// FinanceDao.kt
@Query("""
    SELECT categoryName, SUM(amount) as total
    FROM transactions
    WHERE type = 'EXPENSE'
    GROUP BY categoryName
""")
fun getCategoryBreakdown(): Flow<List<CategoryTuple>>
```

---

## 11. In-App Notifications

### Overview

Smart notification system that auto-generates alerts from app data for budgets, bills, debts, and events.

**Key Files:**
- Entity: `data/local/entity/NotificationEntity.kt`
- DAO: `data/local/dao/NotificationDao.kt`
- Repository: `data/repository/NotificationRepositoryImpl.kt`
- ViewModel: `presentation/notifications/NotificationViewModel.kt`
- Screen: `presentation/notifications/NotificationScreen.kt`
- Badge: `presentation/components/NotificationIconWithBadge.kt`
- System: `core/notification/NotificationHelper.kt`

### Notification Types

| Type | Trigger | Example Message |
|------|---------|-----------------|
| `BUDGET_ALERT` | Budget reaches 80% or 100% | "Food budget is at 85% (₹17,000/₹20,000)" |
| `UPCOMING_BILL` | Subscription due (7, 3, 2, 1, 0 days before, 1 day after) | "Netflix subscription due in 2 days (₹649)" |
| `DEBT` | One-time when person first has non-zero balance | "John owes you ₹5,000" |
| `DEBT_REMINDER` | Weekly Sunday 9 AM (if debts exist) | "2 people owe you ₹500. You owe 1 person ₹200." |
| `TRIP` | Group progress (legacy, rarely used) | "Goa Trip: ₹15,000 spent of ₹50,000 budget" |

**Note:** INCOME and EXPENSE notifications are disabled (not generated).

### Auto-Generation Flow

```
App Launch / Pull-to-Refresh
    │
    ▼
NotificationRepository.generateNotifications()
    │
    ├── generateBudgetAlertNotifications()
    │       └── Check each budget: current month spending vs limit
    │           └── Alert at 80% (warning) and 100% (exceeded)
    │
    ├── generateUpcomingBillNotifications()
    │       └── Check each subscription: due within 3 days?
    │           └── Include overdue bills
    │
    ├── generateDebtNotifications()
    │       └── Check each person: non-zero balance?
    │
    └── generateEventNotifications()
            └── Check each active event: has budget?
```

### Smart Deduplication

Each notification is identified by `referenceId` + `referenceType`. On regeneration:

```kotlin
// NotificationRepositoryImpl.kt
val existingMessage = notificationDao.getMessageByReference(referenceId, referenceType)
if (existingMessage == newMessage) {
    // Skip - preserves read status
    return
}
// Message changed - delete old, create new (unread)
notificationDao.deleteByReference(referenceId, referenceType)
notificationDao.insert(newNotification)
```

This prevents:
- Duplicate notifications for the same item
- Read notifications reverting to unread when data unchanged
- Stale notifications when data changes

### Thread Safety

```kotlin
// NotificationRepositoryImpl.kt
private val generationMutex = Mutex()

override suspend fun generateNotifications() {
    generationMutex.withLock {
        // Serializes concurrent calls
        cleanupOldNotifications()
        generateBudgetAlertNotifications()
        generateUpcomingBillNotifications()
        generateDebtNotifications()
        generateEventNotifications()
    }
}
```

### Budget Alert Logic

```kotlin
// Current month spending only
val (startOfMonth, endOfMonth) = getCurrentMonthRange()
val spent = dao.getSpentForCategory(categoryName, startOfMonth, endOfMonth)
val progress = spent.toFloat() / budget.limitAmount

when {
    progress >= 1.0 -> createAlert("Budget exceeded! ${category} at 100%")
    progress >= 0.8 -> createAlert("${category} budget is at ${(progress * 100).toInt()}%")
}
```

**Special handling:**
- "All Expenses" budget excludes "Money Transfer" category
- Unique per month (allows new alerts each billing cycle)

### Subscription Reminder Logic

```kotlin
val today = LocalDate.now()
val dueDate = subscription.nextDueDate.toLocalDate()
val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate)

when {
    daysUntilDue < 0 -> createAlert("Overdue Bill: ${name}")
    daysUntilDue <= 3 -> createAlert("${name} due in ${daysUntilDue} days")
}
```

### UI Features

| Feature | Description |
|---------|-------------|
| **Tabs** | All / Unread filter |
| **Mark all read** | Header button |
| **Swipe-to-delete** | Left or right swipe |
| **Relative time** | Just now, 2 mins ago, Yesterday, 3 days ago |
| **Auto-cleanup** | 30-day old notifications removed |
| **Badge** | Unread count on all main screens (1-9, then 9+) |

### System Notifications

The app also posts to Android's notification bar:

```kotlin
// NotificationHelper.kt
fun showNotification(notification: NotificationEntity) {
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra("notification_id", notification.id)
        putExtra("target_route", getRouteForType(notification.referenceType))
    }

    NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(notification.title)
        .setContentText(notification.message)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
}
```

**Deep linking routes:**

| referenceType | Target Route |
|---------------|--------------|
| TRANSACTION | ledger |
| BUDGET | insights |
| SUBSCRIPTION | insights |
| PERSON | wallet |
| GROUP | ledger (Groups tab) |
| WEEKLY_REMINDER | wallet |

### NotificationEntity Schema

```kotlin
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val type: String,                    // NotificationType as string
    val timestamp: Long,                 // Creation time
    val isRead: Boolean = false,
    val referenceId: Long? = null,       // Related entity ID
    val referenceType: String? = null    // "BUDGET", "SUBSCRIPTION", etc.
)
```

---

## 12. Groups & Split Bills

### Overview

Groups is a unified feature for managing shared expenses, trip budgets, and savings goals with multiple people. It replaces the previous EventEntity with a more comprehensive system.

**Key Files:**
- Entity: `data/local/entity/GroupEntity.kt`
- Repository: `domain/repository/GroupRepository.kt`
- ViewModel: `presentation/group/GroupViewModel.kt`
- Screens: `presentation/group/GroupScreen.kt`, `GroupDetailScreen.kt`

### Group Types

| Type | Display Name | Purpose | Key Features |
|------|--------------|---------|--------------|
| `GENERAL` | General | All-purpose group (default) | Combined features |
| `TRIP` | Group By Category | Category-based expense tracking | Budget limit, expense tracking |
| `SPLIT_EXPENSE` | Split Bill | Splitting bills with friends | Equal/custom splits, settlements |
| `SAVINGS_GOAL` | Savings | Collaborative savings target | Contributions tracking, progress |

**UI Order:** General → Group By Category → Split Bill → Savings

### Core Features

#### Expense Splitting

Split expenses among group members with multiple methods:

```kotlin
enum class SplitType {
    EQUAL,      // Divide equally among all members
    CUSTOM,     // Specific amounts per person
    PERCENTAGE, // Percentage-based split
    SHARES      // Share units (e.g., 2 shares vs 1 share)
}
```

**Equal Split Example:**
- Total bill: ₹1,500
- 3 members (you + 2 friends)
- Each owes: ₹500

#### Settlement Suggestions

The app calculates optimal settlements to minimize transactions:

```
Before:
  Alice owes you ₹500
  Bob owes you ₹300
  You owe Carol ₹800

Optimized Settlement:
  Alice → Carol: ₹500
  Bob → Carol: ₹300
  (Only 2 transactions instead of 3)
```

#### Savings Goals

Track contributions toward a shared goal:

- Set target amount (e.g., ₹50,000 for a trip fund)
- Members contribute individually
- Progress visualization
- Per-member contribution tracking

### Group Membership

- Current user is implicitly a member of all their groups
- Add people from your contacts (PersonEntity)
- Members can be owners or regular members
- Members persist across sessions

### Data Flow

```
User creates expense
    │
    ▼
GroupRepository.addGroupExpense()
    │
    ├── Create TransactionEntity (for balance tracking)
    ├── Create GroupTransactionEntity (links to group)
    └── Create GroupSplitEntity entries (per-member shares)
    │
    ▼
Balance updates calculated
    │
    ▼
Settlement suggestions refreshed
```

### UI Access

Groups are accessed via the **Ledger screen's "Groups" tab**, which shows:
- All active groups
- Filter by type
- Group cards with summary stats

### Group Transaction Isolation

Group expenses are **isolated from personal transactions** to prevent double-counting:

| View                              | Group Transactions                     |
|-----------------------------------|----------------------------------------|
| Home screen recent transactions   | Excluded                               |
| Ledger "All Transactions" tab     | Excluded                               |
| Monthly income/expense totals     | Excluded                               |
| Insights category breakdown       | Excluded                               |
| CSV export                        | Excluded                               |
| Group Detail screen               | **Included** (only place they appear)  |

This design ensures:

- Personal finances remain separate from shared group expenses
- No double-counting of expenses in totals
- Clear separation between "your money" and "group money"

**Note:** Group transactions still affect wallet balances when "You" is the payer.

### Key Screens

| Screen | Purpose |
|--------|---------|
| GroupScreen | List of all groups with filters |
| GroupDetailScreen | Individual group details, expenses, settlements |
| AddGroupSheet | Create new group |
| AddGroupExpenseSheet | Add expense with split configuration |
| SettleUpSheet | Record settlement between members |
| AddContributionSheet | Add contribution to savings goal |

### AddGroupExpenseSheet

Fields: Amount, Description, Category, Paid By, From Wallet, Split Type, Custom Amounts

- **Category dropdown**: Shows categories grouped by EXPENSE/INCOME with icons
- **Transaction Type**: Auto-derived from selected category (no manual selection required)
  - Selecting an EXPENSE category → transaction saved as EXPENSE
  - Selecting an INCOME category → transaction saved as INCOME
- **Paid By**: Chips for "You" and all group members (hidden for GENERAL groups)
- **From Wallet**: Shown only when "You" is selected as payer (hidden for GENERAL groups)
- **Split Type**: Equal or Custom (hidden for GENERAL groups)
- **Custom Amounts**: Per-member amount fields when Custom split selected

### Group Transaction Cards

Transaction cards in Group Detail display:

- **Category icon**: With color background from `CategoryEntity.colorHex`
- **Note**: Main text (transaction description)
- **Category name**: Shown below the note
- **Amount**: Colored based on type (green with `+` for INCOME, red with `-` for EXPENSE)
- **Date**: Formatted date and time on the right side (e.g., "14 Mar 2026, 02:47 PM")

### Group Card Display

Group list cards show:

- Group name and member count
- Group type badge (General, Group By Category, Split Bill, Savings)
- **GENERAL groups**: Shows "Net Total" (income - expense)
- **TRIP groups**: Shows "Total expenses" from filtered transactions by category
- **Other groups**: Shows "Total expenses" from group transactions

Note: The "Settled up" section has been removed from all group cards.

### Group Detail Tabs

| Tab | Shows |
|-----|-------|
| **Expenses** | All group expenses with who paid and splits |
| **Balances** | Who owes whom and settlement suggestions |
| **Activity** | Chronological history of all transactions |
| **Stats** | Spending breakdown by category/tag |

### Settlement Recording

When a member pays another:

```kotlin
groupRepository.recordSettlement(
    groupId = 1L,
    fromPersonId = 2L,      // Alice pays
    fromSelf = false,
    toPersonId = null,      // Current user receives
    toSelf = true,
    amount = 50000L,        // ₹500
    note = "Dinner settlement"
)
```

This adjusts the internal balance tracking and updates settlement suggestions.

---

## 13. Display Size Scaling

### Overview

User-configurable display scaling to adjust text sizes, spacing, icons, and cards throughout the app. Particularly useful for users who find the default sizing too large or too small for their device.

**Location**: Profile → Preferences → Display Size

### Scale Options

| Size | Label | Scale Factor | Description |
|------|-------|--------------|-------------|
| Extra Small | XS | 0.75x | Maximum content density |
| Small | S | 0.85x | Compact displays |
| Medium | M | 0.92x | Balanced (default) |
| Large | L | 1.0x | Original app sizing |

### Key Files

| File | Purpose |
|------|---------|
| `presentation/theme/Dimensions.kt` | Scalable spacing system with `LocalDimensions` CompositionLocal |
| `presentation/theme/Type.kt` | `scaledTypography(scale)` function for text sizes |
| `presentation/theme/Theme.kt` | Applies scale via `FinanceTrackerTheme(displaySize=...)` |
| `data/repository/UserPreferencesRepositoryImpl.kt` | Persists preference in DataStore |

### How It Works

1. **User selects size** in Profile → Preferences
2. **Preference saved** to DataStore (`display_size` key)
3. **MainActivity observes** preference and passes to theme
4. **Theme calculates** scale factor and provides scaled `Dimensions` via CompositionLocal
5. **All composables** access `LocalDimensions.current` for scaled values

### Implementation Pattern

```kotlin
@Composable
fun MyScreen() {
    val dims = LocalDimensions.current

    // Use scaled values instead of hardcoded dp
    Card(
        modifier = Modifier.padding(dims.lg),     // 16.dp × scale
        shape = RoundedCornerShape(dims.cardRadius) // 20.dp × scale
    ) {
        Icon(
            modifier = Modifier.size(dims.iconMd)  // 24.dp × scale
        )
    }
}
```

### Available Dimension Names

| Category | Name | Base Value | Usage |
|----------|------|------------|-------|
| **Spacing** | `xs` | 4.dp | Fine borders, tiny gaps |
| | `sm` | 8.dp | Small spacing |
| | `md` | 12.dp | Medium spacing |
| | `lg` | 16.dp | Standard padding |
| | `xl` | 20.dp | Large padding |
| | `xxl` | 24.dp | Hero card padding |
| | `xxxl` | 32.dp | Extra large |
| **Icons** | `iconXs` | 16.dp | Tiny icons |
| | `iconSm` | 20.dp | Small icons |
| | `iconMd` | 24.dp | Medium icons |
| | `iconLg` | 32.dp | Large icons |
| | `iconXl` | 48.dp | Avatar-sized icons |
| | `iconXxl` | 64.dp | Hero icons |
| **Avatars** | `avatarXs` | 28.dp | Tiny avatars |
| | `avatarSm` | 36.dp | Small avatars |
| | `avatarMd` | 48.dp | Medium avatars |
| | `avatarLg` | 64.dp | Large avatars |
| **Components** | `cardRadius` | 20.dp | Card corner radius |
| | `heroRadius` | 24.dp | Hero card radius |
| | `chipRadius` | 8.dp | Chip/tag radius |
| | `fabSize` | 64.dp | FAB size |
| | `fabClearance` | 80.dp | Bottom padding for FAB |
| | `buttonHeight` | 56.dp | Standard button height |
| | `statusBarSpace` | 48.dp | Status bar spacing |
| **Progress** | `progressHeight` | 8.dp | Progress bar height |
| | `dividerHeight` | 1.dp | Divider thickness |
| | `indicatorHeight` | 4.dp | Indicator thickness |
| **Special** | `donutChartSize` | 100.dp | Chart container size |
| | `searchBarHeight` | 48.dp | Search bar height |
| | `textFieldHeight` | 56.dp | Text field height |

### What Scales

| Element | Scales | Notes |
|---------|--------|-------|
| Typography | ✅ | All MaterialTheme text styles |
| Padding/Margins | ✅ | All spacing values |
| Card corners | ✅ | Radius proportional to size |
| Icons | ✅ | Both size and container |
| Avatars | ✅ | Profile images, initials |
| FAB | ✅ | Size and offset |
| Bottom bar spacing | ✅ | Content padding |
| Dividers | ❌ | Kept at 1.dp for crispness |

### Storage

- **Key**: `display_size`
- **Values**: `"extra_small"`, `"small"`, `"medium"`, `"large"`
- **Default**: `"medium"`

### UI Selector

The Display Size selector appears in Profile → Preferences as segmented buttons:

```
Display Size    [XS] [S] [M] [L]
Medium                     ✓
```

- Selected size is highlighted with accent color border
- Full label shown below the selector row
- Changes apply immediately (no restart required)

---

## 14. Error Handling

### Overview

The app implements comprehensive error handling with user-visible error states and centralized logging.

**Key Files:**
- `core/util/AppLogger.kt` - Centralized logging utility
- `presentation/components/ErrorSnackbar.kt` - Reusable error Snackbar component

### Error State Pattern

All ViewModels include error state management:

```kotlin
data class DashboardUiState(
    // ... other fields
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel : ViewModel() {
    private val _error = MutableStateFlow<String?>(null)

    fun clearError() {
        _error.value = null
    }

    private fun handleError(e: Exception) {
        AppLogger.e(TAG, "Operation failed", e)
        _error.value = "Failed to complete operation"
    }
}
```

### UI Error Display

All main screens include SnackbarHost for error display:

```kotlin
@Composable
fun HomeScreen(viewModel: DashboardViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Auto-show error when state changes
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { ... }
}
```

### Logging

**Location**: `core/util/AppLogger.kt`

| Level | Method | Usage |
|-------|--------|-------|
| DEBUG | `AppLogger.d(tag, msg)` | Development debugging |
| INFO | `AppLogger.i(tag, msg)` | Informational messages |
| WARN | `AppLogger.w(tag, msg)` | Warning conditions |
| ERROR | `AppLogger.e(tag, msg, throwable?)` | Error conditions |

Logging is only active in DEBUG builds - production builds compile out log calls.

### ViewModels with Error Handling

| ViewModel | Error Field | Screens |
|-----------|-------------|---------|
| `DashboardViewModel` | `error: String?` | HomeScreen |
| `LedgerViewModel` | `error: String?` | LedgerScreen |
| `WalletViewModel` | `error: String?` | WalletScreen |
| `InsightsViewModel` | `error: String?` | InsightsScreen |
| `SettingsViewModel` | `error: String?` | ProfileScreen |
| `CategoryViewModel` | `error: String?` | CategoryListScreen |
| `NotificationViewModel` | `error: String?` | NotificationScreen |
| `UserProfileViewModel` | `error: String?` | UserProfileScreen |
| `GroupViewModel` | `error: String?` | GroupDetailScreen |

---

## 15. Battery Optimization

### Overview

The app prompts users to disable battery optimization to ensure reliable background SMS sync.

**Key Files:**
- `core/util/BatteryOptimizationUtil.kt` - Battery optimization management
- `presentation/components/BatteryOptimizationDialog.kt` - User prompt dialog

### Why It's Needed

Android's battery optimization can prevent background workers from running reliably. The `PeriodicSmsSyncWorker` runs every 15 minutes to sync SMS transactions, and battery optimization can delay or skip these syncs.

### Implementation

```kotlin
// BatteryOptimizationUtil.kt
object BatteryOptimizationUtil {
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }

    fun shouldShowBatteryOptimizationPrompt(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
               !isIgnoringBatteryOptimizations(context)
    }
}
```

### User Flow

1. User enables Auto SMS Sync in Profile → Preferences
2. App checks if battery optimization is enabled
3. If enabled, shows `BatteryOptimizationDialog`
4. User can:
   - **Enable**: Opens system settings to disable battery optimization
   - **Skip**: Dismisses dialog (can check "Don't ask again")
5. If "Don't ask again" is checked, dialog won't appear again

### Dialog Features

- **Title**: "Enable Background Sync"
- **Message**: Explains why battery optimization exemption is needed
- **Checkbox**: "Don't ask again" to permanently dismiss
- **Buttons**: Enable (primary), Skip (secondary)

---

## 16. Log Out

### Overview

The Log Out feature allows users to reset the app to its initial state, with the option to keep or delete their data.

**Location**: Profile → Support → Log Out

### User Flow

```
[Log Out Button] → [Confirmation Dialog] → [Data Choice Dialog] → [Loading] → [App Restart]
                                                  ↓
                                    ┌─────────────┴─────────────┐
                                    ↓                           ↓
                              Keep My Data              Delete Everything
                                    ↓                           ↓
                           Reset prefs only          Clear DB + Reset prefs
                                    ↓                           ↓
                    [Privacy Policy] → [Onboarding] → [Home with/without data]
```

### Dialogs

| Dialog | Purpose | Buttons |
|--------|---------|---------|
| `LogoutConfirmationDialog` | Confirm intent to log out | Cancel, Continue |
| `LogoutDataChoiceDialog` | Choose data handling | Keep My Data, Delete Everything |

**File**: `presentation/components/LogoutDialogs.kt`

### Data Handling Options

| Option | Behavior |
|--------|----------|
| **Keep My Data** | Resets preferences only. Transactions, wallets, budgets preserved. |
| **Delete Everything** | Clears ALL database tables (transactions, wallets, people, groups, notifications, budgets, subscriptions). Categories are preserved and re-seeded. |

### Preferences Reset

When logging out, the following preferences are reset to defaults:

| Preference | Reset Value |
|------------|-------------|
| `privacy_policy_accepted` | `false` (triggers Privacy Policy dialog) |
| `onboarding_complete` | `false` (triggers Onboarding screen) |
| `user_name`, `user_email`, etc. | Cleared |
| `theme` | `"system"` |
| `display_size` | `"medium"` |
| `currency` | `"INR"` |
| `privacy_mode` | `false` |
| `trip_mode` | `false` |
| `amounts_hidden` | `false` |
| `periodic_sync_enabled` | `true` |
| `notifications_enabled` | `true` |
| `last_sync_timestamp` | `0L` |

### Implementation Details

1. **Application Scope**: Logout uses `applicationScope` (not `viewModelScope`) to ensure database operations complete even when activity is destroyed.

2. **App Restart**: Uses `activity.finish()` + `activity.startActivity(intent)` pattern for clean restart.

3. **Workers Cancelled**: Periodic SMS sync, debt reminder, and subscription reminder workers are cancelled on logout.

### Post-Logout Onboarding Behavior

When user completes onboarding after logout:

| Onboarding Choice | Result |
|-------------------|--------|
| **Enable Secure Sync** | SMS sync enabled, Privacy Mode OFF |
| **Skip and Use Manually** | SMS sync disabled, Privacy Mode ON |


