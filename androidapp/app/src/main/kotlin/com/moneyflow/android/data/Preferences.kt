package com.moneyflow.android.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal enum class Appearance(val label: String) { System("System"), Light("Light"), Dark("Dark") }

internal data class Settings(
    val appearance: Appearance = Appearance.System,
    val dynamicColor: Boolean = true,
    val lastPayment: String? = null,
)

internal interface Preferences {
    val settings: Flow<Settings>
    suspend fun appearance(value: Appearance)
    suspend fun dynamicColor(enabled: Boolean)
    suspend fun lastPayment(value: String?)
}

private val Context.moneyFlowPreferences by preferencesDataStore("moneyflow_settings")

internal class DevicePreferences(context: Context) : Preferences {
    private val store = context.moneyFlowPreferences
    private val theme = stringPreferencesKey("appearance")
    private val dynamic = booleanPreferencesKey("dynamic_color")
    private val payment = stringPreferencesKey("last_payment")

    override val settings: Flow<Settings> = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { values ->
        Settings(
            Appearance.entries.firstOrNull { it.name == values[theme] } ?: Appearance.System,
            values[dynamic] ?: true,
            values[payment],
        )
    }

    override suspend fun appearance(value: Appearance) {
        store.edit { it[theme] = value.name }
    }

    override suspend fun dynamicColor(enabled: Boolean) {
        store.edit { it[dynamic] = enabled }
    }

    override suspend fun lastPayment(value: String?) {
        store.edit { values ->
            if (value == null) values.remove(payment) else values[payment] = value
        }
    }
}
