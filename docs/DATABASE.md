# Database Documentation

This document describes the Room database schema, entities, relationships, and queries.

---

## Database Configuration

**Location**: `data/local/AppDatabase.kt`

```kotlin
@Database(
    entities = [
        WalletEntity::class,
        PersonEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        GroupEntity::class,
        GroupMemberEntity::class,
        GroupTransactionEntity::class,
        GroupSplitEntity::class,
        GroupContributionEntity::class,
        SettlementEntity::class,
        SubscriptionEntity::class,
        CategoryEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao
    abstract fun notificationDao(): NotificationDao
    abstract fun groupDao(): GroupDao
}
```

| Property | Value |
|----------|-------|
| Database Name | `finance_tracker.db` |
| Version | 1 |
| Migration Strategy | Destructive (dev mode) |

---

## Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  ┌──────────────┐         ┌──────────────────────┐              │
│  │ WalletEntity │◄────────│  TransactionEntity   │              │
│  │              │ CASCADE │                      │              │
│  │ - id (PK)    │         │ - id (PK)            │              │
│  │ - name       │         │ - amount             │              │
│  │ - type       │         │ - note               │              │
│  │ - balance    │         │ - timestamp          │              │
│  │ - colorHex   │         │ - type               │              │
│  │ - accountNum │         │ - categoryName       │              │
│  └──────────────┘         │ - merchant           │              │
│                           │ - walletId (FK) ─────┘              │
│  ┌──────────────┐         │ - personId (FK) ─────┐              │
│  │ PersonEntity │◄────────│ - groupId (FK) ──────┼──┐           │
│  │              │ SET_NULL│ - smsId              │  │           │
│  │ - id (PK)    │         └──────────────────────┘  │           │
│  │ - name       │                                   │           │
│  │ - balance    │         ┌──────────────────────┐  │           │
│  └──────────────┘         │   GroupEntity        │◄─┘           │
│         ▲                 │                      │ CASCADE      │
│         │                 │ - id (PK)            │              │
│         │                 │ - name               │              │
│         │                 │ - type (GroupType)   │              │
│         │                 │ - budgetLimit        │              │
│         │                 │ - targetAmount       │              │
│         │                 │ - isActive           │              │
│         │                 └──────────┬───────────┘              │
│         │                            │                          │
│         │         ┌──────────────────┼──────────────────┐       │
│         │         │                  │                  │       │
│         │         ▼                  ▼                  ▼       │
│         │  ┌─────────────┐  ┌─────────────────┐  ┌───────────┐  │
│         └──│GroupMember  │  │GroupTransaction │  │Settlement │  │
│            │             │  │                 │  │           │  │
│            │- groupId(FK)│  │- groupId (FK)   │  │- groupId  │  │
│            │- memberId   │  │- transactionId  │  │- fromId   │  │
│            │- isOwner    │  │- paidByPersonId │  │- toId     │  │
│            └─────────────┘  │- splitType      │  │- amount   │  │
│                             └────────┬────────┘  └───────────┘  │
│                                      │                          │
│                                      ▼                          │
│                             ┌─────────────────┐                 │
│                             │  GroupSplit     │                 │
│                             │                 │                 │
│                             │- groupTxnId(FK) │                 │
│                             │- memberId       │                 │
│                             │- shareAmount    │                 │
│                             └─────────────────┘                 │
│                                                                 │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────┐   │
│  │CategoryEntity│    │ BudgetEntity │    │SubscriptionEntity│   │
│  │              │    │              │    │                  │   │
│  │ - id (PK)    │    │ - id (PK)    │    │ - id (PK)        │   │
│  │ - name       │    │ - categoryNm │    │ - merchantName   │   │
│  │ - type       │    │ - limitAmt   │    │ - amount         │   │
│  │ - colorHex   │    │ - colorHex   │    │ - frequency      │   │
│  │ - iconId     │    │ - rollover   │    │ - nextDueDate    │   │
│  │ - isDefault  │    │ - created    │    │ - colorHex       │   │
│  └──────────────┘    └──────────────┘    │ - isAutoDetected │   │
│                                          └──────────────────┘   │
│                                                                 │
│  ┌──────────────────┐  ┌──────────────────────┐                 │
│  │NotificationEntity│  │GroupContributionEntity│                │
│  │                  │  │ (for savings goals)   │                │
│  │ - id (PK)        │  │                       │                │
│  │ - title          │  │ - id (PK)             │                │
│  │ - message        │  │ - groupId (FK)        │                │
│  │ - type           │  │ - memberId (FK)       │                │
│  │ - timestamp      │  │ - amount              │                │
│  │ - isRead         │  │ - timestamp           │                │
│  │ - referenceId    │  │ - note                │                │
│  │ - referenceType  │  └──────────────────────┘                 │
│  └──────────────────┘                                           │
└─────────────────────────────────────────────────────────────────┘
```

---

## Entity Definitions

### TransactionEntity

**Location**: `data/local/entity/TransactionEntity.kt`

```kotlin
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("walletId"),
        Index("personId"),
        Index("groupId"),
        Index("smsId")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Long,              // In paisa (1/100 rupee)
    val note: String,
    val timestamp: Long,           // Unix timestamp in milliseconds
    val type: TransactionType,     // EXPENSE, INCOME, TRANSFER
    val categoryName: String,
    val merchant: String? = null,
    val walletId: Long,
    val personId: Long? = null,
    val groupId: Long? = null,
    val smsId: String? = null      // Hash for deduplication
)
```

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Auto-generated primary key |
| amount | Long | No | Amount in paisa |
| note | String | No | Transaction description |
| timestamp | Long | No | Unix timestamp (ms) |
| type | TransactionType | No | EXPENSE/INCOME/TRANSFER |
| categoryName | String | No | Category name |
| merchant | String | Yes | Merchant/payee name |
| walletId | Long | No | FK to wallet (CASCADE) |
| personId | Long | Yes | FK to person (SET_NULL) |
| groupId | Long | Yes | FK to group (CASCADE) |
| smsId | String | Yes | SMS hash for dedup |

---

### WalletEntity

**Location**: `data/local/entity/WalletEntity.kt`

```kotlin
@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: WalletType,          // BANK, CASH, CREDIT
    val balance: Long = 0,         // In paisa
    val colorHex: String,
    val accountNumber: String? = null
)
```

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Auto-generated primary key |
| name | String | No | Wallet display name |
| type | WalletType | No | BANK/CASH/CREDIT |
| balance | Long | No | Current balance in paisa |
| colorHex | String | No | UI display color |
| accountNumber | String | Yes | Last 4 digits of account |

---

### PersonEntity

**Location**: `data/local/entity/PersonEntity.kt`

```kotlin
@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val currentBalance: Long = 0   // Positive = they owe you
)
```

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Auto-generated primary key |
| name | String | Person's name |
| currentBalance | Long | Debt balance (+ = owed to you) |

---

### CategoryEntity

**Location**: `data/local/entity/CategoryEntity.kt`

```kotlin
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String,              // "EXPENSE" or "INCOME"
    val colorHex: String,
    val iconId: Int,
    val isSystemDefault: Boolean = false
)
```

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Auto-generated primary key |
| name | String | Category name (unique) |
| type | String | EXPENSE or INCOME |
| colorHex | String | UI display color |
| iconId | Int | Icon identifier (0-25, maps to Material Icons) |
| isSystemDefault | Boolean | Prevent user deletion |

**Icon ID Mapping:**

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

---

### BudgetEntity

**Location**: `data/local/entity/BudgetEntity.kt`

```kotlin
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryName: String,
    val limitAmount: Long,         // In paisa
    val colorHex: String,
    val rolloverEnabled: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis()
)
```

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Auto-generated primary key |
| categoryName | String | Category to budget |
| limitAmount | Long | Monthly limit in paisa |
| colorHex | String | UI display color |
| rolloverEnabled | Boolean | Carry unused budget |
| createdTimestamp | Long | Creation timestamp |

---

### GroupEntity

**Location**: `data/local/entity/GroupEntity.kt`

```kotlin
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String = GroupType.GENERAL.name,
    val budgetLimit: Long = 0L,     // For trips (in paisa)
    val targetAmount: Long = 0L,    // For savings goals (in paisa)
    val description: String = "",
    val colorHex: String = "#2196F3",
    val iconId: Int = 0,
    val isActive: Boolean = true,
    val createdTimestamp: Long = System.currentTimeMillis()
)

