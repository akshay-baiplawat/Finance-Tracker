# Screens Documentation

This document describes all UI screens, their components, and navigation flows.

---

## Navigation Structure

```
MainActivity
    │
    └── MainScreen (NavHost + Bottom Navigation)
            │
            ├── HomeScreen ("/home")
            │   └── DashboardViewModel
            │
            ├── LedgerScreen ("/ledger")
            │   └── LedgerViewModel
            │
            ├── WalletScreen ("/wallet")
            │   └── WalletViewModel
            │
            ├── InsightsScreen ("/insights")
            │   └── InsightsViewModel
            │
            ├── ProfileScreen ("/profile")
            │   └── SettingsViewModel
            │
            ├── CategoryListScreen ("/categories")
            │   └── CategoryViewModel
            │
            ├── NotificationScreen ("/notifications")
            │   └── NotificationViewModel
            │
            ├── UserProfileScreen ("/user")
            │   └── UserProfileViewModel
            │
            ├── PrivacyPolicyScreen ("/privacyPolicy")
            │   └── (no ViewModel - stateless)
            │
            └── GroupDetailScreen ("/groupDetail/{groupId}")
                └── GroupViewModel
```

### Bottom Navigation Tabs

| Tab | Icon | Route | Description |
|-----|------|-------|-------------|
| Home | Home | `/home` | Dashboard overview |
| Ledger | List | `/ledger` | All transactions |
| (FAB) | + | - | Add transaction |
| Wallet | Wallet | `/wallet` | Accounts & people |
| Insights | Chart | `/insights` | Analytics & budgets |

---

## Screen Details

### 1. HomeScreen (Dashboard)

**Location**: `presentation/home/HomeScreen.kt`
**ViewModel**: `DashboardViewModel`

#### Layout

```
┌────────────────────────────────────────────┐
│  Header                                    │
│  ┌─────┐              ┌────┐ ┌────┐        │
│  │Avatar│  User Name   │Bell│ │Gear│       │
│  └─────┘              └────┘ └────┘        │
├────────────────────────────────────────────┤
│  Filter Chips                              │
│  [All] [Daily] [Weekly] [Monthly]          │
├────────────────────────────────────────────┤
│  Net Worth Card                            │
│  ┌────────────────────────────────────────┐│
│  │  Net Worth            Privacy Toggle   ││
│  │  ₹1,25,000               👁            ││
│  │  +5.2% this month                      ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Income & Expense Card                     │
│  ┌───────────────────┬────────────────────┐│
│  │  Income           │     [Donut]        ││
│  │  ₹50,000          │     [Chart]        ││
│  │                   │                    ││
│  │  Expense          │                    ││
│  │  ₹35,000          │                    ││
│  └───────────────────┴────────────────────┘│
├────────────────────────────────────────────┤
│  My Accounts (Horizontal Scroll)           │
│  ┌────────┐ ┌────────┐ ┌────────┐          │
│  │ HDFC   │ │ Cash   │ │ ICICI  │ →        │
│  │₹50,000 │ │₹10,000 │ │₹35,000 │          │
│  └────────┘ └────────┘ └────────┘          │
├────────────────────────────────────────────┤
│  Recent Transactions                       │
│  ┌────────────────────────────────────────┐│
│  │ 🍕 Food           -₹500      10 Mar    ││
│  │ 💰 Salary        +₹50,000    1 Mar     ││
│  │ 🛒 Shopping       -₹2,000    28 Feb    ││
│  └────────────────────────────────────────┘│
└────────────────────────────────────────────┘
```

#### Components

| Component | Description |
|-----------|-------------|
| NetWorthCard | Shows total net worth with % change |
| IncomeSpentCard | Side-by-side income/expense with donut chart |
| AccountsRow | Horizontal scrolling wallet cards |
| TransactionList | Last 5 transactions |
| FilterChips | Time range filter (All/Daily/Weekly/Monthly) |

