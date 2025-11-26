package com.example.ratify.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ratify.handlers.AuthHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.ratify.handlers.FirestoreApiHandler



data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val userName: String = "",
    val isLoginMode: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(
    private val authHandler: AuthHandler,
    private val firestoreHandler: FirestoreApiHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun setEmail(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun setPassword(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun toggleMode() {
        _uiState.update { it.copy(isLoginMode = !it.isLoginMode, error = null) }
    }

    fun authenticate(onSuccess: () -> Unit) {
        val state = _uiState.value

        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "El email y la contraseña son requeridos.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                if (state.isLoginMode) {
                    authHandler.signIn(state.email, state.password)
                } else {
                    if (uiState.value.userName.isBlank()) {
                        _uiState.update { it.copy(isLoading = false, error = "El nombre de usuario no puede estar vacío.") }
                        return@launch
                    }
                    authHandler.signUp(state.email, state.password)
                }
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                val errorMessage = e.localizedMessage ?: "Error de autenticación desconocido."
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }
    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val userId = authHandler.signInWithGoogleToken(idToken)

                val googleUser = authHandler.auth.currentUser
                val googleName = googleUser?.displayName
                    ?: googleUser?.email?.substringBefore('@')
                    ?: "Usuario Google"
                
                firestoreHandler.saveUserName(userId, googleName)

                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                val errorMessage = e.localizedMessage ?: "Error de autenticación con Google."
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }
    fun setUserName(newName: String) {
        _uiState.update { it.copy(userName = newName, error = null) }
    }

}

class AuthViewModelFactory(
    private val authHandler: AuthHandler,
    private val firestoreHandler: FirestoreApiHandler
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authHandler,firestoreHandler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}