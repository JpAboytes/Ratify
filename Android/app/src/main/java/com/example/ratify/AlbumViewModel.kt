package com.example.ratify.viewmodels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ratify.data.Album
import com.example.ratify.data.Review
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
data class AlbumDetailUiState(
    val album: Album,
    val rating: Int = 0,
    val currentUserId: String,
    val comment: String = "",
    val userReview: Review?,
    val editingRating: Int,
    val editingComment: String,
    val isSaving: Boolean = false
)
const val CURRENT_USER_ID = "user_123"
const val CURRENT_USER_NAME = "Paul Cruz"

class AlbumDetailViewModel(
    album: Album
) : ViewModel() {
    private val userReview = album.reviews?.find { it.userId == CURRENT_USER_ID }


    private val _uiState = MutableStateFlow(
        AlbumDetailUiState(
            album = album,
            currentUserId = CURRENT_USER_ID,
            userReview = userReview,
            editingRating = userReview?.rating ?: 0,
            editingComment = userReview?.comment ?: ""
        )
    )
    val uiState: StateFlow<AlbumDetailUiState> = _uiState

    fun setRating(newRating: Int) {
        val safeRating = newRating.coerceIn(0, 5)
        _uiState.update { it.copy(editingRating = safeRating) }
    }

    fun setComment(newComment: String) {
        _uiState.update { it.copy(editingComment = newComment) }
    }
}

class AlbumDetailViewModelFactory(
    private val album: Album
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlbumDetailViewModel::class.java)) {
            return AlbumDetailViewModel(album) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}