#### State

```kotlin
data class DashboardUiState(
    val netWorth: String = "₹0",
    val netWorthRaw: Long = 0,
    val netWorthChange: String = "+0%",
    val monthlyIncome: String = "₹0",
    val monthlyExpense: String = "₹0",
    val accounts: List<AccountUiModel> = emptyList(),
    val recentTransactions: List<TransactionUiModel> = emptyList(),
    val selectedTimeRange: TimeRange = TimeRange.ALL,
    val isPrivacyMode: Boolean = false
)
```

---

### 2. LedgerScreen

**Location**: `presentation/ledger/LedgerScreen.kt`
**ViewModel**: `LedgerViewModel`

#### Layout

```
┌────────────────────────────────────────────┐
│  Tab Bar                                   │
│  [All Transactions] [People] [Groups]      │
├────────────────────────────────────────────┤
│  Search Bar                                │
│  ┌────────────────────────────────────────┐│
│  │ 🔍 Search transactions...              ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Filter Chips                              │
│  [All] [Income] [Expense] [Transfer]       │
├────────────────────────────────────────────┤
│  Transaction List                          │
│  ┌────────────────────────────────────────┐│
│  │ March 2025                             ││
│  ├────────────────────────────────────────┤│
│  │ 🍕 Lunch at Restaurant                 ││
│  │    Food • HDFC Bank         -₹500      ││
│  │    10 Mar, 1:30 PM                     ││
│  ├────────────────────────────────────────┤│
│  │ 💰 March Salary                        ││
│  │    Salary • Cash           +₹50,000    ││
│  │    1 Mar, 10:00 AM                     ││
│  └────────────────────────────────────────┘│
└────────────────────────────────────────────┘
```

#### Features

- **Search**: Real-time search with 300ms debounce
- **Filters**: All, Income, Expense, Transfer
- **Tabs**: All Transactions, People, Groups
- **Actions**: Long-press to edit/delete

#### State

```kotlin
data class LedgerUiState(
    val transactions: List<TransactionUiModel> = emptyList(),
    val people: List<PersonUiModel> = emptyList(),
    val groups: List<GroupUiModel> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: TransactionFilter = TransactionFilter.ALL,
    val selectedTab: Int = 0,  // 0=Transactions, 1=People, 2=Groups
    val currencySymbol: String = "₹"
)
```

---

### 3. WalletScreen

**Location**: `presentation/wallet/WalletScreen.kt`
**ViewModel**: `WalletViewModel`

#### Layout

```
┌────────────────────────────────────────────┐
│  Tab Bar                                   │
│  [My Accounts] [People]                    │
├────────────────────────────────────────────┤
│  MY ACCOUNTS TAB                           │
├────────────────────────────────────────────┤
│  Assets                                    │
│  ┌────────────────────────────────────────┐│
│  │ 🏦 HDFC Bank              ₹50,000      ││
│  │    Bank Account                        ││
│  ├────────────────────────────────────────┤│
│  │ 💵 Cash                   ₹10,000      ││
│  │    Cash                                ││
│  └────────────────────────────────────────┘│
│  Total Assets: ₹60,000                     │
├────────────────────────────────────────────┤
│  Liabilities                               │
│  ┌────────────────────────────────────────┐│
│  │ 💳 ICICI Credit Card      ₹15,000      ││
│  │    Credit Card                         ││
│  └────────────────────────────────────────┘│
│  Total Liabilities: ₹15,000                │
├────────────────────────────────────────────┤
│  PEOPLE TAB                                │
├────────────────────────────────────────────┤
│  ┌────────────────────────────────────────┐│
│  │ 👤 John                                ││
│  │    Owes you               ₹5,000       ││
│  ├────────────────────────────────────────┤│
│  │ 👤 Sarah                               ││
│  │    You owe                ₹2,000       ││
│  └────────────────────────────────────────┘│
│                                            │
│                          [+ Add] FAB       │
└────────────────────────────────────────────┘
```

