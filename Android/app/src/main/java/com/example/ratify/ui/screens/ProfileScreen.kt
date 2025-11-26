package com.example.ratify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ratify.viewmodels.ProfileViewModel
import com.example.ratify.viewmodels.RatedAlbumDetails


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Tu Perfil", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardColor),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.errorMessage != null -> ErrorMessage(uiState.errorMessage!!)
                else -> {
                    ProfileHeader(uiState.userName, uiState.email, uiState.profilePictureUrl)
                    Spacer(modifier = Modifier.height(24.dp))
                    RatedAlbumsSection(uiState.ratedAlbums)
                }
            }
        }
    }
}

@Composable
fun LoadingIndicator() {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = PrimaryColor)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Cargando perfil...", color = TextLight)
    }
}

@Composable
fun ErrorMessage(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Error:", color = Color.Red, fontWeight = FontWeight.Bold)
        Text(message, color = TextLight, modifier = Modifier.padding(8.dp))
    }
}

@Composable
fun ProfileHeader(userName: String, email: String, pictureUrl: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (pictureUrl != null) {
            AsyncImage(
                model = pictureUrl,
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(PrimaryColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "No hay foto",
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(userName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(email, color = TextLight, fontSize = 16.sp)
    }
}

@Composable
fun RatedAlbumsSection(ratings: List<RatedAlbumDetails>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Álbumes Calificados (${ratings.size})",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        if (ratings.isEmpty()) {
            Text("Aún no has calificado ningún álbum.", color = TextLight)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ratings) { details ->
                    RatingItem(details)
                }
            }
        }
    }
}

@Composable
fun RatingItem(details: RatedAlbumDetails) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (details.imageUrl.isNullOrBlank()) {
                    Box(modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.DarkGray))
                } else {
                    AsyncImage(
                        model = details.imageUrl,
                        contentDescription = details.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = details.name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = details.artistName,
                        color = TextLight,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = details.rating.toString(),
                    color = Color.Yellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color.Yellow,
                    modifier = Modifier.size(16.dp).padding(start = 2.dp)
                )
            }
        }
    }
}