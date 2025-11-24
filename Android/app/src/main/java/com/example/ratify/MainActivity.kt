package com.example.ratify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ratify.handlers.SpotifyApiHandler
import com.example.ratify.ui.screens.HomeScreen
import com.example.ratify.ui.theme.RatifyTheme
import android.util.Log
import android.widget.Toast
import com.example.ratify.data.Album
import com.example.ratify.AlbumDetailScreen
import com.example.ratify.viewmodels.HomeViewModel
import com.example.ratify.viewmodels.AlbumDetailViewModelFactory
import okhttp3.OkHttpClient

sealed class Screen {
    object Home : Screen()
    data class Detail(val album: Album) : Screen()
}

class MainActivity : ComponentActivity() {

    private val httpClient by lazy { OkHttpClient() }
    private val spotifyApiHandler by lazy { SpotifyApiHandler(httpClient) }

    class HomeViewModelFactory(
        private val spotifyApiHandler: SpotifyApiHandler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(spotifyApiHandler) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val homeViewModelFactory = HomeViewModelFactory(spotifyApiHandler)

        setContent {
            RatifyTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (val screen = currentScreen) {
                        is Screen.Home -> {
                            HomeScreen(
                                modifier = Modifier.padding(innerPadding),
                                viewModel = viewModel(factory = homeViewModelFactory),
                                onAlbumClick = { album ->
                                    currentScreen = Screen.Detail(album)
                                }
                            )
                        }
                        is Screen.Detail -> {
                            val album = screen.album
                            AlbumDetailScreen(
                                album = album,
                                onBack = {
                                    currentScreen = Screen.Home
                                },
                                onSaveReview = { album, rating, comment, reviewId ->
                                    Toast.makeText(this, "Review Guardada ($rating/5 estrellas)", Toast.LENGTH_LONG).show()
                                    currentScreen = Screen.Home
                                },
                                viewModel = viewModel(
                                    key = album.id,
                                    factory = AlbumDetailViewModelFactory(album)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}