enum class GroupType {
    GENERAL,        // All features combined (default)
    TRIP,           // Group By Category - budget tracking with expenses
    SPLIT_EXPENSE,  // Expense sharing with settlement suggestions
    SAVINGS_GOAL    // Collaborative target with contributions
}
```

**UI Display Names:** General, Group By Category, Split Bill, Savings

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Auto-generated primary key |
| name | String | Group name (e.g., "Office Supplies") |
| type | String | GroupType enum value |
| budgetLimit | Long | Budget cap for trips (in paisa) |
| targetAmount | Long | Goal amount for savings (in paisa) |
| description | String | Optional description |
| colorHex | String | UI display color |
| iconId | Int | Icon identifier |
| isActive | Boolean | Whether group is ongoing |
| createdTimestamp | Long | Creation timestamp |

---

### GroupMemberEntity

**Location**: `data/local/entity/GroupMemberEntity.kt`

```kotlin
@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "memberId"],
    foreignKeys = [
        ForeignKey(entity = GroupEntity::class, ...),
        ForeignKey(entity = PersonEntity::class, ...)
    ]
)
data class GroupMemberEntity(
    val groupId: Long,
    val memberId: Long,
    val isOwner: Boolean = false,
    val joinedTimestamp: Long = System.currentTimeMillis()
)
```

| Field | Type | Description |
|-------|------|-------------|
| groupId | Long | FK to group (CASCADE) |
| memberId | Long | FK to person (CASCADE) |
| isOwner | Boolean | Whether this member created the group |
| joinedTimestamp | Long | When member was added |

---

### GroupTransactionEntity

**Location**: `data/local/entity/GroupTransactionEntity.kt`

```kotlin
@Entity(
    tableName = "group_transactions",
    foreignKeys = [
        ForeignKey(entity = GroupEntity::class, ...),
        ForeignKey(entity = TransactionEntity::class, ...),
        ForeignKey(entity = PersonEntity::class, ...)
    ]
)
data class GroupTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupId: Long,
    val transactionId: Long,
    val paidByPersonId: Long? = null, // null = paid by current user
    val paidByUserSelf: Boolean = true,
    val splitType: String = SplitType.EQUAL.name,
    val tag: String = ""
)

