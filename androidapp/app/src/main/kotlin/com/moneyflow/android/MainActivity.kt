package com.moneyflow.android

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.moneyflow.android.data.DevicePreferences
import com.moneyflow.android.data.KeystoreVault
import com.moneyflow.android.data.NetworkRepository
import com.moneyflow.android.ui.MoneyFlowApp
import com.moneyflow.android.ui.MoneyViewModel

class MoneyFlowApplication : Application() {
    internal val viewModelFactory by lazy {
        viewModelFactory {
            initializer {
                MoneyViewModel(NetworkRepository(), KeystoreVault(this@MoneyFlowApplication), DevicePreferences(this@MoneyFlowApplication))
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    private val model by viewModels<MoneyViewModel> {
        (application as MoneyFlowApplication).viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { model.state.value.restoring }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) receiveShortcut(intent)
        setContent { MoneyFlowApp(model) }
    }

    override fun onResume() {
        super.onResume()
        model.foreground()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveShortcut(intent)
    }

    private fun receiveShortcut(intent: Intent?) {
        if (intent?.action == "com.moneyflow.android.ADD_EXPENSE") model.requestExpenseShortcut()
    }
}
