package com.moneyflow.android.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moneyflow.android.data.Settings
import com.moneyflow.android.domain.Dashboard
import com.moneyflow.android.domain.Entry
import com.moneyflow.android.domain.Kind
import java.math.BigDecimal
import java.time.LocalDate

@Preview(name = "Dashboard and ledger", widthDp = 393, showBackground = true)
@Preview(
    name = "Dashboard and ledger, dark",
    widthDp = 393,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun WorkspacePreview() {
    val demo = Dashboard(BigDecimal("57000.00"), BigDecimal("12784.65"), emptyList(), emptyList(), emptyList(), emptyList())
    val expense = Entry(
        Kind.Expense, 1, BigDecimal("380.50"), LocalDate.of(2026, 11, 24),
        1, "Groceries", "Market", "upi", "Weekly shop",
    )
    MoneyTheme(Settings(dynamicColor = false)) {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CashFlowSummary(demo)
                SectionHeading("Recent transactions")
                EntryRow(expense, showDate = true, onClick = {})
            }
        }
    }
}

@Preview(name = "Sign in", widthDp = 393, heightDp = 720, showBackground = true)
@Composable
private fun SignInPreview() {
    MoneyTheme(Settings(dynamicColor = false)) {
        Surface {
            AuthScreen(
                registration = false,
                busy = false,
                error = null,
                notice = null,
                switchMode = {},
                login = { _, _ -> },
                register = { _, _, _, _ -> },
            )
        }
    }
}
