package com.moneyflow.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moneyflow.android.R

@Composable
internal fun AuthScreen(
    registration: Boolean,
    busy: Boolean,
    error: String?,
    notice: String?,
    switchMode: (Boolean) -> Unit,
    login: (String, String) -> Unit,
    register: (String, String, String, String) -> Unit,
) {
    val name = rememberTextFieldState()
    val fullName = rememberTextFieldState()
    val email = rememberTextFieldState()
    val password = remember(registration) { TextFieldState() }
    var visible by remember(registration) { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = {
        keyboard?.hide()
        val userName = name.text.toString()
        val secret = password.text.toString()
        if (registration) {
            register(fullName.text.toString(), userName, email.text.toString(), secret)
        } else {
            login(userName, secret)
        }
    }

    BackHandler(enabled = registration && !busy) { switchMode(false) }
    Box(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Image(painterResource(R.drawable.ic_moneyflow_badge), contentDescription = null, Modifier.size(64.dp))
            Text(
                if (registration) "Create an account" else "Sign in to MoneyFlow",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
            )
            if (notice != null) Text(notice, color = MaterialTheme.colorScheme.primary)
            if (registration) {
                OutlinedTextField(
                    state = fullName,
                    modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.PersonFullName },
                    label = { Text("Full name") }, lineLimits = TextFieldLineLimits.SingleLine, enabled = !busy,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    state = email,
                    modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.EmailAddress },
                    label = { Text("Email") }, lineLimits = TextFieldLineLimits.SingleLine, enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )
            }
            OutlinedTextField(
                state = name,
                modifier = Modifier.fillMaxWidth().semantics {
                    contentType = if (registration) ContentType.NewUsername else ContentType.Username
                },
                label = { Text("Username") }, lineLimits = TextFieldLineLimits.SingleLine, enabled = !busy,
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
            )
            OutlinedSecureTextField(
                state = password,
                modifier = Modifier.fillMaxWidth().semantics {
                    contentType = if (registration) ContentType.NewPassword else ContentType.Password
                },
                label = { Text("Password") }, enabled = !busy,
                textObfuscationMode = if (visible) TextObfuscationMode.Visible else TextObfuscationMode.Hidden,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                onKeyboardAction = { if (!busy) submit() },
                trailingIcon = {
                    TextButton(
                        onClick = { visible = !visible },
                        modifier = Modifier.semantics {
                            contentDescription = if (visible) "Hide password" else "Show password"
                        },
                    ) { Text(if (visible) "Hide" else "Show") }
                },
            )
            if (error != null) ErrorText(error)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Button(
                onClick = submit,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(if (registration) "Create account" else "Sign in")
            }
            TextButton(onClick = { switchMode(!registration) }, enabled = !busy) {
                Text(if (registration) "Back to sign in" else "Create an account")
            }
        }
    }
}