enum class SplitType {
    EQUAL,      // Split equally among all members
    CUSTOM,     // Custom amounts per member
    PERCENTAGE, // Percentage-based split
    SHARES      // Share-based split
}
```

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Auto-generated primary key |
| groupId | Long | No | FK to group (CASCADE) |
| transactionId | Long | No | FK to transaction (CASCADE) |
| paidByPersonId | Long | Yes | Who paid (null = current user) |
| paidByUserSelf | Boolean | No | True if current user paid |
| splitType | String | No | SplitType enum value |
| tag | String | No | Custom tag within group |

---

### GroupSplitEntity

**Location**: `data/local/entity/GroupSplitEntity.kt`

```kotlin
@Entity(
    tableName = "group_splits",
    foreignKeys = [
        ForeignKey(entity = GroupTransactionEntity::class, ...),
        ForeignKey(entity = PersonEntity::class, ...)
    ]
)
data class GroupSplitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupTransactionId: Long,
    val memberId: Long? = null,     // null = current user
    val isSelfUser: Boolean = false,
    val shareAmount: Long,          // Amount owed (in paisa)
    val isPaid: Boolean = false
)
```

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Auto-generated primary key |
| groupTransactionId | Long | No | FK to group transaction (CASCADE) |
| memberId | Long | Yes | Who owes (null = current user) |
| isSelfUser | Boolean | No | True if represents current user |
| shareAmount | Long | No | Amount owed in paisa |
| isPaid | Boolean | No | Settlement tracking |

---

### GroupContributionEntity

**Location**: `data/local/entity/GroupContributionEntity.kt`

```kotlin
@Entity(
    tableName = "group_contributions",
    foreignKeys = [
        ForeignKey(entity = GroupEntity::class, ...),
        ForeignKey(entity = PersonEntity::class, ...)
    ]
)
data class GroupContributionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupId: Long,
    val memberId: Long? = null,     // null = current user
    val isSelfUser: Boolean = true,
    val amount: Long,               // in paisa
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)
```

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Auto-generated primary key |
| groupId | Long | No | FK to group (CASCADE) |
| memberId | Long | Yes | Contributor (null = current user) |
| isSelfUser | Boolean | No | True if current user contributed |
| amount | Long | No | Contribution amount in paisa |
| timestamp | Long | No | When contribution was made |
| note | String | No | Optional note |

---

### SettlementEntity

**Location**: `data/local/entity/SettlementEntity.kt`

```kotlin
@Entity(
    tableName = "settlements",
    foreignKeys = [
        ForeignKey(entity = GroupEntity::class, ...),
        ForeignKey(entity = PersonEntity::class, ...), // fromPersonId
        ForeignKey(entity = PersonEntity::class, ...)  // toPersonId
    ]
)
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupId: Long,
    val fromPersonId: Long? = null, // null = current user
    val fromSelfUser: Boolean = false,
    val toPersonId: Long? = null,   // null = current user
    val toSelfUser: Boolean = true,
    val amount: Long,               // in paisa
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)
```

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Auto-generated primary key |
| groupId | Long | No | FK to group (CASCADE) |
| fromPersonId | Long | Yes | Who paid (null = current user) |
| fromSelfUser | Boolean | No | True if current user paid |
| toPersonId | Long | Yes | Who received (null = current user) |
| toSelfUser | Boolean | No | True if current user received |
| amount | Long | No | Settlement amount in paisa |
| timestamp | Long | No | When settlement was made |
| note | String | No | Optional note |

---

### SubscriptionEntity

**Location**: `data/local/entity/SubscriptionEntity.kt`

```kotlin
@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val merchantName: String,
    val amount: Long,              // In paisa
    val frequency: String,         // "MONTHLY", "YEARLY"
    val nextDueDate: Long,         // Unix timestamp
    val colorHex: String,
    val isAutoDetected: Boolean = false
)
```

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Auto-generated primary key |
| merchantName | String | Service/company name |
| amount | Long | Recurring amount in paisa |
| frequency | String | MONTHLY or YEARLY |
| nextDueDate | Long | Next payment timestamp |
| colorHex | String | UI display color |
| isAutoDetected | Boolean | True if AI-detected |

---

### NotificationEntity

**Location**: `data/local/entity/NotificationEntity.kt`

```kotlin
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val type: String,              // NotificationType as string
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val referenceId: Long? = null, // ID of related entity
    val referenceType: String? = null // "TRANSACTION", "BUDGET", etc.
)
```

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Auto-generated primary key |
| title | String | No | Notification title |
| message | String | No | Notification message body |
| type | String | No | NotificationType enum value |
| timestamp | Long | No | Creation timestamp |
| isRead | Boolean | No | Read status (default: false) |
| referenceId | Long | Yes | FK to related entity |
| referenceType | String | Yes | Entity type for navigation |

**Notification Types:**
```kotlin
enum class NotificationType {
    INCOME,        // Disabled (not generated)
    EXPENSE,       // Disabled (not generated)
    BUDGET_ALERT,  // Budget 80%/100% threshold
    UPCOMING_BILL, // Subscription due 7, 3, 2, 1, 0 days before, 1 day after
    DEBT,          // One-time when person first has non-zero balance
    DEBT_REMINDER, // Weekly Sunday 9 AM summary
    TRIP,          // Group progress (legacy, rarely used)
    SUMMARY        // Fallback for unknown types
}
```

**Reference Types for Navigation:**
| referenceType | Target Screen |
|---------------|---------------|
| TRANSACTION | Ledger |
| BUDGET | Insights |
| SUBSCRIPTION | Insights |
| PERSON | Wallet |
| GROUP | Ledger (Groups tab) |
| WEEKLY_REMINDER | Wallet |

---

## Type Converters

**Location**: `data/local/AppTypeConverters.kt`

```kotlin
class AppTypeConverters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType =
        TransactionType.valueOf(value)

    @TypeConverter
    fun fromWalletType(type: WalletType): String = type.name

    @TypeConverter
    fun toWalletType(value: String): WalletType =
        WalletType.valueOf(value)
}
```

---

## Enums

### TransactionType

**Location**: `data/local/entity/TransactionType.kt`

```kotlin
enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}
```

### WalletType

**Location**: `data/local/entity/WalletType.kt`

```kotlin
enum class WalletType {
    BANK,
    CASH,
    CREDIT
}
```

---

## DAO Methods

**Location**: `data/local/FinanceDao.kt`

### Transaction Queries

```kotlin
// Get recent transactions (last 5) - excludes group transactions
@Query("SELECT * FROM transactions WHERE groupId IS NULL ORDER BY timestamp DESC LIMIT 5")
fun getRecentTransactions(): Flow<List<TransactionEntity>>

