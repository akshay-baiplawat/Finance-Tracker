# CLAUDE.md - Finance Tracker

This file provides context for AI assistants working on this codebase.

## Project Overview

Finance Tracker is an Android personal finance app built with Kotlin and Jetpack Compose. It helps users track expenses, income, budgets, and subscriptions with automatic SMS transaction parsing.

## Architecture

- **Pattern**: MVVM + Clean Architecture
- **UI**: Jetpack Compose (single-activity)
- **Database**: Room (SQLite)
- **DI**: Hilt
- **Async**: Kotlin Coroutines + Flow

## Key Directories

```
app/src/main/java/com/example/financetracker/
├── di/                    # Hilt modules (DatabaseModule, RepositoryModule)
├── presentation/          # UI layer (Screens, ViewModels)
├── domain/                # Business logic (interfaces, models)
├── data/                  # Data layer (Room entities, DAOs, repositories)
└── core/                  # Utilities (currency, date, SMS parsing)
```

## Critical Files

| File | Purpose |
|------|---------|
| `MainActivity.kt` | Single activity, handles permissions and theming |
| `presentation/main/MainScreen.kt` | Navigation hub with bottom nav |
| `data/local/AppDatabase.kt` | Room database definition (v3) |
| `data/local/FinanceDao.kt` | Primary DAO with all queries |
| `data/local/dao/GroupDao.kt` | Group-related queries |
| `data/repository/FinanceRepositoryImpl.kt` | Core business logic |
| `data/repository/TransactionRepositoryImpl.kt` | Transaction operations (clear, batch import) |
| `data/repository/GroupRepositoryImpl.kt` | Group operations (splits, settlements) |
| `core/parser/SmsParserEngine.kt` | SMS transaction parsing |
| `core/parser/CategoryInferrer.kt` | Automatic category inference from merchant names |
| `core/parser/MerchantNormalizer.kt` | Merchant name normalization (aliases, variations) |
| `data/local/dao/MerchantCategoryPreferenceDao.kt` | User category preference DAO |
| `data/local/entity/MerchantCategoryPreferenceEntity.kt` | Stores user category corrections |
| `core/util/CsvExportUtil.kt` | CSV export to Downloads folder |
| `core/util/CsvImportUtil.kt` | CSV import and parsing |
| `domain/logic/SubscriptionLogic.kt` | Subscription detection |
| `presentation/add_transaction/AddTransactionSheet.kt` | Add new transaction bottom sheet |
| `presentation/add_transaction/EditTransactionSheet.kt` | Edit transaction bottom sheet |
| `presentation/group/GroupScreen.kt` | Group list screen |
| `presentation/group/GroupDetailScreen.kt` | Group detail screen |
| `presentation/group/GroupViewModel.kt` | Group state management |
| `presentation/profile/UserProfileScreen.kt` | User profile editing screen |
| `presentation/profile/UserProfileViewModel.kt` | User profile state management |
| `data/repository/UserPreferencesRepositoryImpl.kt` | User preferences (DataStore) |
| `data/local/dao/NotificationDao.kt` | Notification queries (CRUD, mark read) |
| `data/repository/NotificationRepositoryImpl.kt` | Notification generation from app data |
| `core/notification/NotificationHelper.kt` | System notification posting and channels |
| `presentation/notifications/NotificationScreen.kt` | Notifications UI |
| `presentation/notifications/NotificationViewModel.kt` | Notification state management |
| `presentation/components/NotificationIconWithBadge.kt` | Shared notification icon with unread badge |
| `core/util/CategoryIconUtil.kt` | Category icon mapping and utility |
| `core/util/DateUtil.kt` | Centralized date/time formatting utility |
| `presentation/components/CategoryIcon.kt` | Reusable category icon composable |
| `presentation/components/IconPicker.kt` | Icon picker grid for category editing |
| `core/util/ValidationUtils.kt` | Input validation utilities (amounts, names, emails) |
| `core/util/TextHighlightUtil.kt` | Search result text highlighting utility |
| `presentation/components/TransactionDatePicker.kt` | Date picker for transaction entry |
| `data/worker/DebtReminderWorker.kt` | Weekly Sunday 9 AM debt reminder notifications |
| `data/worker/SubscriptionReminderWorker.kt` | Daily 9:05 AM subscription bill reminders |
| `data/worker/PeriodicSmsSyncWorker.kt` | Every 15 minutes SMS sync (user-toggleable) |
| `presentation/components/PrivacyPolicyContent.kt` | Centralized privacy policy text |
| `presentation/components/PrivacyPolicyDialog.kt` | First-launch privacy acceptance dialog |
| `presentation/components/PrivacyPolicyDeclineDialog.kt` | Decline confirmation dialog |
| `presentation/profile/PrivacyPolicyScreen.kt` | Full privacy policy screen |
| `presentation/components/KeyboardAwareBottomSheet.kt` | Custom Dialog-based bottom sheet with keyboard handling |

## Important Conventions

### Amounts
- All monetary amounts stored as `Long` in **paisa** (1/100 of rupee)
- Example: ₹500.00 → stored as `50000L`
- Use `CurrencyFormatter.formatAsCurrency()` for display

### Database
- Database name: `finance_tracker.db`
- Uses `@Transaction` for atomic wallet balance updates
- Foreign keys with CASCADE (wallet, group) and SET_NULL (person)

### State Management
- ViewModels expose `StateFlow<UiState>`
- Repositories return `Flow<T>` for reactive data
- Use `combine()` to merge multiple flows

### Transaction Types
```kotlin
enum class TransactionType { EXPENSE, INCOME, TRANSFER }
```

### Wallet Types
```kotlin
enum class WalletType { BANK, CASH, CREDIT }
```

## Common Tasks

### Add a new screen
1. Create screen composable in `presentation/<feature>/`
2. Create ViewModel with `@HiltViewModel`
3. Add route to `MainScreen.kt` NavHost
4. Inject repository via constructor

### Add a new entity
1. Create entity in `data/local/entity/`
2. Add to `AppDatabase.kt` entities list
3. Add DAO methods to `FinanceDao.kt`
4. Increment database version
5. Add repository methods

### Modify transaction logic
- Main logic in `FinanceRepositoryImpl.addTransaction()`
- Wallet balance updates are atomic
- Person balance updates for debt tracking

## Testing

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest

# Specific test class
./gradlew test --tests "SmsParserEngineTest"
```

### Test Files

| File | Location | Tests | Coverage |
|------|----------|-------|----------|
| `SmsParserEngineTest.kt` | `src/test/.../core/parser/` | 6 | SMS parsing edge cases |
| `CategoryInferrerTest.kt` | `src/test/.../core/parser/` | 26 | Category inference from merchant names |
| `MerchantNormalizerTest.kt` | `src/test/.../core/parser/` | 13 | Merchant name normalization |
| `SmsParserTest.kt` | `src/test/.../` | 3 | Merchant cleaning, OTP exclusion |
| `ReproductionTest.kt` | `src/test/.../` | 4 | Known parsing issues |
| `ValidationUtilsTest.kt` | `src/test/.../core/util/` | 30 | Amount, name, email, color validation |
| `CsvUtilsTest.kt` | `src/test/.../core/util/` | 35 | CSV escaping, formula injection, type mapping |
| `BudgetThresholdTest.kt` | `src/test/.../domain/` | 30 | 80%/100% budget alerts, category spending |
| `SettlementCalculationTest.kt` | `src/test/.../domain/` | 23 | Greedy settlement algorithm, balance calculation |
| `ErrorStateTest.kt` | `src/test/.../presentation/` | 4 | ViewModel error state handling |

**Total: 180 unit tests**

## Build Commands

```bash
./gradlew assembleDebug      # Debug APK
./gradlew assembleRelease    # Release APK (minified with ProGuard)
./gradlew clean              # Clean build
```

### Release Build

The release build is configured with:
- **ProGuard/R8 minification** enabled (`isMinifyEnabled = true`)
- **Resource shrinking** enabled (`isShrinkResources = true`)
- ProGuard rules in `proguard-rules.pro` for Room entities, Kotlin, Hilt, Compose

## Screens Overview

| Route | Screen | ViewModel |
|-------|--------|-----------|
| `home` | Dashboard | `DashboardViewModel` |
| `ledger` | Transactions | `LedgerViewModel` |
| `wallet` | Accounts | `WalletViewModel` |
| `insights` | Analytics | `InsightsViewModel` |
| `profile` | Settings | `SettingsViewModel` |
| `user` | Edit Profile | `UserProfileViewModel` |
| `categories` | Categories | `CategoryViewModel` |
| `notifications` | Notifications | `NotificationViewModel` |
| `privacyPolicy` | Privacy Policy | None |
| `groupDetail/{groupId}` | Group Detail | `GroupViewModel` |

## Data Flow

```
User Action → ViewModel → Repository → DAO → Database
                 ↑                        ↓
            StateFlow ← Flow<T> ← Flow<Entity>
```

## Notes

- SMS parsing supports Indian bank formats (HDFC, ICICI, SBI, etc.)
- Subscription detection uses 25-35 day interval heuristic
- Budget rollover calculates unused amount from previous month
- Pull-to-refresh on Home screen triggers SMS sync + notification refresh
- Notification and Settings buttons appear on all main screens (Home, Ledger, Wallet, Insights)
- Notification icon shows unread count badge on all main screens
- Dark mode follows system setting by default ("system" theme preference)

## User Profile

### Stored Fields (DataStore)

- `userName` - User's display name
- `userEmail` - Email with validation (must match email pattern)
- `userGender` - Male, Female, Other, Prefer not to say
- `userDateOfBirth` - Format: `dd/MM/yyyy`
- `userDescription` - About me text
- `userImagePath` - Profile photo path (stored in app internal storage)
- `isPrivacyMode` - When enabled, ignores/blocks SMS transaction sync
- `isAmountsHidden` - When enabled, hides all amounts in Home screen UI with ●●●●●●
- `isTripMode` - When enabled, defaults expense category to "Trip" for manual entries and SMS sync
- `lastSyncTimestamp` - Timestamp of last successful SMS sync (persists across app restarts)
- `isNotificationsEnabled` - When disabled, blocks all notification generation and system notifications
- `isPrivacyPolicyAccepted` - Must be true to use app (shown on first launch)
- `displaySize` - Display scaling preference: "extra_small", "small", "medium" (default), "large"

### Features

- Profile photo picker with delete option
- Email validation using `Patterns.EMAIL_ADDRESS`
- Date picker for date of birth
- Auto-save with 500ms debounce on text fields
- Dynamic greeting on Home screen shows user name and profile photo
- Tap profile section on Home screen to edit profile

## Theme

### Theme Preferences

- `"system"` (default) - Follows device dark/light mode setting
- `"dark"` - Force dark mode
- `"light"` - Force light mode

The app automatically uses the device's dark mode setting on first install.

### Display Size Scaling

The app supports user-configurable display scaling via Profile → Preferences → Display Size.

| Size | Scale Factor | Description |
|------|--------------|-------------|
| Extra Small (XS) | 0.75x | Maximum content density |
| Small (S) | 0.85x | Compact displays |
| Medium (M) | 0.92x | Balanced (default) |
| Large (L) | 1.0x | Current/original sizing |

**Key Files:**

| File | Purpose |
|------|---------|
| `presentation/theme/Dimensions.kt` | Scalable spacing system with CompositionLocal |
| `presentation/theme/Type.kt` | `scaledTypography(scale)` function |
| `presentation/theme/Theme.kt` | Applies scale via `FinanceTrackerTheme(displaySize=...)` |

**Usage in Composables:**

```kotlin
@Composable
fun MyScreen() {
    val dims = LocalDimensions.current

    // Use scaled values instead of hardcoded dp
    Modifier.padding(dims.lg)    // 16.dp base, scales with preference
    Modifier.size(dims.iconXl)   // 48.dp base, scales with preference
}
```

**Dimension Names:**

| Name | Base Value | Usage |
|------|------------|-------|
| `xs` | 4.dp | Fine borders, tiny gaps |
| `sm` | 8.dp | Small spacing |
| `md` | 12.dp | Medium spacing |
| `lg` | 16.dp | Standard padding |
| `xl` | 20.dp | Large padding |
| `xxl` | 24.dp | Hero card padding |
| `iconSm` | 20.dp | Small icons |
| `iconMd` | 24.dp | Medium icons |
| `iconLg` | 32.dp | Large icons |
| `iconXl` | 48.dp | Avatar-sized icons |
| `avatarSm` | 36.dp | Small avatars |
| `avatarMd` | 48.dp | Medium avatars |
| `avatarLg` | 64.dp | Large avatars |
| `cardRadius` | 20.dp | Card corner radius |
| `heroRadius` | 24.dp | Hero card radius |
| `fabClearance` | 80.dp | Bottom padding for FAB |

### Elevation & Border Utilities

The app uses centralized elevation and border utilities in `presentation/theme/Elevation.kt`:

| Function | Usage | Light Mode | Dark Mode |
|----------|-------|------------|-----------|
| `heroCardStyling()` | Major cards (Net Worth, Income/Spent) | 2.dp elevation | 0.dp + border |
| `cardStyling()` | Standard cards (wallets, budgets, settings) | 1.dp elevation | 0.dp + border |
| `listItemCardStyling()` | Transaction rows | 0.5.dp elevation | 0.dp + border |
| `isDarkTheme()` | Dark mode detection | false | true |
| `inputBorderColor()` | Text field/button borders | LightGray | DarkSurfaceHighlight |
| `selectionBorder()` | Selected item borders | 2.dp accent | 2.dp accent |
| `accentBorder()` | Alert card borders (Trip banner) | 1.dp color | 1.dp color |

**Usage Example:**

```kotlin
val styling = cardStyling()
Card(
    elevation = CardDefaults.cardElevation(defaultElevation = styling.elevation),
    border = styling.border
) { ... }
```

**Key Files:**

| File | Purpose |
|------|---------|
| `presentation/theme/Elevation.kt` | Elevation and border utilities |
| `presentation/theme/Color.kt` | Color definitions (DarkSurface, DarkSurfaceHighlight) |
| `presentation/theme/Theme.kt` | Color schemes, shapes |

## CSV Export/Import

### Export Format
CSV files are saved to Downloads folder with columns:
`Date,Merchant,Amount,Type,Category,Source`

- Amount is in rupees (not paisa)
- Type: EXPENSE, INCOME, or TRANSFER
- Source: SMS or MANUAL

### Import
- Supports both EXPENSE/INCOME and DEBIT/CREDIT type formats
- Amounts should be in rupees (automatically converted to paisa)
- Date format: `yyyy-MM-dd HH:mm:ss`

## Data Management

### Clear All Data
- Located in Profile → Data Management → Delete All
- Only clears transactions (preserves wallets, budgets, categories, etc.)
- Does NOT prevent SMS re-sync on app restart

## Transaction Sheets

### AddTransactionSheet

Fields: Amount, Category, Note/Merchant, Transaction Date, Payment Type (wallet selection)

- Note/Merchant defaults to "Manual Entry" if left blank
- Categories grouped by EXPENSE/INCOME
- Payment Type shows up to 3 user wallets
- Transaction Date defaults to current date, can be changed via date picker
- Wallet selection passes wallet **name** to ViewModel, which resolves by name match

### EditTransactionSheet

Fields: Amount, Category, Note/Merchant, Date

- SMS transactions: Amount and Date are read-only, category limited to same type
- Manual transactions: All fields are editable including Date
- Delete button available in header

## Bottom Sheet Keyboard Handling

All bottom sheets use `KeyboardAwareModalBottomSheet`, a custom Dialog-based implementation that provides:

1. **Keyboard-aware back press**: First back press hides keyboard, second closes sheet
2. **Keyboard-aware scrim tap**: First tap hides keyboard, second closes sheet
3. **Swipe-to-dismiss**: Swipe down gesture to close sheet
4. **Proper navigation bar padding**: Content stays above system navigation bar

### Why Custom Dialog Instead of ModalBottomSheet

Material3's `ModalBottomSheet` has internal back press handling that cannot be overridden. The custom `KeyboardAwareModalBottomSheet` uses Compose `Dialog` to gain full control over back press behavior.

### Architecture

**Key File**: `presentation/components/KeyboardAwareBottomSheet.kt`

```kotlin
// KeyboardAwareModalBottomSheet captures nav bar height BEFORE Dialog
// because WindowInsets may return 0 inside Dialog's separate window
val density = LocalDensity.current
val navBarHeight = WindowInsets.navigationBars.getBottom(density)