#### Features

- **Tabs**: My Accounts, People
- **Asset/Liability**: Automatic categorization by wallet type
- **Debt Status**: Color-coded (green = owed to you, orange = you owe)
- **FAB**: Context-aware (Add Wallet or Add Person based on tab)

#### State

```kotlin
data class WalletUiState(
    val assets: List<AccountUiModel> = emptyList(),
    val liabilities: List<AccountUiModel> = emptyList(),
    val totalAssets: String = "₹0",
    val totalLiabilities: String = "₹0",
    val people: List<PersonUiModel> = emptyList(),
    val totalOwedToYou: String = "₹0",
    val totalYouOwe: String = "₹0",
    val selectedTab: WalletTab = WalletTab.ACCOUNTS
)
```

---

### 4. InsightsScreen

**Location**: `presentation/insights/InsightsScreen.kt`
**ViewModel**: `InsightsViewModel`

#### Layout

```
┌────────────────────────────────────────────┐
│  Tab Bar                                   │
│  [Overview] [Budgets] [Subscriptions]      │
├────────────────────────────────────────────┤
│  OVERVIEW TAB                              │
├────────────────────────────────────────────┤
│  Expense Breakdown                         │
│  ┌────────────────────────────────────────┐│
│  │      [Pie Chart]                       ││
│  │                                        ││
│  │  Food         ████████  40%  ₹14,000   ││
│  │  Shopping     █████     25%  ₹8,750    ││
│  │  Transport    ████      20%  ₹7,000    ││
│  │  Other        ███       15%  ₹5,250    ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Income Breakdown                          │
│  ┌────────────────────────────────────────┐│
│  │  Salary       ██████████ 80%  ₹50,000  ││
│  │  Freelance    ████       20%  ₹12,500  ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  BUDGETS TAB                               │
├────────────────────────────────────────────┤
│  ┌────────────────────────────────────────┐│
│  │ 🍕 Food                                ││
│  │ ████████████░░░░  ₹14,000 / ₹20,000   ││
│  │ 70% used                               ││
│  ├────────────────────────────────────────┤│
│  │ 🛒 Shopping                            ││
│  │ ██████████████████  ₹8,750 / ₹8,000   ││
│  │ ⚠️ Over budget!                        ││
│  └────────────────────────────────────────┘│
│                          [+ Add Budget]    │
├────────────────────────────────────────────┤
│  SUBSCRIPTIONS TAB                         │
├────────────────────────────────────────────┤
│  ┌────────────────────────────────────────┐│
│  │ 🎵 Spotify           ₹119/month        ││
│  │    Due: Mar 15                         ││
│  ├────────────────────────────────────────┤│
│  │ 📺 Netflix           ₹649/month        ││
│  │    Due: Mar 20                         ││
│  ├────────────────────────────────────────┤│
│  │ 🔍 Amazon (Detected) ₹1,499/month      ││
│  │    ~85% confidence                     ││
│  └────────────────────────────────────────┘│
│                      [+ Add Subscription]  │
└────────────────────────────────────────────┘
```

#### Features

- **Overview**: Pie charts for expense/income breakdown
- **Budgets**: Progress bars with color coding
- **Subscriptions**: Manual + AI-detected (grayed out)
- **Rollover**: Shows carried budget from previous month

#### State

```kotlin
data class InsightsUiState(
    val expenseBreakdown: List<CategoryBreakdownUiModel> = emptyList(),
    val incomeBreakdown: List<CategoryBreakdownUiModel> = emptyList(),
    val budgets: List<BudgetUiModel> = emptyList(),
    val subscriptions: List<SubscriptionUiModel> = emptyList(),
    val selectedTab: InsightsTab = InsightsTab.OVERVIEW
)
```

---

### 5. ProfileScreen

**Location**: `presentation/profile/ProfileScreen.kt`
**ViewModel**: `SettingsViewModel`