// Get all transactions - excludes group transactions
@Query("SELECT * FROM transactions WHERE groupId IS NULL ORDER BY timestamp DESC")
fun getAllTransactions(): Flow<List<TransactionEntity>>

// Search transactions - excludes group transactions
@Query("""
    SELECT * FROM transactions
    WHERE groupId IS NULL
    AND (note LIKE '%' || :query || '%'
    OR categoryName LIKE '%' || :query || '%'
    OR merchant LIKE '%' || :query || '%')
    ORDER BY timestamp DESC
""")
fun searchTransactions(query: String): Flow<List<TransactionEntity>>

// Get transactions by date range - excludes group transactions
@Query("""
    SELECT * FROM transactions
    WHERE groupId IS NULL
    AND timestamp BETWEEN :startTime AND :endTime
    ORDER BY timestamp DESC
""")
fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

// Get transaction by ID
@Query("SELECT * FROM transactions WHERE id = :id")
suspend fun getTransactionById(id: Long): TransactionEntity?
```

**Note:** All list queries exclude transactions with `groupId IS NOT NULL`. Group transactions are only accessible via `GroupDao` queries within the Group Detail screen.

### Wallet Queries

```kotlin
// Get all wallets
@Query("SELECT * FROM wallets")
fun getAllWallets(): Flow<List<WalletEntity>>