Dialog(
    properties = DialogProperties(
        dismissOnBackPress = false,      // Custom back handling
        dismissOnClickOutside = false,   // Custom scrim handling
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false
    )
) {
    // BackHandler with keyboard-aware logic
    // Surface with swipe-to-dismiss gesture
    Column(
        modifier = Modifier
            .padding(bottom = with(density) { navBarHeight.toDp() })
            .imePadding()
    ) {
        content()
    }
}
```

### Helper Functions

| Function | Purpose |
|----------|---------|
| `rememberKeyboardAwareOnClick(onDismiss)` | Returns click handler that hides keyboard first, then dismisses |
| `KeyboardAwareBackHandler(onDismiss)` | BackHandler that intercepts back when keyboard visible |
| `rememberKeyboardState()` | Debounced keyboard visibility (handles animation race conditions) |
| `isKeyboardVisible()` | Raw keyboard visibility check |

### Content Pattern

```kotlin
@Composable
fun AddSomethingSheet(onDismiss: () -> Unit, onSave: (...) -> Unit) {
    KeyboardAwareModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        AddSomethingContent(onDismiss, onSave)
    }
}

@Composable
fun AddSomethingContent(onDismiss: () -> Unit, onSave: (...) -> Unit) {
    val keyboardAwareOnClick = rememberKeyboardAwareOnClick(onDismiss)
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxWidth()) {
        // FIXED HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(FinosMint)
                .padding(16.dp)
        ) {
            IconButton(onClick = keyboardAwareOnClick) { /* Back icon */ }
            // Title & action buttons
        }

        // SCROLLABLE CONTENT
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            // Form fields...

            // Bottom padding for navigation bar clearance
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
```

### Bottom Sheets Using KeyboardAwareModalBottomSheet

| Sheet                      | Location                        |
| -------------------------- | ------------------------------- |
| `AddTransactionSheet.kt`   | `presentation/add_transaction/` |
| `EditTransactionSheet.kt`  | `presentation/add_transaction/` |
| `AddPersonSheet.kt`        | `presentation/wallet/`          |
| `AddWalletSheet.kt`        | `presentation/wallet/`          |
| `AddBudgetSheet.kt`        | `presentation/insights/`        |
| `AddSubscriptionSheet.kt`  | `presentation/insights/`        |
| `AddGroupSheet.kt`         | `presentation/group/`           |
| `AddGroupExpenseSheet.kt`  | `presentation/group/`           |
| `AddContributionSheet.kt`  | `presentation/group/`           |
| `EditContributionSheet.kt` | `presentation/group/`           |
| `EditGroupExpenseSheet.kt` | `presentation/group/`           |
| `AddMemberSheet.kt`        | `presentation/group/`           |

### Key Points

- **Header stays fixed**: Green header with back button stays at top while content scrolls
- **Navigation bar padding**: Captured BEFORE Dialog (WindowInsets return 0 inside Dialog)
- **`imePadding()`**: Automatically adjusts for keyboard height
- **`verticalScroll(scrollState)`**: Makes form content scrollable
- **Bottom spacer**: Add `Spacer(modifier = Modifier.height(24.dp))` at end of form
- **Keyboard-aware back button**: Use `rememberKeyboardAwareOnClick(onDismiss)` for header buttons

## Notifications

### Notification Toggle

Located in **Profile → Preferences → Notifications**. When disabled:

- No new in-app notifications are generated
- No system notifications appear in Android notification bar
- Weekly debt reminders are skipped
- Existing notifications remain visible (stored data)

Storage key: `notifications_enabled` (default: `true`)

### Notification Types

| Type | Description | Trigger |
| ---- | ----------- | ------- |
| `BUDGET_ALERT` | Budget threshold | When budget reaches 80% or 100% |
| `UPCOMING_BILL` | Bill reminder | Daily at 9:05 AM on days 7, 3, 2, 1, 0 before due, and 1 day overdue |
| `DEBT` | New debt created | One-time when person first has non-zero balance |
| `DEBT_REMINDER` | Weekly summary | Every Sunday at 9:00 AM (if outstanding debts exist) |
| `TRIP` | Event progress | Progress towards event/trip goals |

**Note:** Transaction notifications (INCOME, EXPENSE) are disabled.

### Notification Architecture

- **Entity**: `NotificationEntity` in `data/local/entity/`
- **DAO**: `NotificationDao` in `data/local/dao/`
- **Repository**: `NotificationRepositoryImpl` generates notifications from app data
- **ViewModel**: `NotificationViewModel` manages UI state
- **Helper**: `NotificationHelper` in `core/notification/` handles system notifications
- **Workers**:
  - `DebtReminderWorker` - Weekly debt reminders (Sundays 9:00 AM)
  - `SubscriptionReminderWorker` - Daily bill reminders (9:05 AM)

### Notification Features

- Notifications are auto-generated from real app data
- **Smart deduplication**: Only recreates notification if message content changed
- **Read status preserved**: If data unchanged, existing notification keeps its read status
- Mark as read (single or all)
- Filter by All/Unread tabs
- **Swipe-to-delete**: Swipe left or right to delete a notification
- 30-day auto-cleanup of old notifications
- Relative time formatting (Just now, 2 mins ago, Yesterday, etc.)
- **Notification badge** shows unread count on all main screens (Home, Ledger, Wallet, Insights)
- Badge displays count up to 9, then shows "9+" for larger counts
- Badge uses coral color (`FinosCoral`) for visibility
- **System notifications**: New notifications appear in Android notification bar
- **Deep linking**: Clicking system notification opens app and navigates to relevant screen

### Notification Deduplication Logic

Notifications use `referenceId` + `referenceType` for identity. On regeneration:

1. Compare new message with existing notification's message
2. If same → skip (preserves read status)
3. If different → delete old, create new (unread)

This prevents:

- Duplicate notifications for the same item
- Read notifications reverting to unread when data hasn't changed
- **Mutex protection**: Concurrent calls to `generateNotifications()` are serialized to prevent race conditions

### Notification Triggers

Notifications are generated automatically when:

- **App Launch**: Dashboard generates all pending notifications (budgets, debts, events)
- **Pull-to-Refresh**: Home screen refresh regenerates notifications
- **Budget Added/Updated**: Threshold alerts for 80%/100% usage
- **Daily at 9:05 AM**: Subscription reminders via `SubscriptionReminderWorker`
- **Weekly Sundays 9:00 AM**: Debt summary via `DebtReminderWorker`

### Notification Navigation

- Clicking notification icon on any screen navigates to NotificationScreen
- Clicking a notification navigates to the relevant screen (Ledger, Insights, Wallet)
- Navigation uses `popBackStack` + standard bottom bar navigation pattern for consistency
- Back button from destination returns to the screen before NotificationScreen
- **System notification click**: Opens app, marks notification as read, navigates to source

### System Notifications

- **Permission**: `POST_NOTIFICATIONS` (requested at app launch on Android 13+)
- **Channel**: "Finance Alerts" with default importance
- **Auto-cancel**: Notification dismissed when tapped
- **Deep linking**: Uses intent extras (`notification_id`, `target_route`)

| referenceType | Target Route |
| ------------- | ------------ |
| `TRANSACTION` | `ledger` |
| `BUDGET` | `insights` |
| `SUBSCRIPTION` | `insights` |
| `PERSON` | `wallet` |
| `EVENT` | `ledger` |
| `WEEKLY_REMINDER` | `wallet` |

### Budget Alert Logic

- Uses **current month** spending (not all-time)
- Alerts at **80%** (warning) and **100%** (exceeded)
- Unique per month - allows new alerts each billing cycle
- Supports "All Expenses" budget (excludes Money Transfer category)

### Subscription Reminder Logic

Subscription notifications are scheduled via `SubscriptionReminderWorker` and trigger at **9:05 AM** on specific days:

| Days Until Due | Title | Example Message |
| -------------- | ----- | --------------- |
| 7 days | Upcoming Bill | "Netflix monthly subscription of ₹499.00 is due in 7 days." |
| 3 days | Upcoming Bill | "...is due in 3 days." |
| 2 days | Upcoming Bill | "...is due in 2 days." |
| 1 day | Upcoming Bill | "...is due tomorrow." |
| 0 days | Bill Due Today | "...is due today." |
| -1 day (overdue) | Overdue Bill | "...was due 1 day ago." |

**Key behaviors:**

- Each new reminder **replaces** the previous one (only 1 notification per subscription visible)
- Uses `referenceId = subscriptionId + dueDateId` for deduplication per billing cycle
- Allows re-notification for next billing cycle when `nextDueDate` is updated

### Debt Notification Logic

Debt notifications follow a **one-time + weekly reminder** pattern:

1. **Initial notification**: Created once when a person first has a non-zero balance
2. **Silent updates**: If amount changes, notification is updated but marked as read (no new alert)
3. **Auto-cleanup**: When balance becomes zero, the debt notification is removed
4. **Weekly reminder**: Every Sunday at 9:00 AM, a summary notification is sent if any debts exist

Weekly reminder format: "2 people owe you ₹500. You owe 1 person ₹200."

### Weekly Debt Reminder Worker

**File**: `data/worker/DebtReminderWorker.kt`

- Scheduled via WorkManager in `FinanceApp.onCreate()`
- Runs every **Sunday at 9:00 AM**
- Respects `isNotificationsEnabled` preference
- Creates a `DEBT_REMINDER` notification with summary of all outstanding debts
- Work name: `debt_reminder_weekly`
- Policy: `ExistingPeriodicWorkPolicy.KEEP` (doesn't reschedule if already exists)

### Daily Subscription Reminder Worker

**File**: `data/worker/SubscriptionReminderWorker.kt`

- Scheduled via WorkManager in `FinanceApp.onCreate()`
- Runs **daily at 9:05 AM**
- Respects `isNotificationsEnabled` preference
- Trigger days: 7, 3, 2, 1, 0 days before due date, and 1 day after (overdue)
- Creates `UPCOMING_BILL` notifications via `generateSubscriptionNotifications()`
- Work name: `subscription_reminder_daily`
- Policy: `ExistingPeriodicWorkPolicy.KEEP` (doesn't reschedule if already exists)

## Privacy Mode & Amount Visibility

### Five Separate Features

The app has five independent settings-related features:

| Setting          | Location                         | Purpose                              | Storage Key            |
|------------------|----------------------------------|--------------------------------------|------------------------|
| **Privacy Mode** | Profile → Privacy & Security     | Blocks SMS transaction sync          | `privacy_mode`         |
| **Hide Amounts** | Home → Eye icon in Net Worth card| Hides amounts with ●●●●●●            | `amounts_hidden`       |
| **Trip Mode**    | Profile → Privacy & Security     | Defaults expense category to "Trip"  | `trip_mode`            |
| **Notifications**| Profile → Preferences            | Enables/disables all notifications   | `notifications_enabled`|
| **Auto SMS Sync**| Profile → Preferences            | 15-minute periodic SMS sync          | `periodic_sync_enabled`|

### Privacy Mode (Ignore SMS)

When **Privacy Mode is ON**:

- App launch does NOT auto-sync SMS
- Pull-to-refresh does NOT sync SMS
- Periodic 15-minute sync skips SMS read
- Manual sync shows "Privacy Mode is enabled. SMS sync is disabled."
- Existing transactions remain visible (not deleted)
- `lastSyncTimestamp` is updated to current time (SMS arriving while ON are permanently skipped)

When **Privacy Mode is OFF**:

- SMS sync resumes normally
- `lastSyncTimestamp` is updated to current time (fresh start from this moment)
- SMS that arrived while Privacy Mode was ON are **permanently skipped**

### Hide Amounts (Eye Toggle)

When **Hide Amounts is ON**:

- Net Worth shows `₹ ●●●●●●`
- Income/Expense amounts hidden
- Wallet balances hidden
- Transaction amounts hidden

This setting is independent of Privacy Mode.

### Trip Mode

When **Trip Mode is ON**:

- Manual entries default to "Trip" category for expenses
- SMS synced transactions default to "Trip" category for expenses
- INCOME transactions are NOT affected (still default to "Money Received")
- "Trip" category is a system-provided expense category (auto-created for existing users)

When **Trip Mode is OFF**:

- Manual entries default to "Uncategorized" category for expenses
- SMS synced transactions default to "Uncategorized" category for expenses

## Category Icons

### Dynamic Icon System

Categories display icons dynamically based on their `iconId` field or name-based fallback.

### Dynamic Category Colors

Category colors are fetched from the database and applied consistently across all screens:

- **Transaction lists** (Home, Ledger): Colors from `CategoryEntity.colorHex`
- **Insights charts**: Expense/Income breakdown uses actual category colors
- **Category management**: Edit colors in Categories screen

The `FinanceRepositoryImpl` combines transaction flows with category flows to provide dynamic color lookup via `categoryMap`. When you change a category's color in the Categories screen, it reflects everywhere in the app.

### Category Seeding

System categories are auto-seeded on app launch via `seedDefaultCategories()`:

- Cleans up duplicate categories first (`deleteDuplicateCategories()`)
- Inserts missing system categories with proper colors and icons
- Protected system categories cannot be deleted by users

### Icon Mapping

| iconId | Icon | Category |
|--------|------|----------|
| 0 | Receipt | Default/Uncategorized |
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
| 11-25 | Various | User-selectable icons |

### Key Files

| File | Purpose |
|------|---------|
| `core/util/CategoryIconUtil.kt` | Icon ID to Material Icon mapping |
| `presentation/components/CategoryIcon.kt` | Reusable icon composable |
| `presentation/components/IconPicker.kt` | Grid picker for icon selection |

### Features

- **System categories**: Automatic icons based on category name
- **User categories**: Selectable from 25+ predefined icons via IconPicker
- **Consistent display**: Icons appear in transaction lists, category lists, dropdowns, budget cards
- **Color backgrounds**: Icons render with category color as circular background

### Usage

```kotlin
CategoryIcon(
    categoryName = "Food & Dining",
    iconId = 1,
    colorHex = "#EF4444",
    size = 48.dp,
    useRoundedCorners = true // false for circle
)
```

## Input Validation

### ValidationUtils

**Location**: `core/util/ValidationUtils.kt`

Centralized input validation for consistency across the app:

| Method | Purpose |
|--------|---------|
| `isValidAmount(String)` | Validates currency amount string (e.g., "500.00") |
| `isValidAmountPaisa(Long)` | Validates amount in paisa (1 to 99,99,99,999.99) |
| `isValidCategoryName(String)` | Validates category name (1-50 chars, alphanumeric) |
| `isValidEmail(String)` | Validates email using Android Patterns |
| `sanitizeMerchantName(String)` | Trims and normalizes merchant names |
| `isValidWalletName(String)` | Validates wallet/account names |
| `isValidPersonName(String)` | Validates person names |
| `isValidEventName(String)` | Validates event/goal names |
| `isValidColorHex(String)` | Validates hex color strings (#RGB or #RRGGBB) |

## Date Formatting

### DateUtil

**Location**: `core/util/DateUtil.kt`

Centralized date formatting utility for consistent date/time display across the app.

| Method | Purpose | Example Output |
|--------|---------|----------------|
| `formatDateWithTime(timestamp, context)` | Full date with time (respects 24-hour setting) | "14 Mar 2026, 02:47 PM" or "14 Mar 2026, 14:47" |
| `formatDateOnly(timestamp)` | Date only for grouping headers | "14 Mar 2026" |
| `formatTimestamp(timestamp)` | Legacy method (calls formatDateOnly) | "14 Mar 2026" |

### Date Display Locations

| Screen | Component | Format |
|--------|-----------|--------|
| Home | Recent transactions | Date + Time |
| Ledger | Transaction rows | Date + Time |
| Ledger | Date section headers | Date only |
| Group Detail | Expense rows | Date + Time |
| Group Detail | Contributions | Date + Time |
| Group Detail | Settlement history | Date + Time |
| Group Detail | Filtered transactions (TRIP) | Date + Time |

### 24-Hour Format Support

The app respects the device's time format setting:

- **12-hour format**: "14 Mar 2026, 02:47 PM"
- **24-hour format**: "14 Mar 2026, 14:47"

Detection uses `android.text.format.DateFormat.is24HourFormat(context)`.

### Date Formatting Usage

```kotlin
// In Repository (has Context injected)
val dateStr = DateUtil.formatDateWithTime(transaction.timestamp, context)

