package com.example.financetracker.presentation.components

/**
 * Centralized privacy policy content used by both the first-launch dialog
 * and the full Privacy Policy screen.
 */
object PrivacyPolicyContent {
    const val TITLE = "Privacy Policy"
    const val LAST_UPDATED = "Last updated: March 14, 2026"
    const val DEVELOPER_NAME = "Akshay Baiplawat"
    const val APP_NAME = "Finance Tracker"
    const val CONTACT_EMAIL = "support@financetracker.app"

    const val INTRO = "At $DEVELOPER_NAME, we believe your financial data is exactly that—yours.\n\nWe built $APP_NAME as a \"Local-First\" financial operating system. This means our primary business model is providing you with an excellent tool, not harvesting your data to sell you credit cards or loans.\n\nThis Privacy Policy explains how we handle your data in plain English, specifically addressing real-world scenarios regarding how our automated tracking works."

    val SECTIONS = listOf(
        PolicySection(
            title = "1. The \"Local-First\" Guarantee (No Cloud Servers)",
            content = """
                Before we list the permissions we request, it is critical to understand our architecture: We do not have access to your financial data.

                • Every transaction, budget, and balance you track is stored locally in an encrypted database on your device.
                • We do not maintain cloud servers that hold your personal spending history.
                • Because we do not have your data, we cannot read it, share it, or sell it.
            """.trimIndent()
        ),
        PolicySection(
            title = "2. Permissions We Request and Why",
            content = """
                To automate your expense tracking and provide a seamless experience, the app requires certain on-device permissions. All processing for these features happens directly on your phone's processor.
            """.trimIndent()
        ),
        PolicySection(
            title = "A. SMS Reading Permission (READ_SMS)",
            content = """
                Real-World Scenario: You swipe your card, and your bank sends an SMS alert. You want the app to log this automatically.

                How We Use It: The app reads incoming text messages strictly to identify financial transactions (e.g., keywords like "debited," "credited," "UPI") from recognized bank sender IDs (e.g., HDFCBK, SBININ).

                What We DON'T Do: We do not read personal messages from your friends, we do not read OTPs (One Time Passwords), and we never transmit any of your SMS data to our servers or any third party.
            """.trimIndent()
        ),
        PolicySection(
            title = "B. Contacts Permission (READ_CONTACTS)",
            content = """
                Real-World Scenario: You lend ₹500 to your friend Rahul and want to track it in the "Debts" tab.

                How We Use It: We request contact access solely so you can quickly tag a transaction to a friend's name and avatar.

                What We DON'T Do: We never upload your address book to the cloud or use your contacts for marketing purposes.
            """.trimIndent()
        ),
        PolicySection(
            title = "C. Camera/Storage Permission",
            content = """
                Real-World Scenario: You want to snap a picture of a restaurant receipt and attach it to a manual expense entry.

                How We Use It: We use the camera to take the photo and save it locally within the app's secure storage directory.

                What We DON'T Do: We do not upload your photos to external servers or scan them for non-financial data.
            """.trimIndent()
        ),
        PolicySection(
            title = "D. Biometric Authentication",
            content = """
                Real-World Scenario: You want to lock the app with your fingerprint so friends can't see your net worth if they borrow your phone.

                How We Use It: We use the standard Android Biometric API. The app simply asks your phone's operating system, "Did the user provide the correct fingerprint?"

                What We DON'T Do: We do not have access to your actual fingerprint or facial recognition data.
            """.trimIndent()
        ),
        PolicySection(
            title = "E. Notification Permission (POST_NOTIFICATIONS)",
            content = """
                Real-World Scenario: You want to know when you've hit 80% of your "Dining Out" budget, or you need a reminder that your ₹649 Netflix subscription is renewing tomorrow.

                How We Use It: We ask for permission to send you timely, local alerts about your financial guardrails, upcoming bills, and debt reminders. Because our app is "Local-First," these notifications are calculated and scheduled entirely by your phone's processor.

                What We DON'T Do: We do not send you marketing spam, third-party advertisements, or "pre-approved loan" offers. Furthermore, because we don't use cloud servers to generate these alerts, we aren't tracking your interaction with these notifications on a remote database.
            """.trimIndent()
        ),
        PolicySection(
            title = "3. Data We Actually Collect (Telemetry & Analytics)",
            content = """
                While your financial data stays on your device, we do collect minimal, anonymous data to ensure the app doesn't crash and to improve our services.

                Crash Logs & Performance: If the app crashes, an automated report is generated (via tools like Google Firebase Crashlytics) detailing the line of code that failed and your phone model (e.g., "Pixel 8 Pro"). This contains no personal or financial information.

                Purchase History: If you buy our "Pro" subscription, the transaction is processed securely through the Google Play Store. We receive a token confirming your purchase, but we do not receive your credit card details.
            """.trimIndent()
        ),
        PolicySection(
            title = "4. Backups and Data Portability",
            content = """
                Because we do not store your data on our servers, losing your phone means losing your data—unless you back it up.

                Google Drive Backups: You can choose to enable automated backups to your own personal Google Drive account. We facilitate this connection, but we do not have administrative access to your cloud drive.

                CSV Export: You have the absolute right to export your data at any time via the "Export to CSV" feature in Profile > Data Management.
            """.trimIndent()
        ),
        PolicySection(
            title = "5. How We Make Money (Alignment of Interests)",
            content = """
                Many "free" finance apps make money by analyzing your spending habits and selling that profile to lending institutions. We do not.

                Our business model is straightforward and aligned with your privacy: We offer a robust free version of the app, and we charge a direct subscription fee for advanced "Pro" features.

                You are our customer, not our product.
            """.trimIndent()
        ),
        PolicySection(
            title = "6. Your Data Rights (DPDP Act & GDPR Compliance)",
            content = """
                Depending on your region (such as under the Digital Personal Data Protection Act in India or the GDPR in Europe), you have specific rights. Because of our Local-First design, exercising these rights is entirely in your control:

                The Right to Erasure ("Right to be Forgotten"): You can completely destroy all your financial data at any time by navigating to Profile > Data Management > Delete All Data within the app, or simply by uninstalling the application.

                The Right to Access: You can view all processed data directly within your app's ledger.

                Consent Revocation: You can revoke SMS, Contact, or Notification permissions at any time via your phone's OS Settings. (Note: This will disable the automated features of the app).
            """.trimIndent()
        ),
        PolicySection(
            title = "7. Changes to This Policy",
            content = """
                We may update this Privacy Policy as we add new features (for example, if we introduce Account Aggregator API integrations in the future).

                We will notify you of any significant changes via an in-app banner before they take effect.
            """.trimIndent()
        ),
        PolicySection(
            title = "8. Contact Us",
            content = """
                If you have any questions, concerns, or need technical support regarding your privacy, please reach out to us directly:

                Email: $CONTACT_EMAIL
            """.trimIndent()
        )
    )
}

data class PolicySection(
    val title: String,
    val content: String
)
