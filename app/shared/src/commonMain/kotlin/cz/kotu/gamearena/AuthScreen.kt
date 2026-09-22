package cz.kotu.gamearena

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mmk.kmpauth.core.KMPAuth
import com.mmk.kmpauth.core.auth.EmailAuthMode
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.core.auth.rememberEmailAuthState
import com.mmk.kmpauth.google.rememberGoogleAuthState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch

private enum class AuthMode { Login, Register }

@Composable
fun AuthScreen(
    authManager: AuthManager,
    onAuthenticated: () -> Unit,
    onGoogleSignIn: () -> Unit = {},
) {
    var mode by remember { mutableStateOf(AuthMode.Login) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    fun onAuthResult(result: Result<KMPAuthUser>) {
        result.fold(
            onSuccess = { kmpAuthUser ->
                scope.launch {
                    val tokenResult = KMPAuth.currentUserIdToken()
                    tokenResult.fold(
                        onSuccess = { firebaseIdToken ->
                            val firebaseResult = authManager.loginWithFirebase(
                                idToken = firebaseIdToken,
                                username = username.ifBlank { kmpAuthUser.displayName ?: "google-user" },
                                email = kmpAuthUser.email ?: email.ifBlank { null },
                            )
                            firebaseResult.fold(
                                onSuccess = { onAuthenticated() },
                                onFailure = {
                                    Napier.w(it) { "Google sign-in failed 1" }
                                    message = it.message ?: "Google sign-in failed"
                                },
                            )
                        },
                        onFailure = {
                            Napier.w(it) { "Failed to retrieve Firebase ID token" }
                            message = it.message ?: "Failed to retrieve Firebase ID token"
                        },
                    )
                }
            },
            onFailure = {
                Napier.w(it) { "Google sign-in failed 2" }
                message = it.message ?: "Google sign-in failed"
            },
        )
    }

    val emailAuth = rememberEmailAuthState(
        email = email,
        password = password,
        mode = when (mode) {
            AuthMode.Login -> EmailAuthMode.SignIn
            AuthMode.Register -> EmailAuthMode.SignUp
        },
        onResult = ::onAuthResult,
    )
    val googleAuth = rememberGoogleAuthState(onResult = ::onAuthResult)
    val submitEnabled by derivedStateOf { !submitting && !emailAuth.isInProgress && !googleAuth.isInProgress }

    val submit = {
        if (submitEnabled) {
            submitting = true
            message = null
            scope.launch {
                val result = if (mode == AuthMode.Login) {
                    authManager.login(username, password)
                } else {
                    authManager.register(username, email, password)
                }
                submitting = false
                result.fold(
                    onSuccess = {
                        onAuthenticated()
                    },
                    onFailure = { message = it.message ?: "Request failed" },
                )
            }
        }
    }

    Column(
        modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(if (mode == AuthMode.Login) "Welcome to Game Arena" else "Create your account")
        Button(
            enabled = submitEnabled,
            modifier = Modifier.fillMaxWidth(),
            onClick = { googleAuth.launch() },
        ) {
            Text(if (googleAuth.isInProgress) "Signing in with Google..." else "Continue with Google")
        }
        Text("or use your local account")
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
            enabled = submitEnabled,
            modifier = Modifier.fillMaxWidth(),
            onClick = emailAuth::launch,
        ) {
            Text(if (mode == AuthMode.Login) "Log in Firebase" else "Register Firebase")
        }
        Button(
            enabled = submitEnabled,
            modifier = Modifier.fillMaxWidth(),
            onClick = submit,
        ) {
            Text(if (mode == AuthMode.Login) "Old Log in" else "Old Register")
        }
        message?.let { Text(it) }
        TextButton(
            onClick = {
                mode = if (mode == AuthMode.Login) AuthMode.Register else AuthMode.Login
                message = null
            },
        ) {
            Text(if (mode == AuthMode.Login) "Need an account? Register" else "Already registered? Log in")
        }
    }
}
