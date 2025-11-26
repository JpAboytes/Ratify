package com.example.ratify.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ratify.data.Album
import com.example.ratify.data.UiState
import com.example.ratify.data.HomeData
import com.example.ratify.viewmodels.HomeViewModel


val PrimaryColor = Color(0xFF667eea)
val BackgroundColor = Color(0xFF000000)
val CardColor = Color(0xFF1a1a1a)
val TextLight = Color(0xFF999999)
val TextFaded = Color(0xFF666666)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier
) {
    val uiState by viewModel.homeUiState.collectAsState()
    val homeData = (uiState as? UiState.Success)?.data

    Scaffold(
        containerColor = BackgroundColor,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Text(
                text = "Ratify",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
            )
            SearchBar(viewModel)

            when (uiState) {
                is UiState.Loading -> LoadingState()
                is UiState.Error -> ErrorState((uiState as UiState.Error).message)
                is UiState.Success -> {
                    if (homeData != null) {
                        AlbumsSection(homeData, onAlbumClick)
                    }
                }
                is UiState.Idle -> Unit
            }
        }
    }
}
@Composable
fun SearchBar(viewModel: HomeViewModel) {
    var query by remember { mutableStateOf("") }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    val performSearch: () -> Unit = {
        viewModel.searchAlbums(query)
        keyboardController?.hide()
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Buscar álbumes, artistas...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar", tint = TextFaded) },

            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),

            keyboardActions = KeyboardActions(
                onSearch = { performSearch() }
            ),

            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardColor,
                unfocusedContainerColor = CardColor,
                cursorColor = PrimaryColor,
                focusedBorderColor = PrimaryColor,
                focusedLabelColor = PrimaryColor,
                unfocusedLabelColor = TextFaded,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = performSearch,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
        ) {
            Text("Buscar Álbum / Artista")
        }
    }
}

@Composable
fun AlbumsSection(homeData: HomeData, onAlbumClick: (Album) -> Unit) {
    val title = when {
        homeData.isSearching -> "Resultados de Búsqueda"
        homeData.albums.isNotEmpty() -> "Nuevos Lanzamientos (50)"
        else -> "No hay álbumes disponibles"
    }

    Text(
        text = title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 10.dp)
    )

    if (homeData.albums.isEmpty() && homeData.searchPerformed) {
        Text("No se encontraron álbumes para esta búsqueda.", color = TextLight, modifier = Modifier.padding(16.dp))
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(homeData.albums) { album ->
            AlbumCard(album, onAlbumClick)
        }
    }
}

@Composable
fun AlbumCard(album: Album, onClick: (Album) -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(album) }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = album.images.firstOrNull()?.url,
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )

            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = album.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.artists.joinToString(", ") { it.name },
                    color = TextLight,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = PrimaryColor)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Cargando álbumes...", color = TextLight)
    }
}

@Composable
fun ErrorState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Error de conexión:", color = Color.Red, fontWeight = FontWeight.Bold)
        Text(message, color = TextLight, modifier = Modifier.padding(8.dp))
    }
}