// In Composable (use LocalContext)
val context = LocalContext.current
val formattedDate = DateUtil.formatDateWithTime(timestamp, context)

// For date-only (no Context needed)
val dateHeader = DateUtil.formatDateOnly(timestamp)
```

## SMS Sync

### Sync Triggers

| Trigger             | Condition                                  | Start Time          |
|---------------------|--------------------------------------------|---------------------|
| **App Launch**      | SMS permission granted + Privacy Mode OFF  | `lastSyncTimestamp` |
| **Pull-to-Refresh** | Privacy Mode OFF                           | `lastSyncTimestamp` |
| **Manual Sync**     | Privacy Mode OFF                           | User-selected time  |
| **Onboarding**      | User grants permission                     | User-selected time  |
| **Periodic Sync**   | Auto SMS Sync enabled + Privacy Mode OFF   | `lastSyncTimestamp` |

### Periodic SMS Sync

**File**: `data/worker/PeriodicSmsSyncWorker.kt`

- Runs every **15 minutes** via WorkManager
- Scheduled on app startup in `FinanceApp.onCreate()`
- User-toggleable via Profile → Preferences → Auto SMS Sync
- Persists after app kill and device reboot
- Also triggers notification generation after sync

**Behavior:**

- Checks `isPeriodicSyncEnabled` preference first
- Respects Privacy Mode (skips sync when enabled)
- Uses `lastSyncTimestamp` for incremental sync
- Work name: `periodic_sms_sync`
- Policy: `ExistingPeriodicWorkPolicy.KEEP`

**Toggle Control:**

- Storage key: `periodic_sync_enabled` (default: `true`)
- When toggled ON: `PeriodicSmsSyncWorker.schedule(context)`
- When toggled OFF: `PeriodicSmsSyncWorker.cancel(context)`

### Sync Flow

```text
Trigger → Check Privacy Mode → Get lastSyncTimestamp → SmsReader.getTransactions()
    → Parse & Deduplicate → Save to DB → Update lastSyncTimestamp
