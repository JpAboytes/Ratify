package com.example.ratify.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ratify.data.UserAlbumRating
import com.example.ratify.handlers.AuthHandler
import com.example.ratify.handlers.FirestoreApiHandler
import com.example.ratify.handlers.SpotifyApiHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

data class RatedAlbumDetails(
    val albumId: String,
    val rating: Int,
    val name: String = "Álbum Desconocido",
    val artistName: String = "Artista Desconocido",
    val imageUrl: String? = null
)

data class ProfileUiState(
    val userId: String = "",
    val userName: String = "Anónimo",
    val email: String = "",
    val profilePictureUrl: String? = null,
    val isLoading: Boolean = false,
    val ratedAlbums: List<RatedAlbumDetails> = emptyList(),
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val authHandler: AuthHandler,
    private val firestoreHandler: FirestoreApiHandler,
    private val spotifyApiHandler: SpotifyApiHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val firebaseUser = authHandler.auth.currentUser

            if (firebaseUser == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Usuario no autenticado", userId = "Anonimo") }
                return@launch
            }

            val currentUserId = firebaseUser.uid
            val currentEmail = firebaseUser.email ?: "N/A"
            val currentUserName = firebaseUser.displayName
                ?: currentEmail.substringBefore('@')
                ?: "User_${currentUserId.substring(0, 8)}"

            _uiState.update {
                it.copy(
                    userId = currentUserId,
                    userName = currentUserName,
                    email = currentEmail,
                    profilePictureUrl = firebaseUser.photoUrl?.toString()
                )
            }

            try {
                val profileData = firestoreHandler.getUserProfile(currentUserId)

                val detailedRatings = fetchAlbumDetails(profileData.albums)

                _uiState.update {
                    it.copy(
                        ratedAlbums = detailedRatings,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error al cargar perfil de usuario", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error al cargar tu historial de calificaciones."
                    )
                }
            }
        }
    }

    private suspend fun fetchAlbumDetails(ratings: List<UserAlbumRating>): List<RatedAlbumDetails> {
        if (spotifyApiHandler.spotifyToken == null) {
            try {
                val tokenResponse = spotifyApiHandler.getAccessTokenClientCredentials()
                spotifyApiHandler.spotifyToken = tokenResponse.accessToken
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Fallo al obtener token de Spotify", e)
                return ratings.map { RatedAlbumDetails(it.albumId, it.rating) } 
            }
        }

        return ratings.map { rating ->
            viewModelScope.async {
                val album = spotifyApiHandler.obtenerAlbumPorId(rating.albumId)

                RatedAlbumDetails(
                    albumId = rating.albumId,
                    rating = rating.rating,
                    name = album?.name ?: "Álbum Desconocido",
                    artistName = album?.artists?.joinToString(", ") { it.name } ?: "Artista Desconocido",
                    imageUrl = album?.images?.firstOrNull()?.url
                )
            }
        }.awaitAll()
    }
}

class ProfileViewModelFactory(
    private val authHandler: AuthHandler,
    private val firestoreHandler: FirestoreApiHandler,
    private val spotifyApiHandler: SpotifyApiHandler
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(authHandler, firestoreHandler, spotifyApiHandler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}