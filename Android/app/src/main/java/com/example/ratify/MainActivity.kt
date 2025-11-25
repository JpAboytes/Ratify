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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import com.example.ratify.data.Album
import com.example.ratify.ui.screens.AuthScreen
import com.example.ratify.handlers.AuthHandler
import com.example.ratify.handlers.FirestoreApiHandler
import com.example.ratify.ui.screens.BackgroundColor
import com.example.ratify.viewmodels.HomeViewModel
import com.example.ratify.viewmodels.AlbumDetailViewModelFactory
import com.example.ratify.viewmodels.AlbumDetailViewModel
import okhttp3.OkHttpClient

sealed class Screen {
    object Auth : Screen()
    object Home : Screen()
    data class Detail(val album: Album) : Screen()
}

class MainActivity : ComponentActivity() {

    private val httpClient by lazy { OkHttpClient() }
    private val spotifyApiHandler by lazy { SpotifyApiHandler(httpClient) }
    private val firestoreApiHandler by lazy { FirestoreApiHandler() }
    private val authHandler by lazy { AuthHandler() }

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

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val homeViewModelFactory = HomeViewModelFactory(spotifyApiHandler)
        val context = this

        val initialScreen = if (authHandler.isUserLoggedIn()) Screen.Home else Screen.Auth

        setContent {
            RatifyTheme {
                var currentScreen by remember { mutableStateOf<Screen>(initialScreen) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (val screen = currentScreen) {
                        is Screen.Auth -> {
                            AuthScreen(
                                authHandler = authHandler,
                                onAuthSuccess = {
                                    currentScreen = Screen.Home
                                }
                            )
                        }
                        is Screen.Home -> {
                            HomeScreen(
                                modifier = Modifier.padding(innerPadding),
                                viewModel = viewModel(factory = homeViewModelFactory),
                                onAlbumClick = { album ->
                                    currentScreen = Screen.Detail(album)
                                }
                            )
                            Scaffold(
                                containerColor = com.example.ratify.ui.screens.BackgroundColor,
                                topBar = {
                                    TopAppBar(
                                        title = { Text("Ratify", color = Color.White) },
                                        colors = TopAppBarDefaults.topAppBarColors(containerColor = CardColor),
                                        actions = {
                                            IconButton(
                                                onClick = {
                                                    authHandler.signOut()
                                                    currentScreen = Screen.Auth
                                                    Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Filled.ExitToApp,
                                                    contentDescription = "Cerrar sesión",
                                                    tint = Color.White
                                                )
                                            }
                                        }
                                    )
                                }
                            ) { homePadding ->
                                HomeScreen(
                                    modifier = Modifier.padding(homePadding),
                                    viewModel = viewModel(factory = homeViewModelFactory),
                                    onAlbumClick = { album ->
                                        currentScreen = Screen.Detail(album)
                                    }
                                )
                            }
                        }
                        is Screen.Detail -> {
                            val album = screen.album

                            val detailViewModel: AlbumDetailViewModel = viewModel(
                                key = album.id,
                                factory = AlbumDetailViewModelFactory(
                                    album = album,
                                    firestoreHandler = firestoreApiHandler,
                                    authHandler = authHandler
                                )
                            )

                            AlbumDetailScreen(
                                album = album,
                                onBack = {
                                    currentScreen = Screen.Home
                                },
                                onSaveReview = { _, _, _, _ ->
                                    detailViewModel.saveReview(
                                        onSuccess = {
                                            Toast.makeText(context, "¡Review Guardada con éxito!", Toast.LENGTH_LONG).show()
                                            currentScreen = Screen.Home
                                        }
                                    )
                                },
                                viewModel = detailViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}