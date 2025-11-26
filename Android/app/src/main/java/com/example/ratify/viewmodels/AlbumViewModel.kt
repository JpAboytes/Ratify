package com.example.ratify.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ratify.data.Album
import com.example.ratify.data.Review
import com.example.ratify.handlers.FirestoreApiHandler
import com.example.ratify.handlers.AuthHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log

data class AlbumDetailUiState(
    val album: Album,
    val rating: Int = 0,
    val currentUserId: String,
    val currentUserName: String,
    val comment: String = "",
    val userReview: Review?,
    val editingRating: Int,
    val editingComment: String,
    val isSaving: Boolean = false,
    val saveError: String? = null
)

class AlbumDetailViewModel(
    album: Album,
    private val firestoreHandler: FirestoreApiHandler,
    private val authHandler: AuthHandler
) : ViewModel() {

    private val initialAlbum = album

    private val actualUserId = authHandler.getCurrentUserId() ?: "Anonimo"


    private val userReview = initialAlbum.reviews?.find { it.userId == actualUserId }

    private val _uiState = MutableStateFlow(
        AlbumDetailUiState(
            album = initialAlbum,
            currentUserId = actualUserId,
            currentUserName = "Cargando...",
            userReview = userReview,
            editingRating = userReview?.rating ?: 0,
            editingComment = userReview?.comment ?: "",
            saveError = null
        )
    )
    val uiState: StateFlow<AlbumDetailUiState> = _uiState

    init {
        loadUserName()
        loadAlbumRatings()
    }
    private fun loadUserName() {
        if (actualUserId == "Anonimo") return

        viewModelScope.launch {
            try {
                val profile = firestoreHandler.getUserProfile(actualUserId)

                val userNameFromFirestore = profile.userName.ifBlank {
                    authHandler.auth.currentUser?.email?.substringBefore('@') ?: "Usuario"
                }
                _uiState.update {
                    it.copy(currentUserName = userNameFromFirestore)
                }

            } catch (e: Exception) {
                Log.e("ViewModel", "Error al cargar nombre de usuario de Firestore", e)

                val fallbackName = authHandler.auth.currentUser?.email?.substringBefore('@') ?: "Usuario"
                _uiState.update { it.copy(currentUserName = fallbackName) }
            }
        }
    }

    private fun loadAlbumRatings() {
        viewModelScope.launch {
            try {
                val ratingsData = firestoreHandler.getAlbumRatings(initialAlbum.id)
                val updatedAlbum = initialAlbum.copy(
                    averageRating = ratingsData.averageRating,
                    reviewCount = ratingsData.reviewCount,
                    reviews = ratingsData.reviews
                )

                val freshUserReview = ratingsData.reviews.find { it.userId == actualUserId }

                _uiState.update {
                    it.copy(
                        album = updatedAlbum,
                        userReview = freshUserReview,
                        editingRating = freshUserReview?.rating ?: it.editingRating,
                        editingComment = freshUserReview?.comment ?: it.editingComment
                    )
                }

            } catch (e: Exception) {
                Log.e("ViewModel", "Error al cargar ratings en init", e)
            }
        }
    }

    fun setRating(newRating: Int) {
        val safeRating = newRating.coerceIn(0, 5)
        _uiState.update { it.copy(editingRating = safeRating, saveError = null) }
    }

    fun setComment(newComment: String) {
        _uiState.update { it.copy(editingComment = newComment, saveError = null) }
    }

    fun saveReview(onSuccess: () -> Unit) {
        val uiStateValue = _uiState.value
        
        if (uiStateValue.currentUserId == "Anonimo") {
            _uiState.update { it.copy(saveError = "Debes iniciar sesión para publicar una review.") }
            return
        }

        if (uiStateValue.editingRating == 0) {
            _uiState.update { it.copy(saveError = "Debes asignar al menos 1 estrella.") }
            return
        }

        val reviewId = "${uiStateValue.currentUserId}_${uiStateValue.album.id}"

        val reviewToSave = Review(
            reviewId = reviewId,
            userId = uiStateValue.currentUserId,
            userName = uiStateValue.currentUserName,
            rating = uiStateValue.editingRating,
            comment = uiStateValue.editingComment.trim()
        )
        val albumImage = uiStateValue.album.images.firstOrNull()?.url ?: ""
        val albumName = uiStateValue.album.name
        val artistName = uiStateValue.album.artists.firstOrNull()?.name ?: "Artista Desconocido"
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }

            try {
                firestoreHandler.postReview(
                    albumId = uiStateValue.album.id,
                    newReview = reviewToSave,
                    albumImage = albumImage,
                    albumName = albumName,
                    artistName = artistName,
                )

                loadAlbumRatings()
                _uiState.update { it.copy(isSaving = false) }
                onSuccess()

            } catch (e: Exception) {
                Log.e("AlbumDetailViewModel", "Error al guardar review", e)
                _uiState.update { it.copy(isSaving = false, saveError = "Error al guardar: ${e.message}") }
            }
        }
    }
}

class AlbumDetailViewModelFactory(
    private val album: Album,
    private val firestoreHandler: FirestoreApiHandler,
    private val authHandler: AuthHandler
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlbumDetailViewModel::class.java)) {
            return AlbumDetailViewModel(album, firestoreHandler, authHandler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}