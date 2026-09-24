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

data class OnboardingUser(
    val idToken: String,
    val email: String?,
)

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

    private val _onboardingUser = MutableStateFlow<OnboardingUser?>(null)
    val onboardingUser: StateFlow<OnboardingUser?> = _onboardingUser.asStateFlow()

    private val _chosenUsername = MutableStateFlow("")
    val chosenUsername: StateFlow<String> = _chosenUsername.asStateFlow()

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

    fun updateChosenUsername(value: String) {
        _chosenUsername.value = value
    }

    internal fun setOnboardingUserForTesting(onboarding: OnboardingUser?) {
        _onboardingUser.value = onboarding
    }

    fun toggleMode() {
        _mode.value = if (_mode.value == AuthMode.Login) AuthMode.Register else AuthMode.Login
        _message.value = null
    }

    fun clearMessage() {
        _message.value = null
    }

    fun cancelOnboarding() {
        _onboardingUser.value = null
        _chosenUsername.value = ""
        _message.value = null
    }

    fun completeOnboarding(onAuthenticated: () -> Unit) {
        val onboarding = _onboardingUser.value ?: return
        val username = _chosenUsername.value.trim()
        if (username.isBlank()) {
            _message.value = "Please choose a username"
            return
        }

        viewModelScope.launch {
            _submitting.value = true
            _message.value = null
            val result = authManager.registerUser(
                idToken = onboarding.idToken,
                username = username,
                email = onboarding.email,
            )
            _submitting.value = false
            result.fold(
                onSuccess = {
                    _onboardingUser.value = null
                    onAuthenticated()
                },
                onFailure = {
                    Napier.w(it) { "Username onboarding registration failed" }
                    _message.value = it.message ?: "Registration failed"
                },
            )
        }
    }

    fun handleAuthResult(result: Result<KMPAuthUser>, onAuthenticated: () -> Unit) {
        result.fold(
            onSuccess = { kmpAuthUser ->
                viewModelScope.launch {
                    _submitting.value = true
                    _message.value = null
                    val tokenResult = KMPAuth.currentUserIdToken()
                    tokenResult.fold(
                        onSuccess = { firebaseIdToken ->
                            val userEmail = kmpAuthUser.email ?: _email.value.trim().ifBlank { null }
                            // First, check if the user is already provisioned on the backend
                            val existingUserResult = authManager.checkExistingUser()
                            if (existingUserResult.isSuccess) {
                                _submitting.value = false
                                onAuthenticated()
                            } else {
                                // User is authenticated in Firebase but not provisioned on backend yet.
                                // Route to username onboarding.
                                val suggestedUsername = kmpAuthUser.displayName?.trim().takeUnless { it.isNullOrBlank() }
                                    ?: userEmail?.substringBefore('@')?.trim().takeUnless { it.isNullOrBlank() }
                                    ?: ""
                                _chosenUsername.value = suggestedUsername
                                _onboardingUser.value = OnboardingUser(
                                    idToken = firebaseIdToken,
                                    email = userEmail,
                                )
                                _submitting.value = false
                            }
                        },
                        onFailure = {
                            _submitting.value = false
                            Napier.w(it) { "Failed to retrieve Firebase ID token" }
                            _message.value = it.message ?: "Failed to retrieve Firebase ID token"
                        },
                    )
                }
            },
            onFailure = {
                _submitting.value = false
                Napier.w(it) { "Firebase sign-in failed 2" }
                _message.value = it.message ?: "Firebase sign-in failed"
            },
        )
    }
}
