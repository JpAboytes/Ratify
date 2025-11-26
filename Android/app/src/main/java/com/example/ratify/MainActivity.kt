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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ratify.data.Album
import com.example.ratify.ui.screens.AuthScreen
import com.example.ratify.handlers.AuthHandler
import com.example.ratify.handlers.FirestoreApiHandler
import com.example.ratify.ui.screens.ProfileScreen
import com.example.ratify.viewmodels.HomeViewModel
import com.example.ratify.viewmodels.AlbumDetailViewModelFactory
import com.example.ratify.viewmodels.AlbumDetailViewModel
import com.example.ratify.viewmodels.ProfileViewModel
import com.example.ratify.viewmodels.ProfileViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import okhttp3.OkHttpClient
import com.example.ratify.ui.screens.AlbumDetailScreen

sealed class Screen {
    object Auth : Screen()
    object Home : Screen()
    data class Detail(val album: Album) : Screen()
    object Account : Screen()
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


        val initialScreen = if (authHandler.isUserLoggedIn()) Screen.Home else Screen.Auth

        setContent {
            val context = LocalContext.current
            var currentScreen by remember { mutableStateOf<Screen>(initialScreen) }

            val currentFirebaseUser = remember { mutableStateOf(authHandler.auth.currentUser) }
            DisposableEffect(authHandler) {
                val authStateListener = FirebaseAuth.AuthStateListener { auth ->
                    currentFirebaseUser.value = auth.currentUser
                }
                authHandler.auth.addAuthStateListener(authStateListener)
                onDispose {
                    authHandler.auth.removeAuthStateListener(authStateListener)
                }
            }

            val topBar: @Composable () -> Unit = {
                if (currentScreen is Screen.Home ) {
                    val user = currentFirebaseUser.value
                    val isLoggedIn = user != null

                    val displayName = user?.run {
                        displayName ?: email?.substringBefore('@') ?: "User_${uid.substring(0, 4)}"
                    } ?: ""

                    TopAppBar(
                        title = { Text("Ratify", color = Color.White) },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = com.example.ratify.ui.screens.CardColor),
                        actions = {
                            if (isLoggedIn) {
                                AccountButton(
                                    photoUrl = user?.photoUrl?.toString(),
                                    displayName = displayName,
                                    onClick = { currentScreen = Screen.Account }
                                )
                            }
                            IconButton(
                                onClick = {
                                    authHandler.signOut()
                                    currentScreen = Screen.Auth
                                    Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = "Cerrar sesión",
                                    tint = Color.White
                                )
                            }
                        }
                    )
                }
            }


            RatifyTheme {

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = com.example.ratify.ui.screens.BackgroundColor,
                    topBar = topBar
                ) { innerPadding ->
                    val currentUserId = currentFirebaseUser.value?.uid ?: "GUEST_SESSION"

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
                                            Toast.makeText(
                                                context,
                                                "¡Review Guardada con éxito!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            currentScreen = Screen.Home
                                        }
                                    )
                                },
                                viewModel = detailViewModel
                            )
                        }
                        is Screen.Account -> {
                            val profileViewModel: ProfileViewModel = viewModel(
                                key = currentUserId,
                                factory = ProfileViewModelFactory(
                                    authHandler = authHandler,
                                    firestoreHandler = firestoreApiHandler,
                                    spotifyApiHandler = spotifyApiHandler
                                )
                            )
                            ProfileScreen(
                                viewModel = profileViewModel,
                                onBack = {
                                    currentScreen = Screen.Home
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InitialPlaceholder(displayName: String) {
    val TAG = "AccountButton"
    val initial = displayName.firstOrNull()?.toString()?.uppercase() ?: ""
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(com.example.ratify.ui.screens.PrimaryColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun AccountButton(photoUrl: String?, displayName: String, onClick: () -> Unit) {
    val TAG = "AccountButton"
    Log.d(TAG,"[AccountButton] Entrando. URL: ${photoUrl?.take(50)}")

    IconButton(onClick = onClick, modifier = Modifier.padding(horizontal = 4.dp)) {

        if (photoUrl != null && photoUrl.isNotBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Foto de perfil de $displayName",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )
        } else {
            Log.d(TAG,"[AccountButton] No hay URL, usando placeholder.")
            InitialPlaceholder(displayName)
        }
    }
}