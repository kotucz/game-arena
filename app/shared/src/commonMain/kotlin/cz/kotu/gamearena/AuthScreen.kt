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
import gamearena.app.shared.generated.resources.Res
import gamearena.app.shared.generated.resources.arena_already_registered
import gamearena.app.shared.generated.resources.arena_cancel
import gamearena.app.shared.generated.resources.arena_choose_username
import gamearena.app.shared.generated.resources.arena_complete_profile
import gamearena.app.shared.generated.resources.arena_continue_google
import gamearena.app.shared.generated.resources.arena_create_account
import gamearena.app.shared.generated.resources.arena_email
import gamearena.app.shared.generated.resources.arena_finish_setup
import gamearena.app.shared.generated.resources.arena_log_in
import gamearena.app.shared.generated.resources.arena_need_account
import gamearena.app.shared.generated.resources.arena_or_email_password
import gamearena.app.shared.generated.resources.arena_password
import gamearena.app.shared.generated.resources.arena_register
import gamearena.app.shared.generated.resources.arena_setting_up
import gamearena.app.shared.generated.resources.arena_signing_in_google
import gamearena.app.shared.generated.resources.arena_username
import gamearena.app.shared.generated.resources.arena_welcome
import org.jetbrains.compose.resources.stringResource

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit,
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
            Text(stringResource(Res.string.arena_choose_username))
            Text(stringResource(Res.string.arena_complete_profile))
            OutlinedTextField(
                value = chosenUsername,
                onValueChange = viewModel::updateChosenUsername,
                label = { Text(stringResource(Res.string.arena_username)) },
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
                Text(
                    stringResource(
                        if (submitting) Res.string.arena_setting_up else Res.string.arena_finish_setup,
                    ),
                )
            }
            message?.let { Text(it) }
            TextButton(
                onClick = { viewModel.cancelOnboarding() },
            ) {
                Text(stringResource(Res.string.arena_cancel))
            }
        } else {
            Text(stringResource(if (mode == AuthMode.Login) Res.string.arena_welcome else Res.string.arena_create_account))
            Button(
                enabled = submitEnabled,
                modifier = Modifier.fillMaxWidth(),
                onClick = { googleAuth.launch() },
            ) {
                Text(
                    stringResource(
                        if (googleAuth.isInProgress) Res.string.arena_signing_in_google
                        else Res.string.arena_continue_google,
                    ),
                )
            }
            Text(stringResource(Res.string.arena_or_email_password))
            OutlinedTextField(
                value = email,
                onValueChange = viewModel::updateEmail,
                label = { Text(stringResource(Res.string.arena_email)) },
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
                label = { Text(stringResource(Res.string.arena_password)) },
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
                Text(stringResource(if (mode == AuthMode.Login) Res.string.arena_log_in else Res.string.arena_register))
            }
            message?.let { Text(it) }
            TextButton(
                onClick = { viewModel.toggleMode() },
            ) {
                Text(
                    stringResource(
                        if (mode == AuthMode.Login) Res.string.arena_need_account
                        else Res.string.arena_already_registered,
                    ),
                )
            }
        }
    }
}
