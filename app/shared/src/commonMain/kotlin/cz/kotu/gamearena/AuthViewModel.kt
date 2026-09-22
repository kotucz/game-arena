package cz.kotu.gamearena

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmk.kmpauth.core.KMPAuth
import com.mmk.kmpauth.core.auth.KMPAuthUser
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

enum class AuthMode { Login, Register }

@Inject
class AuthViewModel(
    private val authManager: AuthManager,
) : ViewModel() {

    private val _mode = MutableStateFlow(AuthMode.Login)
    val mode: StateFlow<AuthMode> = _mode.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    fun updateMode(mode: AuthMode) {
        _mode.value = mode
        _message.value = null
    }

    fun toggleMode() {
        _mode.value = if (_mode.value == AuthMode.Login) AuthMode.Register else AuthMode.Login
        _message.value = null
    }

    fun updateUsername(value: String) {
        _username.value = value
    }

    fun updateEmail(value: String) {
        _email.value = value
    }

    fun updatePassword(value: String) {
        _password.value = value
    }

    fun clearMessage() {
        _message.value = null
    }

    fun submit(onAuthenticated: () -> Unit) {
        if (_submitting.value) return
        _submitting.value = true
        _message.value = null
        viewModelScope.launch {
            val result = if (_mode.value == AuthMode.Login) {
                authManager.login(_username.value, _password.value)
            } else {
                authManager.register(_username.value, _email.value, _password.value)
            }
            _submitting.value = false
            result.fold(
                onSuccess = { onAuthenticated() },
                onFailure = { _message.value = it.message ?: "Request failed" },
            )
        }
    }

    fun handleAuthResult(result: Result<KMPAuthUser>, onAuthenticated: () -> Unit) {
        result.fold(
            onSuccess = { kmpAuthUser ->
                viewModelScope.launch {
                    val tokenResult = KMPAuth.currentUserIdToken()
                    tokenResult.fold(
                        onSuccess = { firebaseIdToken ->
                            val firebaseResult = authManager.loginWithFirebase(
                                idToken = firebaseIdToken,
                                username = _username.value.ifBlank { kmpAuthUser.displayName ?: "google-user" },
                                email = kmpAuthUser.email ?: _email.value.ifBlank { null },
                            )
                            firebaseResult.fold(
                                onSuccess = { onAuthenticated() },
                                onFailure = {
                                    Napier.w(it) { "Google sign-in failed 1" }
                                    _message.value = it.message ?: "Google sign-in failed"
                                },
                            )
                        },
                        onFailure = {
                            Napier.w(it) { "Failed to retrieve Firebase ID token" }
                            _message.value = it.message ?: "Failed to retrieve Firebase ID token"
                        },
                    )
                }
            },
            onFailure = {
                Napier.w(it) { "Google sign-in failed 2" }
                _message.value = it.message ?: "Google sign-in failed"
            },
        )
    }
}
