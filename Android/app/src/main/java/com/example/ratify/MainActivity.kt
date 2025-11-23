package com.example.ratify

import android.content.ContentValues.TAG
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ratify.handlers.SpotifyApiHandler
import com.example.ratify.ui.screens.HomeScreen
import com.example.ratify.ui.theme.RatifyTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log
import com.example.ratify.viewmodels.HomeViewModel
import okhttp3.OkHttpClient
import kotlin.getValue

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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    HomeScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel(factory = homeViewModelFactory),
                        onAlbumClick = { album ->
                            Log.d(TAG,"Falta agregar");
                        }
                    )
                }
            }
        }
    }
}