// Get total net worth (BANK + CASH assets minus CREDIT liabilities)
@Query("""
    SELECT
        (COALESCE((SELECT SUM(balance) FROM wallets WHERE type IN ('BANK', 'CASH')), 0)) -
        (COALESCE((SELECT ABS(SUM(balance)) FROM wallets WHERE type = 'CREDIT'), 0))
""")
fun getNetWorth(): Flow<Long>

// Update wallet balance (atomic)
@Query("UPDATE wallets SET balance = balance + :amount WHERE id = :id")
suspend fun updateBalance(id: Long, amount: Long)
```

### Cash Flow Queries

```kotlin
// Monthly income - excludes group transactions
@Query("""
    SELECT COALESCE(SUM(amount), 0) FROM transactions
    WHERE groupId IS NULL
    AND type = 'INCOME'
    AND timestamp BETWEEN :startTime AND :endTime
""")
fun getMonthlyIncome(startTime: Long, endTime: Long): Flow<Long>

// Monthly expense - excludes group transactions
@Query("""
    SELECT COALESCE(SUM(amount), 0) FROM transactions
    WHERE groupId IS NULL
    AND type = 'EXPENSE'
    AND timestamp BETWEEN :startTime AND :endTime
""")
fun getMonthlyExpense(startTime: Long, endTime: Long): Flow<Long>
```

**Note:** Cash flow calculations exclude group transactions to prevent double-counting shared expenses.

### Category Analytics

```kotlin
// Category breakdown (expense) - excludes group transactions
@Query("""
    SELECT categoryName, SUM(amount) as total
    FROM transactions
    WHERE groupId IS NULL
    AND type = 'EXPENSE'
    GROUP BY categoryName
    ORDER BY total DESC
""")
fun getCategoryBreakdown(): Flow<List<CategoryTuple>>