#### Layout

```
┌────────────────────────────────────────────┐
│  Profile Header                            │
│  ┌─────────┐                               │
│  │ Avatar  │  User Name                    │
│  │   AK    │  alex.kumar@email.com         │
│  └─────────┘                               │
├────────────────────────────────────────────┤
│  Privacy & Security                        │
│  ┌────────────────────────────────────────┐│
│  │ 🔒 Privacy Mode          [ Toggle ]    ││
│  │ ✈️ Trip Mode             [ Toggle ]    ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Data Management                           │
│  ┌────────────────────────────────────────┐│
│  │ 🔄 Sync SMS Data                    >  ││
│  │ 📦 Backup & Restore                 >  ││ ← Opens DataManagementSheet
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Preferences                               │
│  ┌────────────────────────────────────────┐│
│  │ 🔔 Notifications         [ Toggle ]    ││
│  │ 🔄 Auto SMS Sync         [ Toggle ]    ││
│  │ 🌙 Dark Mode             [ Toggle ]    ││
│  │ 🔤 Display Size     [XS][S][M][L]      ││
│  │ 📁 Categories                       >  ││
│  │ 🌐 Currency              INR (₹)    >  ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Support                                   │
│  ┌────────────────────────────────────────┐│
│  │ ❓ Help Center                      >  ││
│  │ 📄 Privacy Policy                   >  ││
│  │ 🚪 Log Out                          >  ││
│  └────────────────────────────────────────┘│
└────────────────────────────────────────────┘
```

#### Data Management Sheet (Bottom Sheet)

Opened when user taps "Backup & Restore":

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

#### Features

- **Backup & Restore**: Opens DataManagementSheet with Import/Export/Clear
- **Currency Picker**: Opens CurrencyPickerSheet
- **Auto SMS Sync**: Toggle for 15-minute periodic SMS sync
- **Theme**: Light, Dark toggle
- **Display Size**: Segmented buttons (XS/S/M/L) for UI scaling
- **Categories**: Navigate to CategoryListScreen
- **2-Step Delete Confirmation**: Inline card → AlertDialog

---

### 6. CategoryListScreen

**Location**: `presentation/category/CategoryListScreen.kt`
**ViewModel**: `CategoryViewModel`

#### Layout

```
┌────────────────────────────────────────────┐
│  ← Categories                              │
├────────────────────────────────────────────┤
│  Tab Bar                                   │
│  [Expense] [Income]                        │
├────────────────────────────────────────────┤
│  Category List                             │
│  ┌────────────────────────────────────────┐│
│  │ 🍕 Food & Dining                       ││
│  ├────────────────────────────────────────┤│
│  │ 🛒 Shopping                            ││
│  ├────────────────────────────────────────┤│
│  │ 🚗 Transportation                      ││
│  ├────────────────────────────────────────┤│
│  │ 🎬 Entertainment                       ││
│  ├────────────────────────────────────────┤│
│  │ + Add Category                         ││
│  └────────────────────────────────────────┘│
└────────────────────────────────────────────┘
```

#### Features

- **Tabs**: Expense, Income
- **Edit/Delete**: Swipe or long-press (except system defaults)
- **Add**: Opens create category dialog with icon picker
- **Icon Picker**: Grid of 25+ Material icons for category customization
- **System Categories**: Protected from deletion, icon shown but not editable

---

### 7. NotificationScreen

**Location**: `presentation/notifications/NotificationScreen.kt`
**ViewModel**: `NotificationViewModel`

#### Layout

