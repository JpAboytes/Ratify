package com.example.ratify.data
import com.google.gson.annotations.SerializedName

data class Album(
    val id: String,
    val name: String,
    val release_date: String?,
    val total_tracks: Int,
    val artists: List<Artist>,
    val images: List<Image>
)


data class Artist(
    val id: String,
    val name: String
)

data class Image(
    val url: String,
    val height: Int?,
    val width: Int?
)

data class AlbumListResponse(
    val albums: PagedAlbums
)

data class PagedAlbums(
    val items: List<Album>
)


data class SpotifyTokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("expires_in")
    val expiresIn: Int
)

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
data class HomeData(
    val albums: List<Album> = emptyList(),
    val isSearching: Boolean = false,
    val searchPerformed: Boolean = false
)