```

### Key Files

| File                                       | Purpose                                 |
|--------------------------------------------|-----------------------------------------|
| `domain/SmsReader.kt`                      | Reads and parses SMS from inbox         |
| `data/worker/SmsSyncWorker.kt`             | One-time background WorkManager job     |
| `data/worker/PeriodicSmsSyncWorker.kt`     | 15-minute periodic sync worker          |
| `data/repository/FinanceRepositoryImpl.kt` | `syncTransactions()` with deduplication |

### Deduplication

- Each SMS has a unique `smsId` from Android
- Before saving, checks if `smsId` already exists in database
- Prevents duplicate transactions from same SMS

## Automatic Category Inference

### Overview

The app automatically infers transaction categories from merchant names using pattern matching and user preferences. This provides intelligent categorization for SMS-synced transactions while allowing users to override and "teach" the system.

### Architecture

The category inference system has three components:

| Component | File | Purpose |
|-----------|------|---------|
| **CategoryInferrer** | `core/parser/CategoryInferrer.kt` | Rule-based category detection via regex patterns |
| **MerchantNormalizer** | `core/parser/MerchantNormalizer.kt` | Maps merchant variations to canonical names |
| **MerchantCategoryPreference** | `data/local/entity/MerchantCategoryPreferenceEntity.kt` | Stores user corrections for learning |

### Category Inference Flow

```text
SMS Transaction → MerchantNormalizer.normalize(merchant)
    → Check MerchantCategoryPreferenceDao for user override
    → If override exists → Use user's preferred category
    → Else → CategoryInferrer.inferCategory(merchant, isIncome, isTripMode)
        → Pattern match against known merchants
        → If match → Return matched category
        → If no match + Trip Mode → Return "Trip"
        → If no match → Return "Uncategorized"