```
┌────────────────────────────────────────────┐
│  ← Notifications              Mark all read│
├────────────────────────────────────────────┤
│  Tab Bar                                   │
│  [All] [Unread]                            │
├────────────────────────────────────────────┤
│  Notification List                         │
│  ┌────────────────────────────────────────┐│
│  │ Budget Alert                           ││
│  │ Food budget is at 85% (₹17,000/₹20,000)││
│  │ 2 hours ago                        ●   ││
│  ├────────────────────────────────────────┤│
│  │ Upcoming Bill                          ││
│  │ Netflix subscription due in 2 days     ││
│  │ Yesterday                          ●   ││
│  ├────────────────────────────────────────┤│
│  │ Debt Reminder                          ││
│  │ John owes you ₹5,000                   ││
│  │ 3 days ago                             ││
│  └────────────────────────────────────────┘│
│                                            │
│  Empty State (when no notifications):      │
│  ┌────────────────────────────────────────┐│
│  │     🔔 No notifications yet            ││
│  └────────────────────────────────────────┘│
└────────────────────────────────────────────┘
```

#### Features

- **Tabs**: All, Unread (filters notifications)
- **Mark all read**: Button in header
- **Swipe-to-delete**: Swipe left or right to delete
- **Auto-cleanup**: Old notifications (>30 days) removed
- **Relative time**: Just now, 2 mins ago, Yesterday, etc.
- **Navigation**: Tap notification to navigate to relevant screen

#### State

```kotlin
data class NotificationUiState(
    val notifications: List<NotificationUiModel> = emptyList(),
    val selectedTab: NotificationTab = NotificationTab.ALL,
    val unreadCount: Int = 0
)
```

#### Notification Types

| Type | Icon | Description |
|------|------|-------------|
| BUDGET_ALERT | Chart | Budget reached 80%/100% |
| UPCOMING_BILL | Calendar | Subscription due within 3 days |
| DEBT | Person | Money owed to/from someone |
| TRIP | Plane | Event/trip progress update |

---

### 8. OnboardingScreen

**Location**: `presentation/onboarding/OnboardingScreen.kt`

#### Layout

```
┌────────────────────────────────────────────┐
│                                            │
│           [App Logo]                       │
│                                            │
│      Welcome to Finance Tracker            │
│                                            │
│   Take control of your finances with       │
│   automatic SMS transaction tracking       │
│                                            │
│  ┌────────────────────────────────────────┐│
│  │                                        ││
│  │  We need permission to read your SMS   ││
│  │  to automatically import transactions  ││
│  │                                        ││
│  └────────────────────────────────────────┘│
│                                            │
│        [Grant Permission]                  │
│                                            │
│         Skip for now                       │
│                                            │
└────────────────────────────────────────────┘
```

#### Features

- **Permission Request**: SMS read permission
- **Skip Option**: Continue without SMS sync

---

### 9. GroupDetailScreen

**Location**: `presentation/group/GroupDetailScreen.kt`
**ViewModel**: `GroupViewModel`

#### Layout

```
┌────────────────────────────────────────────┐
│  ← Group Name                    [⋮ Menu]  │
├────────────────────────────────────────────┤
│  Summary Card                              │
│  ┌────────────────────────────────────────┐│
│  │  Total Spent: ₹15,000 / ₹50,000       ││
│  │  ████████████░░░░░  30%               ││
│  │  3 members • 12 expenses              ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Tab Bar                                   │
│  [Expenses] [Balances] [Activity]          │
├────────────────────────────────────────────┤
│  EXPENSES TAB                              │
│  ┌────────────────────────────────────────┐│
│  │ 🍕 Dinner at Restaurant                ││
│  │    Paid by You • Split equally         ││
│  │    ₹1,500 • 10 Mar                     ││
│  ├────────────────────────────────────────┤│
│  │ 🚗 Uber to Airport                     ││
│  │    Paid by John • Split equally        ││
│  │    ₹800 • 9 Mar                        ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  BALANCES TAB                              │
│  ┌────────────────────────────────────────┐│
│  │  Settlement Suggestions                ││
│  │  ───────────────────────               ││
│  │  John → You: ₹500                      ││
│  │  Sarah → You: ₹300                     ││
│  │                                        ││
│  │  [Settle Up]                           ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│                          [+ Add Expense]   │
└────────────────────────────────────────────┘
```