// Category breakdown (income) - excludes group transactions
@Query("""
    SELECT categoryName, SUM(amount) as total
    FROM transactions
    WHERE groupId IS NULL
    AND type = 'INCOME'
    GROUP BY categoryName
    ORDER BY total DESC
""")
fun getIncomeCategoryBreakdown(): Flow<List<CategoryTuple>>
```

**Note:** Category analytics exclude group transactions. Group spending breakdown is available within each group's Categories tab.

### Insert/Update/Delete

```kotlin
// Insert transaction
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertTransaction(transaction: TransactionEntity): Long

// Update transaction
@Update
suspend fun updateTransaction(transaction: TransactionEntity)

// Delete transaction
@Delete
suspend fun deleteTransaction(transaction: TransactionEntity)

// Insert wallet
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertWallet(wallet: WalletEntity): Long

// Update person balance (atomic)
@Query("UPDATE persons SET currentBalance = currentBalance + :amount WHERE id = :personId")
suspend fun updatePersonBalance(personId: Long, amount: Long)
```

### Deduplication

```kotlin
// Check if transaction exists (for SMS dedup)
@Query("""
    SELECT EXISTS(
        SELECT 1 FROM transactions
        WHERE amount = :amount
        AND merchant = :merchant
        AND timestamp BETWEEN :startTime AND :endTime
    )
""")
suspend fun checkTransactionExists(
    amount: Long,
    merchant: String,
    startTime: Long,
    endTime: Long
): Boolean

// Check SMS ID exists (single)
@Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE smsId = :smsId)")
suspend fun checkSmsIdExists(smsId: String): Boolean

// Batch check SMS IDs (optimized - single query for multiple IDs)
@Query("SELECT smsId FROM transactions WHERE smsId IN (:smsIds)")
suspend fun getExistingSmsIds(smsIds: List<Long>): List<Long>
```

**SMS Deduplication Strategy:**

- Uses batch query `getExistingSmsIds()` to fetch all existing SMS IDs in a single database call
- Performs in-memory filtering to find new transactions
- Avoids N+1 query problem (previously queried per SMS)

### Category Management

```kotlin
// Get all categories
@Query("SELECT * FROM categories ORDER BY name ASC")
fun getAllCategories(): Flow<List<CategoryEntity>>

// Get category count
@Query("SELECT COUNT(*) FROM categories")
suspend fun getCategoryCount(): Int

// Check if category name exists
@Query("SELECT COUNT(*) FROM categories WHERE name = :name")
suspend fun checkCategoryNameExists(name: String): Int

