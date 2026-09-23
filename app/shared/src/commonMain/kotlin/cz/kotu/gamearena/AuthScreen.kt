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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mmk.kmpauth.core.auth.EmailAuthMode
import com.mmk.kmpauth.core.auth.rememberEmailAuthState
import com.mmk.kmpauth.google.rememberGoogleAuthState

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit,
    onGoogleSignIn: () -> Unit = {},
) {
    val mode by viewModel.mode.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val onboardingUser by viewModel.onboardingUser.collectAsState()
    val chosenUsername by viewModel.chosenUsername.collectAsState()
    val message by viewModel.message.collectAsState()
    val submitting by viewModel.submitting.collectAsState()
    val focusManager = LocalFocusManager.current

    val emailAuth = rememberEmailAuthState(
        email = email,
        password = password,
        mode = when (mode) {
            AuthMode.Login -> EmailAuthMode.SignIn
            AuthMode.Register -> EmailAuthMode.SignUp
        },
        onResult = { result -> viewModel.handleAuthResult(result, onAuthenticated) },
    )
    val googleAuth = rememberGoogleAuthState(
        onResult = { result -> viewModel.handleAuthResult(result, onAuthenticated) },
    )
    val submitEnabled by derivedStateOf { !submitting && !emailAuth.isInProgress && !googleAuth.isInProgress }

    Column(
        modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onboardingUser != null) {
            Text("Choose your username")
            Text("Complete your profile by choosing a username for Game Arena.")
            OutlinedTextField(
                value = chosenUsername,
                onValueChange = viewModel::updateChosenUsername,
                label = { Text("Username") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (submitEnabled) {
                        viewModel.completeOnboarding(onAuthenticated)
                    }
                }),
                modifier = Modifier.fillMaxWidth().semantics {
                    contentType = ContentType.Username
                },
            )
            Button(
                enabled = submitEnabled,
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.completeOnboarding(onAuthenticated) },
            ) {
                Text(if (submitting) "Setting up..." else "Finish Setup")
            }
            message?.let { Text(it) }
            TextButton(
                onClick = { viewModel.cancelOnboarding() },
            ) {
                Text("Cancel")
            }
        } else {
            Text(if (mode == AuthMode.Login) "Welcome to Game Arena" else "Create your account")
            Button(
                enabled = submitEnabled,
                modifier = Modifier.fillMaxWidth(),
                onClick = { googleAuth.launch() },
            ) {
                Text(if (googleAuth.isInProgress) "Signing in with Google..." else "Continue with Google")
            }
            Text("or with email and password")
            OutlinedTextField(
                value = email,
                onValueChange = viewModel::updateEmail,
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                modifier = Modifier.fillMaxWidth().semantics {
                    contentType = ContentType.EmailAddress
                },
            )
            OutlinedTextField(
                value = password,
                onValueChange = viewModel::updatePassword,
                label = { Text("Password") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (submitEnabled) {
                        emailAuth.launch()
                    }
                }),
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
                Text(if (mode == AuthMode.Login) "Log in" else "Register")
            }
            message?.let { Text(it) }
            TextButton(
                onClick = { viewModel.toggleMode() },
            ) {
                Text(if (mode == AuthMode.Login) "Need an account? Register" else "Already registered? Log in")
            }
        }
    }
}