#### Features

- **Summary Card**: Budget progress, member count, expense count
- **Expenses Tab**: All group expenses with who paid
- **Balances Tab**: Settlement suggestions and settle up action
- **Activity Tab**: Chronological history
- **FAB**: Context-aware (Add Expense for trips, Add Contribution for savings)

#### State

```kotlin
data class GroupDetailUiModel(
    val id: Long,
    val name: String,
    val type: String,
    val totalSpent: Long,
    val budgetLimit: Long,
    val targetAmount: Long,
    val memberCount: Int,
    val expenses: List<GroupExpenseUiModel>,
    val settlements: List<SettlementSuggestion>,
    val contributions: List<ContributionUiModel>
)
```

---

## Modal Sheets

### AddTransactionSheet

**Location**: `presentation/add_transaction/AddTransactionSheet.kt`

```
┌────────────────────────────────────────────┐
│  Add Transaction                     [X]   │
├────────────────────────────────────────────┤
│  Type                                      │
│  [Expense] [Income] [Transfer]             │
├────────────────────────────────────────────┤
│  Amount                                    │
│  ┌────────────────────────────────────────┐│
│  │ ₹ 0.00                                 ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Category                                  │
│  ┌────────────────────────────────────────┐│
│  │ Select category...               ▼     ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Wallet                                    │
│  ┌────────────────────────────────────────┐│
│  │ HDFC Bank                        ▼     ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Note (Optional)                           │
│  ┌────────────────────────────────────────┐│
│  │                                        ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Person (Optional - for debts)             │
│  ┌────────────────────────────────────────┐│
│  │ None                             ▼     ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│                                            │
│              [Add Transaction]             │
│                                            │
└────────────────────────────────────────────┘
```

### EditTransactionSheet

**Location**: `presentation/add_transaction/EditTransactionSheet.kt`

Same as AddTransactionSheet but:
- Pre-filled with existing transaction data
- Has "Delete" button
- "Save" instead of "Add"

### AddWalletSheet

**Location**: `presentation/wallet/AddWalletSheet.kt`

```
┌────────────────────────────────────────────┐
│  Add Wallet                          [X]   │
├────────────────────────────────────────────┤
│  Name                                      │
│  ┌────────────────────────────────────────┐│
│  │ e.g., HDFC Savings                     ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Type                                      │
│  [Bank] [Cash] [Credit Card]               │
├────────────────────────────────────────────┤
│  Initial Balance                           │
│  ┌────────────────────────────────────────┐│
│  │ ₹ 0.00                                 ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Color                                     │
│  [🔴] [🟠] [🟡] [🟢] [🔵] [🟣]              │
├────────────────────────────────────────────┤
│                                            │
│              [Add Wallet]                  │
│                                            │
└────────────────────────────────────────────┘
```

### AddBudgetSheet

**Location**: `presentation/insights/AddBudgetSheet.kt`

```
┌────────────────────────────────────────────┐
│  Add Budget                          [X]   │
├────────────────────────────────────────────┤
│  Category                                  │
│  ┌────────────────────────────────────────┐│
│  │ Select category...               ▼     ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Monthly Limit                             │
│  ┌────────────────────────────────────────┐│
│  │ ₹ 0.00                                 ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Rollover unused budget                    │
│  [ Toggle ]                                │
├────────────────────────────────────────────┤
│                                            │
│              [Add Budget]                  │
│                                            │
└────────────────────────────────────────────┘
```

### CurrencyPickerSheet

**Location**: `presentation/profile/CurrencyPickerSheet.kt`