```

### CategoryInferrer

**Location**: `core/parser/CategoryInferrer.kt`

Pattern-based category detection with these categories:

| Category | Example Merchants |
|----------|------------------|
| Food & Dining | Zomato, Swiggy, Domino's, Starbucks, McDonald's, Cafe |
| Shopping | Amazon, Flipkart, Myntra, Ajio, Nykaa, H&M, Zara |
| Transportation | Uber, Ola, Rapido, Indian Oil, Metro, Petrol |
| Bills & Utilities | Jio, Airtel, Electricity, Gas, Broadband, EMI |
| Entertainment | Netflix, Spotify, Hotstar, YouTube, Cinema, Gaming |
| Health & Wellness | Apollo, Pharmeasy, 1mg, Hospital, Gym, Salon |
| Trip | MakeMyTrip, Goibibo, Airlines, Hotels, Travel |
| Money Transfer | UPI, NEFT, RTGS, Self Transfer |
| Money Received | Salary, Refund, Cashback (for INCOME transactions) |

**Key Methods:**

```kotlin
// Infer category from merchant
CategoryInferrer.inferCategory(
    merchant = "ZOMATO",
    isIncome = false,
    isTripMode = false
) // Returns "Food & Dining"

// Get confidence level (1.0 = matched, 0.5 = defaulted)
CategoryInferrer.getInferenceConfidence("ZOMATO", isIncome = false) // Returns 1.0f

// Get all inferable categories
CategoryInferrer.getInferrableCategories() // Returns Set<String>
```

### MerchantNormalizer

**Location**: `core/parser/MerchantNormalizer.kt`

Maps merchant name variations to canonical forms:

| Normalized | Variations |
|------------|------------|
| Amazon | AMAZON, AMAZON INDIA, AMAZONPAY, AMZN, AWS |
| Zomato | ZOMATO, ZOMATO.COM, BLINKIT, ZOMATO BLINKIT |
| Uber | UBER, UBER INDIA, UBER BV, UBER TRIP |
| Netflix | NETFLIX, NETFLIX.COM, NETFLIX INC |

**Key Methods:**

```kotlin
// Normalize merchant name
MerchantNormalizer.normalize("AMAZON INDIA") // Returns "Amazon"

// Check if merchant is known
MerchantNormalizer.isKnownMerchant("ZOMATO") // Returns true

// Get all known merchants (for autocomplete)
MerchantNormalizer.getAllKnownMerchants() // Returns sorted List<String>
```

### User Category Preferences (Learning)

**Entity**: `MerchantCategoryPreferenceEntity`

When a user manually changes a transaction's category, the app can store this preference:

```kotlin
@Entity(tableName = "merchant_category_preferences")
data class MerchantCategoryPreferenceEntity(
    @PrimaryKey val merchantNormalized: String,  // "Amazon"
    val categoryName: String,                     // "Food & Dining"
    val lastUpdated: Long,                        // Timestamp
    val correctionCount: Int                      // Times user set this
)
```

**DAO Methods:**

| Method | Purpose |
|--------|---------|
| `upsertPreference()` | Insert or update preference |
| `getPreferredCategory(merchant)` | Get user's preferred category |
| `getAllPreferences()` | Get all for batch lookup |
| `deletePreference(merchant)` | Remove a preference |
| `deleteAllPreferences()` | Clear all (for logout) |
| `incrementCorrectionCount()` | Track consistency |

### Priority Order

1. **User Preference** - Stored in `merchant_category_preferences` table (highest priority)
2. **Pattern Match** - `CategoryInferrer` regex patterns
3. **Trip Mode Default** - Returns "Trip" for unknown merchants when enabled
4. **Uncategorized** - Default fallback

### Database Migration

**Version 2 → 3** (`MIGRATION_2_3`):

Creates the `merchant_category_preferences` table for storing user corrections.

## Groups & Split Bills

### Overview

Groups is a unified feature for managing shared expenses, trip budgets, and savings goals with multiple people. It replaces the previous EventEntity.

### Group Types

```kotlin
enum class GroupType {
    GENERAL,        // All features combined (default)
    TRIP,           // Group By Category - budget tracking with expenses
    SPLIT_EXPENSE,  // Expense sharing with settlement suggestions
    SAVINGS_GOAL    // Collaborative target with contributions
}
```

**UI Display Order:** General → Group By Category → Split Bill → Savings

### Split Types

```kotlin
enum class SplitType {
    EQUAL,      // Split equally among all members
    CUSTOM,     // Custom amounts per member
    PERCENTAGE, // Percentage-based split
    SHARES      // Share-based split
}
```

### Key Entities

| Entity | Purpose |
|--------|---------|
| `GroupEntity` | Main group (name, type, budget, target) |
| `GroupMemberEntity` | Junction table linking groups to people |
| `GroupTransactionEntity` | Links transaction to group with split info |
| `GroupSplitEntity` | Individual split amounts per member |
| `GroupContributionEntity` | Savings goal contributions |
| `SettlementEntity` | Settlement records between members |

### Group Repository Methods

```kotlin
interface GroupRepository {
    fun getAllGroups(): Flow<List<GroupUiModel>>
    fun getGroupDetail(groupId: Long): Flow<GroupDetailUiModel?>
    suspend fun createGroup(name: String, type: String, ...)
    suspend fun addGroupExpense(groupId: Long, amount: Long, splits: List<SplitInput>, ...)
    fun calculateSettlements(groupId: Long): Flow<List<SettlementSuggestion>>
    suspend fun recordSettlement(groupId: Long, fromPersonId: Long?, ...)
    suspend fun addContribution(groupId: Long, amount: Long, ...)
}
```

### Current User Representation

The current user is implicitly a member of all their groups. In entities:
- `memberId = null` + `isSelfUser = true` represents the current user
- This allows tracking splits and contributions without a separate user entity

### Navigation

Groups are accessed via the Ledger screen's "Groups" tab. Clicking a group navigates to `groupDetail/{groupId}`.

### Group Transaction Isolation

Group expenses are **isolated from personal transactions**:

- Group expenses do NOT appear in the "All Transactions" tab or Home screen
- Group expenses are ONLY visible within their respective Group Detail screen
- Monthly income/expense totals exclude group transactions
- Category breakdown charts exclude group transactions
- CSV export excludes group transactions

This prevents double-counting and keeps personal finances separate from shared group expenses. Group transactions still affect wallet balances (deducted from the selected wallet when "You" pays).

### Key Files

| File | Purpose |
|------|---------|
| `data/local/entity/GroupEntity.kt` | Group entity with type enum |
| `data/local/entity/GroupTransactionEntity.kt` | Group expense with split type |
| `data/local/entity/GroupSplitEntity.kt` | Per-member split amounts |
| `data/local/entity/SettlementEntity.kt` | Settlement records |
| `data/local/dao/GroupDao.kt` | Group-related queries |
| `domain/repository/GroupRepository.kt` | Repository interface |
| `data/repository/GroupRepositoryImpl.kt` | Settlement calculation logic |
| `presentation/group/GroupViewModel.kt` | UI state management |
| `presentation/group/GroupDetailScreen.kt` | Group detail view |
| `presentation/group/AddGroupSheet.kt` | Create new group bottom sheet |
| `presentation/group/AddGroupExpenseSheet.kt` | Add expense to group bottom sheet |

### Group Detail Tabs

| Group Type | Tabs |
|------------|------|
| General | Expenses, Balances, Settle Up, Categories |
| Group By Category | Expenses, Balances, Settle Up, Categories |
| Split Bill | Expenses, Balances, Settle Up, Categories |
| Savings Goal | Contributions, Balances |

### AddGroupSheet

Fields: Group Type, Group Name, Budget/Target (optional), Color, Members

- **Group Type**: General, Group By Category, Split Bill, Savings (chips with icons, uses FlowRow to wrap)
- **Members**: Shows existing people + "Add New" button to create new person
- **Add New**: Navigates to Wallet screen to add a new person
- Budget field shown for Group By Category/General types
- Target amount field shown for Savings Goal type

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
- **Date**: Formatted date on the right side

### Group Card Display

Group list cards show:

- Group name and member count
- Group type badge (General, Group By Category, Split Bill, Savings)
- **GENERAL groups**: Shows "Net Total" (income - expense)
- **TRIP groups**: Shows "Total expenses" from filtered transactions
- **Other groups**: Shows "Total expenses" from group transactions

Note: The "Settled up" section has been removed from all group cards.

### Categories Tab (Group Detail)

Displays spending breakdown by category with:

- **Donut chart**: Visual representation of spending distribution
- **Category list**: Each category shows:
  - Color indicator with rounded background
  - Category name
  - Progress bar showing percentage
  - Amount and percentage

Category breakdown uses `categoryName` from transactions (not the legacy `tag` field).
Default fallback is "Uncategorized" for expenses without a category.

### Undo Settlement

When a settlement is recorded, a Snackbar appears with an "Undo" action for quick reversal.

**Implementation Files:**

| File | Changes |
|------|---------|
| `domain/repository/GroupRepository.kt` | `recordSettlement()` returns settlement ID |
| `data/repository/GroupRepositoryImpl.kt` | Returns ID from DAO insert |
| `presentation/group/GroupViewModel.kt` | Tracks `lastSettlementId`, `showUndoSnackbar` |
| `presentation/group/GroupDetailScreen.kt` | Shows undo Snackbar via `LaunchedEffect` |

**User Flow:**

1. User records settlement in Settle Up tab
2. Snackbar shows "Settlement recorded" with "Undo" button (~4 second duration)
3. Tap "Undo" → Settlement deleted, balances recalculate via Flow
4. If dismissed → Snackbar state cleared

**Key State:**

```kotlin
// GroupViewModel.kt
private val _lastSettlementId = MutableStateFlow<Long?>(null)
private val _showUndoSnackbar = MutableStateFlow(false)

