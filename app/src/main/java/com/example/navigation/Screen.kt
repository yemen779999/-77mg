package com.example.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Accounts : Screen("accounts")
    object Transactions : Screen("transactions")
    object AiAdvisor : Screen("ai_advisor")
    object Settings : Screen("settings")
    object Trash : Screen("trash")
    object CloudSync : Screen("cloud_sync")
    object InvoiceDesigner : Screen("invoice_designer")
    object ClientPortal : Screen("client_portal")
    object AccountDetail : Screen("account_detail/{accountId}") {
        fun createRoute(accountId: Int) = "account_detail/$accountId"
    }
}