```
┌────────────────────────────────────────────┐
│  Select Currency                     [X]   │
├────────────────────────────────────────────┤
│  ┌────────────────────────────────────────┐│
│  │ ₹  Indian Rupee (INR)           ✓      ││
│  ├────────────────────────────────────────┤│
│  │ $  US Dollar (USD)                     ││
│  ├────────────────────────────────────────┤│
│  │ €  Euro (EUR)                          ││
│  ├────────────────────────────────────────┤│
│  │ £  British Pound (GBP)                 ││
│  └────────────────────────────────────────┘│
└────────────────────────────────────────────┘
```

### AddGroupSheet

**Location**: `presentation/group/AddGroupSheet.kt`

```
┌────────────────────────────────────────────┐
│  Create New Group                    [X]   │
├────────────────────────────────────────────┤
│  Group Type                                │
│  [General] [Trip] [Split Bill] [Savings]   │
├────────────────────────────────────────────┤
│  Group Name                                │
│  ┌────────────────────────────────────────┐│
│  │ e.g., Goa Trip                         ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Budget/Target (optional)                  │
│  ┌────────────────────────────────────────┐│
│  │ ₹ 0.00                                 ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Members                                   │
│  [John] [Sarah] [+ Add New]                │
├────────────────────────────────────────────┤
│                                            │
│              [Create Group]                │
│                                            │
└────────────────────────────────────────────┘
```

### AddGroupExpenseSheet

**Location**: `presentation/group/AddGroupExpenseSheet.kt`

```
┌────────────────────────────────────────────┐
│  Add Expense                         [X]   │
├────────────────────────────────────────────┤
│  Amount                                    │
│  ┌────────────────────────────────────────┐│
│  │ ₹ 0.00                                 ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Description                               │
│  ┌────────────────────────────────────────┐│
│  │ What was this for?                     ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Category                                  │
│  ┌────────────────────────────────────────┐│
│  │ Food & Dining                    ▼     ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Paid By                                   │
│  [You ✓] [John] [Sarah]                    │
├────────────────────────────────────────────┤
│  From Wallet (when You selected)           │
│  ┌────────────────────────────────────────┐│
│  │ HDFC Bank                        ▼     ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Split Type                                │
│  [Equal ✓] [Custom]                        │
├────────────────────────────────────────────┤
│                                            │
│              [Add Expense]                 │
│                                            │
└────────────────────────────────────────────┘
```

### SettleUpSheet

**Location**: `presentation/group/SettleUpSheet.kt`

```
┌────────────────────────────────────────────┐
│  Record Settlement                   [X]   │
├────────────────────────────────────────────┤
│  From                                      │
│  ┌────────────────────────────────────────┐│
│  │ John                             ▼     ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  To                                        │
│  ┌────────────────────────────────────────┐│
│  │ You                              ▼     ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Amount                                    │
│  ┌────────────────────────────────────────┐│
│  │ ₹ 500.00                               ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│                                            │
│              [Record Settlement]           │
│                                            │
└────────────────────────────────────────────┘
```

### AddContributionSheet

**Location**: `presentation/group/AddContributionSheet.kt`

```
┌────────────────────────────────────────────┐
│  Add Contribution                    [X]   │
├────────────────────────────────────────────┤
│  Contributor                               │
│  [You ✓] [John] [Sarah]                    │
├────────────────────────────────────────────┤
│  Amount                                    │
│  ┌────────────────────────────────────────┐│
│  │ ₹ 0.00                                 ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│  Note (optional)                           │
│  ┌────────────────────────────────────────┐│
│  │                                        ││
│  └────────────────────────────────────────┘│
├────────────────────────────────────────────┤
│                                            │
│              [Add Contribution]            │
│                                            │
└────────────────────────────────────────────┘
```

---

## Navigation Flow Diagram