fun undoLastSettlement() {
    val id = _lastSettlementId.value ?: return
    groupRepository.deleteSettlement(id)
    _lastSettlementId.value = null
    _showUndoSnackbar.value = false
}
```

## Tab State Management

### Overview

Tab state in screens with tabs (Ledger, Insights, GroupDetail, Notifications) is persisted using `SavedStateHandle` in ViewModels. This ensures tab selection survives:

- Configuration changes (rotation)
- Navigation away and back
- Process death

### Implementation Pattern

```kotlin
@HiltViewModel
class LedgerViewModel @Inject constructor(
    ...,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val selectedTab: StateFlow<Int> = savedStateHandle.getStateFlow("ledger_tab", 0)

    fun selectTab(index: Int) {
        savedStateHandle["ledger_tab"] = index
    }

    fun resetTab() {
        savedStateHandle["ledger_tab"] = 0
    }
}
```

### ViewModels with Tab State

| ViewModel | State Key | Tabs |
|-----------|-----------|------|
| `LedgerViewModel` | `ledger_tab` | All Transactions, People, Groups |
| `InsightsViewModel` | `insights_tab` | Overview, Budgets, Subscriptions |
| `GroupViewModel` | `group_detail_tab` | Expenses, Balances, Settle Up, etc. |
| `NotificationViewModel` | `notification_filter` | All, Unread |

### Navigation Behavior

**Back Navigation**: Preserves tab state (returns to same tab)

**Footer Navigation Click**: Resets tab to first tab (index 0)

The `MainScreen.kt` passes ViewModels to screens and calls `resetTab()` when navigating via bottom bar:

```kotlin
onNavigate = { route ->
    when (route) {
        "ledger" -> ledgerViewModel.resetTab()
        "insights" -> insightsViewModel.resetTab()
    }
    navController.navigate(route) { ... }
}
```

### Key Files

| File | Purpose |
|------|---------|
| `presentation/ledger/LedgerViewModel.kt` | Ledger tab state |
| `presentation/insights/InsightsViewModel.kt` | Insights tab state |
| `presentation/group/GroupViewModel.kt` | Group detail tab state |
| `presentation/notifications/NotificationViewModel.kt` | Notification filter state |
| `presentation/main/MainScreen.kt` | Tab reset on footer nav click |

## Ledger Screen Tabs

### Tab Structure

| Tab Index | Tab Name | Content |
|-----------|----------|---------|
| 0 | All Transactions | Transaction list with search & filters |
| 1 | People | Debt tracking with contacts |
| 2 | Groups | Split bills and group expenses |

### People Tab

- Shows summary cards: "They Owe You" / "You Owe Them"
- List of people with balance status
- "Add Person" button at bottom (pill-shaped, outlined)

### Groups Tab

- Shows group cards with type badge, member count, total expenses
- "Settled up" indicator when all balances are zero
- "Add New Group" button at bottom (pill-shaped, outlined - same style as "Add Person")
- Empty state shows message + "Add New Group" button

## Transaction Date Selection

### Overview

All manual transaction entry points support date selection, allowing users to backdate transactions. The date defaults to the current date/time when adding a new transaction.

### Entry Points with Date Selection

| Entry Point | Sheet | Default Date |
|-------------|-------|--------------|
| Add Transaction (FAB) | AddTransactionSheet | Current date |
| Edit Transaction | EditTransactionSheet | Transaction's existing date |
| Add Group Expense | AddGroupExpenseSheet | Current date |
| Add Contribution | AddContributionSheet | Current date |

### Date Selection Behavior

- **Manual transactions**: Date is fully editable via date picker
- **SMS transactions**: Date is read-only (synced from SMS timestamp)
- **Settlements**: Use current timestamp (when settlement is recorded)

### Key Files

| File | Purpose |
|------|---------|
| `presentation/components/TransactionDatePicker.kt` | Reusable date picker component |
| `presentation/add_transaction/AddTransactionSheet.kt` | Add transaction with date selection |
| `presentation/add_transaction/EditTransactionSheet.kt` | Edit transaction with date editing |
| `presentation/group/AddGroupExpenseSheet.kt` | Group expense with date selection |
| `presentation/group/AddContributionSheet.kt` | Contribution with date selection |

### TransactionDatePicker Usage

```kotlin
var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

