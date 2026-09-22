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

enum class AuthMode {
    Login,
    Register,
}

@Inject
class AuthViewModel(
    private val authManager: AuthManager,
) : ViewModel() {

    private val _mode = MutableStateFlow(AuthMode.Login)
    val mode: StateFlow<AuthMode> = _mode.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    fun updateEmail(value: String) {
        _email.value = value
    }

    fun updatePassword(value: String) {
        _password.value = value
    }

    fun toggleMode() {
        _mode.value = if (_mode.value == AuthMode.Login) AuthMode.Register else AuthMode.Login
        _message.value = null
    }

    fun clearMessage() {
        _message.value = null
    }

    fun handleAuthResult(result: Result<KMPAuthUser>, onAuthenticated: () -> Unit) {
        result.fold(
            onSuccess = { kmpAuthUser ->
                viewModelScope.launch {
                    val tokenResult = KMPAuth.currentUserIdToken()
                    tokenResult.fold(
                        onSuccess = { firebaseIdToken ->
                            val userEmail = kmpAuthUser.email ?: _email.value.trim().ifBlank { null }
                            val userName = kmpAuthUser.displayName?.trim().takeUnless { it.isNullOrBlank() }
                                ?: userEmail?.substringBefore('@')
                                ?: "user"
                            val firebaseResult = authManager.loginWithFirebase(
                                idToken = firebaseIdToken,
                                username = userName,
                                email = userEmail,
                            )
                            firebaseResult.fold(
                                onSuccess = { onAuthenticated() },
                                onFailure = {
                                    Napier.w(it) { "Firebase sign-in failed 1" }
                                    _message.value = it.message ?: "Firebase sign-in failed"
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
                Napier.w(it) { "Firebase sign-in failed 2" }
                _message.value = it.message ?: "Firebase sign-in failed"
            },
        )
    }
}