// Delete duplicate categories (keeps oldest by ID)
@Query("DELETE FROM categories WHERE id NOT IN (SELECT MIN(id) FROM categories GROUP BY name)")
suspend fun deleteDuplicateCategories()

// Insert category
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertCategory(category: CategoryEntity)
```

---

## Data Mappers

**Location**: `data/local/mapper/DomainMappers.kt`

```kotlin
// Wallet to UI Model
fun WalletEntity.toAccountUiModel(currencyCode: String): AccountUiModel {
    return AccountUiModel(
        id = id,
        name = name,
        balanceFormatted = balance.formatAsCurrency(currencyCode),
        balanceRaw = balance,
        type = type,
        color = Color(android.graphics.Color.parseColor(colorHex))
    )
}

// Transaction to UI Model
fun TransactionEntity.toTransactionUiModel(currencyCode: String): TransactionUiModel {
    return TransactionUiModel(
        id = id,
        title = note,
        amountFormatted = amount.formatAsCurrency(currencyCode),
        amountRaw = amount,
        dateFormatted = DateUtil.formatDateWithTime(timestamp, context), // "14 Mar 2026, 02:47 PM"
        timestamp = timestamp,
        category = categoryName,
        type = type,
        merchant = merchant,
        isExpense = type == TransactionType.EXPENSE
    )
}

// Person to UI Model
fun PersonEntity.toPersonUiModel(currencyCode: String): PersonUiModel {
    val statusText = when {
        currentBalance > 0 -> "Owes you"
        currentBalance < 0 -> "You owe"
        else -> "Settled"
    }

    val statusColor = when {
        currentBalance > 0 -> Color.Green
        currentBalance < 0 -> Color(0xFFFF9800)
        else -> Color.Gray
    }

    return PersonUiModel(
        id = id,
        name = name,
        balanceFormatted = abs(currentBalance).formatAsCurrency(currencyCode),
        balanceRaw = currentBalance,
        statusText = statusText,
        statusColor = statusColor
    )
}
```

---

## CategoryTuple

**Location**: `data/local/CategoryTuple.kt`

```kotlin
data class CategoryTuple(
    val categoryName: String,
    val total: Long
)
```

Used for category breakdown queries where only name and sum are needed.

---

## Bulk Operations

### Clear All Data (Logout)

When a user logs out with "Delete Everything", the following tables are cleared in order:

```kotlin
// TransactionRepositoryImpl.kt
suspend fun clearAllData() {
    financeDao.deleteAllTransactions()
    financeDao.deleteAllWallets()
    financeDao.deleteAllPeople()
    financeDao.deleteAllBudgets()
    financeDao.deleteAllSubscriptions()
    notificationDao.deleteAllNotifications()
    groupDao.deleteAllSettlements()
    groupDao.deleteAllGroupSplits()
    groupDao.deleteAllGroupContributions()
    groupDao.deleteAllGroupTransactions()
    groupDao.deleteAllGroupMembers()
    groupDao.deleteAllGroups()
    // Categories are NOT deleted - they're re-seeded after logout
}
```

**Important**: Categories are preserved and re-seeded via `seedDefaultCategories()` after logout to ensure system categories always exist.

---

## Database Transactions

For operations that affect multiple tables, use Room's `@Transaction`:

```kotlin
// In FinanceRepositoryImpl.kt
suspend fun addTransaction(transaction: TransactionEntity) {
    database.withTransaction {
        // Insert transaction
        val id = financeDao.insertTransaction(transaction)

        // Update wallet balance
        val impact = when (transaction.type) {
            TransactionType.EXPENSE -> -transaction.amount
            TransactionType.INCOME -> transaction.amount
            TransactionType.TRANSFER -> -transaction.amount
        }
        financeDao.updateWalletBalance(transaction.walletId, impact)

        // Update person balance if linked
        transaction.personId?.let { personId ->
            financeDao.updatePersonBalance(personId, transaction.amount)
        }
    }
}
```

This ensures ACID compliance for balance updates.
