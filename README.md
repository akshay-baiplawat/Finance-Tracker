# Finance Tracker

A modern Android personal finance management app built with Kotlin and Jetpack Compose. Track expenses, income, budgets, subscriptions, and debts with automatic SMS transaction parsing.

## Features

- **Automatic SMS Parsing** - Automatically imports transactions from bank SMS messages
- **Multi-Wallet Support** - Manage multiple accounts (Bank, Cash, Credit Cards)
- **Budget Management** - Set category budgets with rollover support
- **Subscription Detection** - AI-powered detection of recurring payments
- **Debt Tracking** - Track money owed to/from people
- **Groups & Split Bills** - Manage shared expenses with settlement suggestions
- **Smart Notifications** - Budget alerts, bill reminders, debt summaries
- **CSV Import/Export** - Backup and restore your data
- **Multi-Currency** - Support for multiple currencies (default: INR)
- **Privacy Mode** - Hide sensitive amounts with a single tap
- **Trip Mode** - Auto-categorize expenses during trips
- **Display Scaling** - Adjustable UI size (XS/S/M/L)

## Tech Stack

| Technology | Purpose |
|------------|---------|
| **Kotlin** | Primary language |
| **Jetpack Compose** | Modern declarative UI |
| **Room** | SQLite database with ORM |
| **Hilt** | Dependency injection |
| **WorkManager** | Background SMS sync & notifications |
| **Kotlin Flow** | Reactive data streams |
| **Material 3** | Design system |
| **DataStore** | User preferences storage |

## Architecture

The app follows **MVVM + Clean Architecture** principles:

```
┌─────────────────────────────────────────────────────────┐
│  PRESENTATION (UI + ViewModels)                         │
│  Jetpack Compose screens + StateFlow state management   │
├─────────────────────────────────────────────────────────┤
│  DOMAIN (Business Logic)                                │
│  Repository interfaces, SMS parsing, subscription logic │
├─────────────────────────────────────────────────────────┤
│  DATA (Persistence)                                     │
│  Room DB, DAOs, Repository implementations              │
└─────────────────────────────────────────────────────────┘
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for detailed architecture documentation.

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- Kotlin 1.9+

## Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd FinanceTracker
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory

3. **Sync Gradle**
   - Android Studio will automatically sync Gradle
   - If not, click "Sync Project with Gradle Files"

4. **Run the app**
   ```bash
   ./gradlew installDebug
   ```
   Or use the Run button in Android Studio

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build (ProGuard minified)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

## Project Structure

```
app/src/main/java/com/example/financetracker/
├── FinanceApp.kt                 # Application class
├── MainActivity.kt               # Single activity entry point
├── di/                           # Dependency injection modules
├── presentation/                 # UI layer (Screens + ViewModels)
│   ├── home/                     # Dashboard screen
│   ├── ledger/                   # Transaction list
│   ├── wallet/                   # Wallet management
│   ├── insights/                 # Budgets & analytics
│   ├── profile/                  # Settings
│   ├── group/                    # Groups & split bills
│   ├── notifications/            # In-app notifications
│   └── components/               # Reusable UI components
├── domain/                       # Business logic layer
│   ├── repository/               # Repository interfaces
│   ├── logic/                    # Business logic (subscriptions)
│   └── model/                    # Domain models
├── data/                         # Data layer
│   ├── local/                    # Room database
│   │   ├── entity/               # Database entities
│   │   ├── dao/                  # Data access objects
│   │   └── mapper/               # Entity to domain mappers
│   ├── repository/               # Repository implementations
│   └── worker/                   # Background workers
└── core/                         # Utilities
    ├── util/                     # Currency, date, validation utilities
    ├── parser/                   # SMS parsing engine
    └── notification/             # System notification helpers
```

## Documentation

| Document | Description |
|----------|-------------|
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Architecture patterns and data flow |
| [FEATURES.md](docs/FEATURES.md) | Detailed feature documentation |
| [DATABASE.md](docs/DATABASE.md) | Database schema and entities |
| [SCREENS.md](docs/SCREENS.md) | UI screens and navigation |

## Permissions

The app requires the following permissions:

- `READ_SMS` - For automatic transaction import from bank SMS
- `RECEIVE_SMS` - For real-time SMS notifications
- `POST_NOTIFICATIONS` - For budget alerts and reminders
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - For reliable background sync

## Testing

The app includes 141+ unit tests covering:

- SMS parsing edge cases
- Input validation (amounts, names, emails, colors)
- CSV export/import utilities
- Budget threshold alerts
- Settlement calculations
- Error state handling

Run tests with:
```bash
./gradlew test
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
