package cz.kotu.gamearena

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private enum class AuthMode { Login, Register }

@Composable
fun AuthScreen(
    authClient: AuthClient,
    onAuthenticated: (String) -> Unit,
) {
    var mode by remember { mutableStateOf(AuthMode.Login) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val submit = {
        if (!submitting) {
            submitting = true
            message = null
            scope.launch {
                val result = if (mode == AuthMode.Login) {
                    authClient.login(username, password)
                } else {
                    authClient.register(username, email, password)
                }
                submitting = false
                result.fold({ onAuthenticated(username.trim()) }, { message = it.message ?: "Request failed" })
            }
        }
    }

    Column(
        modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(if (mode == AuthMode.Login) "Welcome to Game Arena" else "Create your account")
        OutlinedTextField(
            username, { username = it }, label = { Text("Username") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
            modifier = Modifier.fillMaxWidth().semantics {
                contentType = if (mode == AuthMode.Register) ContentType.NewUsername else ContentType.Username
            },
        )
        if (mode == AuthMode.Register) {
            OutlinedTextField(
                email, { email = it }, label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                modifier = Modifier.fillMaxWidth().semantics {
                    contentType = ContentType.EmailAddress
                },
            )
        }
        OutlinedTextField(
            password, { password = it }, label = { Text("Password") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().semantics {
                contentType = if (mode == AuthMode.Register) ContentType.NewPassword else ContentType.Password
            },
        )
        Button(
            enabled = !submitting,
            modifier = Modifier.fillMaxWidth(),
            onClick = submit,
        ) {
            Text(if (mode == AuthMode.Login) "Log in" else "Register")
        }
        message?.let { Text(it) }
        TextButton(onClick = {
            mode = if (mode == AuthMode.Login) AuthMode.Register else AuthMode.Login
            message = null
        }) {
            Text(if (mode == AuthMode.Login) "Need an account? Register" else "Already registered? Log in")
        }
    }
}
