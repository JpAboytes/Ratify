package com.example.ratify.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ratify.data.AlbumListResponse
import com.example.ratify.data.UiState
import com.example.ratify.handlers.SpotifyApiHandler
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.ratify.data.HomeData

class HomeViewModel(
    private val spotifyApi: SpotifyApiHandler
) : ViewModel() {

    private val _homeUiState = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val homeUiState: StateFlow<UiState<HomeData>> = _homeUiState

    private val gson = Gson()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _homeUiState.value = UiState.Loading
            try {
                if (spotifyApi.spotifyToken == null) {
                    val tokenResponse = spotifyApi.getAccessTokenClientCredentials()
                    spotifyApi.spotifyToken = tokenResponse.accessToken
                }

                val releasesJson = spotifyApi.obtenerAlbumsNuevos()

                val releasesResponse = gson.fromJson(releasesJson, AlbumListResponse::class.java)
                val newReleases = releasesResponse.albums.items.take(50)

                _homeUiState.value = UiState.Success(
                    HomeData(albums = newReleases, isSearching = false)
                )

            } catch (e: Exception) {
                _homeUiState.value = UiState.Error(e.message ?: "Error al cargar nuevos lanzamientos")
            }
        }
    }

    fun searchAlbums(query: String) {
        if (query.isBlank()) {
            loadHomeData()
            return
        }

        viewModelScope.launch {
            _homeUiState.value = UiState.Loading
            try {
                val searchJson = spotifyApi.buscarAlbums(query)
                val searchResponse = gson.fromJson(searchJson, AlbumListResponse::class.java)
                val searchResults = searchResponse.albums.items

                _homeUiState.value = UiState.Success(
                    HomeData(albums = searchResults, isSearching = true, searchPerformed = true)
                )

            } catch (e: Exception) {
                _homeUiState.value = UiState.Error(e.message ?: "Error al realizar la búsqueda")
            }
        }
    }
}