package cz.kotu.game.gotfive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private enum class AuthMode { Login, Register }

@Composable
fun AuthScreen(onAuthenticated: () -> Unit) {
    var mode by remember { mutableStateOf(AuthMode.Login) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val authClient = remember { AuthClient() }

    LaunchedEffect(Unit) {
        authClient.currentUser().onSuccess { onAuthenticated() }
    }

    Column(
        modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(if (mode == AuthMode.Login) "Welcome to GotFive" else "Create your account")
        OutlinedTextField(username, { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        if (mode == AuthMode.Register) {
            OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        }
        OutlinedTextField(
            password, { password = it }, label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
        )
        Button(
            enabled = !submitting,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                submitting = true
                message = null
                scope.launch {
                    val result = if (mode == AuthMode.Login) {
                        authClient.login(username, password)
                    } else {
                        authClient.register(username, email, password)
                    }
                    submitting = false
                    result.fold({ onAuthenticated() }, { message = it.message ?: "Request failed" })
                }
            },
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