```
                    ┌───────────────┐
                    │ MainActivity  │
                    └───────┬───────┘
                            │
              ┌─────────────┴─────────────┐
              │                           │
     ┌────────▼────────┐       ┌──────────▼──────────┐
     │ OnboardingScreen│       │     MainScreen      │
     │ (First Launch)  │       │   (NavHost + BottomNav)
     └────────┬────────┘       └──────────┬──────────┘
              │                           │
              └───────────────────────────┤
                                          │
        ┌─────────┬─────────┬─────────┬───┴───┐
        │         │         │         │       │
   ┌────▼────┐┌───▼───┐┌────▼────┐┌───▼───┐┌──▼──┐
   │  Home   ││Ledger ││ Wallet  ││Insights││Prof.│
   └────┬────┘└───┬───┘└────┬────┘└───┬───┘└──┬──┘
        │         │         │         │       │
   ┌────▼────┐    │    ┌────▼────┐┌───▼───┐   │
   │Add Trans│    │    │AddWallet││AddBudg│   │
   │  Sheet  │    │    │  Sheet  ││ Sheet │   │
   └─────────┘    │    └─────────┘└───────┘   │
                  │                           │
             ┌────▼────┐              ┌───────▼───────┐
             │Edit Trans│              │CategoryList   │
             │  Sheet   │              │   Screen      │
             └──────────┘              └───────────────┘
```

---

## ViewModels Summary

| ViewModel | Screens | Key State |
|-----------|---------|-----------|
| `DashboardViewModel` | HomeScreen | Net worth, cash flow, transactions |
| `LedgerViewModel` | LedgerScreen | Search, filters, transaction list |
| `WalletViewModel` | WalletScreen | Assets, liabilities, people |
| `InsightsViewModel` | InsightsScreen | Breakdown, budgets, subscriptions |
| `SettingsViewModel` | ProfileScreen | Preferences, data operations |
| `CategoryViewModel` | CategoryListScreen | Categories CRUD |
| `NotificationViewModel` | NotificationScreen | Notifications, unread count |
| `UserProfileViewModel` | UserProfileScreen | User profile fields, avatar |
| `GroupViewModel` | GroupDetailScreen | Groups, expenses, settlements |

---

## 10. PrivacyPolicyScreen

**Location**: `presentation/profile/PrivacyPolicyScreen.kt`
**ViewModel**: None (stateless)

#### Layout

```
┌────────────────────────────────────────────┐
│  ← Privacy Policy                          │
├────────────────────────────────────────────┤
│  Section List (Scrollable)                 │
│  ┌────────────────────────────────────────┐│
│  │ FINANCE TRACKER                        ││
│  │ Last updated: March 14, 2026           ││
│  ├────────────────────────────────────────┤│
│  │ 1. The "Local-First" Guarantee         ││
│  │    Your data never leaves your device  ││
│  ├────────────────────────────────────────┤│
│  │ 2. Permissions We Request              ││
│  │    READ_SMS, Contacts, Camera...       ││
│  ├────────────────────────────────────────┤│
│  │ 3. Data We Collect                     ││
│  │    Crash logs only via Crashlytics     ││
│  ├────────────────────────────────────────┤│
│  │ 4. Your Data Rights                    ││
│  │    DPDP Act & GDPR compliance          ││
│  ├────────────────────────────────────────┤│
│  │ ...more sections...                    ││
│  └────────────────────────────────────────┘│
└────────────────────────────────────────────┘
```

#### Features

- **Scrollable**: LazyColumn for all policy sections
- **Access**: Profile → Support → Privacy Policy
- **Content**: Uses centralized `PrivacyPolicyContent` object
- **13 sections** covering data handling, permissions, rights

#### First-Launch Dialog

On first launch, users must accept the privacy policy:

```
┌────────────────────────────────────────────┐
│  🔒 Privacy Policy                         │
├────────────────────────────────────────────┤
│  Please review our privacy policy          │
│                                            │
│  • Local-First Architecture                │
│  • No Cloud Servers                        │
│  • Your Data, Your Control                 │
│  • Transparent Business Model              │
│                                            │
│         [Decline]    [Accept]              │
└────────────────────────────────────────────┘
```

- Cannot be dismissed by back press or tapping outside
- Decline shows confirmation dialog with Exit App option