TransactionDatePicker(
    selectedTimestamp = selectedTimestamp,
    onDateSelected = { selectedTimestamp = it },
    label = "Transaction Date"  // Optional, defaults to "Transaction Date"
)
```

## Search Result Highlighting

### Overview

When searching transactions in the Ledger screen, matching text is highlighted in the search results for better visibility.

### Highlighted Fields

- **Title** (merchant/note) - Primary transaction description
- **Category** - Transaction category name

### Highlight Color

Uses `FinosMint.copy(alpha = 0.4f)` for consistent theming with the app's mint accent color.

### Key Files

| File | Purpose |
|------|---------|
| `core/util/TextHighlightUtil.kt` | Utility for building highlighted AnnotatedString |
| `presentation/home/HomeScreen.kt` | TransactionRowItem with highlighting support |
| `presentation/ledger/LedgerScreen.kt` | Passes search query to TransactionRowItem |

### TextHighlightUtil Usage

```kotlin
import com.example.financetracker.core.util.TextHighlightUtil

Text(
    text = TextHighlightUtil.buildHighlightedText(
        text = "Coffee Shop",
        query = "coff",
        highlightColor = FinosMint.copy(alpha = 0.4f)
    ),
    style = MaterialTheme.typography.titleMedium
)
```

### Behavior

- Case-insensitive matching
- Supports multiple matches in the same text
- Returns plain AnnotatedString when query is blank
- Preserves original text casing while highlighting matched portions

## Privacy Policy

### Overview

The app requires users to accept a Privacy Policy on first launch before accessing any features. The policy emphasizes the "Local-First" architecture where all data stays on the user's device.

### First-Launch Agreement

- **Dialog**: `PrivacyPolicyDialog` shown in `MainActivity` before any other UI
- **Blocking**: Cannot be dismissed by back press or tapping outside
- **Accept**: Saves preference and continues to onboarding/main screen
- **Decline**: Shows `PrivacyPolicyDeclineDialog` with options to go back or exit app

Storage key: `privacy_policy_accepted` (default: `false`)

### Privacy Policy Screen

- **Route**: `privacyPolicy`
- **Access**: Profile → Support → Privacy Policy
- **Content**: Scrollable list of policy sections from `PrivacyPolicyContent`

### Privacy Policy Files

| File | Purpose |
|------|---------|
| `presentation/components/PrivacyPolicyContent.kt` | Centralized policy text (title, sections, contact info) |
| `presentation/components/PrivacyPolicyDialog.kt` | First-launch acceptance dialog |
| `presentation/components/PrivacyPolicyDeclineDialog.kt` | Decline confirmation with exit option |
| `presentation/profile/PrivacyPolicyScreen.kt` | Full scrollable policy screen |

### Policy Content Structure

```kotlin
object PrivacyPolicyContent {
    const val TITLE = "Privacy Policy"
    const val LAST_UPDATED = "Last updated: March 14, 2026"
    const val DEVELOPER_NAME = "Akshay Baiplawat"
    const val APP_NAME = "Finance Tracker"
    const val CONTACT_EMAIL = "support@financetracker.app"
    const val INTRO = "..." // Introduction paragraph
    val SECTIONS = listOf<PolicySection>(...) // 13 policy sections
}
```

### Policy Sections

1. **The "Local-First" Guarantee** - No cloud servers, data stays on device
2. **Permissions We Request and Why** - Overview of on-device processing
3. **A. SMS Reading (READ_SMS)** - Bank alerts only, no personal messages/OTPs
4. **B. Contacts (READ_CONTACTS)** - For tagging debts to friends
5. **C. Camera/Storage** - For receipt photos, stored locally
6. **D. Biometric Authentication** - Uses OS API, no fingerprint access
7. **E. Notifications** - Budget alerts and reminders, no marketing
8. **Data We Actually Collect** - Crash logs only (Firebase Crashlytics)
9. **Backups and Data Portability** - Google Drive backups, CSV export
10. **How We Make Money** - Direct subscription, not data selling
11. **Your Data Rights** - DPDP Act & GDPR compliance
12. **Changes to This Policy** - In-app notification of changes
13. **Contact Us** - Support email

### Dialog Highlights (First-Launch)

The acceptance dialog shows four key points:

- **Local-First Architecture** - Encrypted database on device
- **No Cloud Servers** - We cannot read, share, or sell data
- **Your Data, Your Control** - Export or delete anytime
- **Transparent Business Model** - Customer, not product

### User Flow

```text
First Launch:
App starts → Privacy Policy Dialog
    ├─ Accept → Save preference → Continue to Onboarding/Main
    └─ Decline → Decline Dialog
                    ├─ Go Back → Return to Privacy Dialog
                    └─ Exit App → finish() closes activity

Subsequent Launches:
App starts → Check isPrivacyPolicyAccepted (true) → Skip dialog

Profile Access:
Profile → Support → Privacy Policy → PrivacyPolicyScreen
```

## Help Center

The Help Center provides users with a direct way to contact support via email. Located in Profile → Support → Help Center.

### Email Intent Behavior

When tapped, opens the device's default email app with pre-filled fields:

- **Recipient**: `support@example.com` (configurable in `ProfileScreen.kt`)
- **Subject**: "FinanceTracker Support Request"
- **Body**: Empty (user types their request)

### Implementation

**File**: `presentation/profile/ProfileScreen.kt`

The `openEmailForSupport()` function uses Android's `ACTION_SENDTO` intent with `mailto:` scheme:

```kotlin
private fun openEmailForSupport(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf("support@example.com"))
        putExtra(Intent.EXTRA_SUBJECT, "FinanceTracker Support Request")
    }
    // Opens email app or shows Toast if none available
}
```

### Fallback

If no email app is installed, displays a Toast: "No email app found"

### Customization

To change the support email address, update the `EXTRA_EMAIL` value in the `openEmailForSupport()` function in `ProfileScreen.kt`.

## Log Out

The Log Out feature allows users to reset the app to its initial state, with the option to keep or delete their data.

### Location

Profile → Support → Log Out

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

### Implementation Files

| File | Purpose |
|------|---------|
| `presentation/profile/ProfileScreen.kt` | Log Out button and dialog states |
| `presentation/profile/SettingsViewModel.kt` | `logout(keepData, onComplete)` function |
| `presentation/components/LogoutDialogs.kt` | Confirmation and data choice dialogs |
| `domain/repository/UserPreferencesRepository.kt` | `resetAllPreferences()` interface |
| `data/repository/UserPreferencesRepositoryImpl.kt` | Reset implementation |
| `domain/repository/FinanceRepository.kt` | `clearAllData()` interface |
| `data/repository/FinanceRepositoryImpl.kt` | Database clearing implementation |

### Key Implementation Details

1. **Application Scope**: Logout uses `applicationScope` (not `viewModelScope`) to ensure database operations complete even when activity is destroyed.

2. **App Restart**: Uses `activity.finish()` + `activity.startActivity(intent)` pattern for clean restart.

3. **Workers Cancelled**: Periodic SMS sync, debt reminder, and subscription reminder workers are cancelled on logout.

### Post-Logout Onboarding Behavior

When user completes onboarding after logout:

| Onboarding Choice | Result |
|-------------------|--------|
| **Enable Secure Sync** | SMS sync enabled, Privacy Mode OFF |
| **Skip and Use Manually** | SMS sync disabled, Privacy Mode ON (blocks ALL SMS sync including pull-to-refresh) |
