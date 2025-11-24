package com.example.ratify
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ratify.data.Review
import com.example.ratify.viewmodels.CURRENT_USER_ID
import com.example.ratify.viewmodels.CURRENT_USER_NAME
import com.example.ratify.viewmodels.AlbumDetailViewModel
import com.example.ratify.viewmodels.AlbumDetailViewModelFactory

val PrimaryColor = Color(0xFF667eea)
val BackgroundColor = Color(0xFF000000)
val CardColor = Color(0xFF1a1a1a)
val TextLight = Color(0xFF999999)
val TextFaded = Color(0xFF666666)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    album: Album,
    onBack: () -> Unit,
    onSaveReview: (Album, Int, String, String?) -> Unit,
    viewModel: AlbumDetailViewModel = viewModel(
        factory = AlbumDetailViewModelFactory(album)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.album.name,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardColor),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
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
                .verticalScroll(rememberScrollState())
        ) {
            AlbumHeader(album)
            AverageRatingSection(album.averageRating, album.reviewCount)

            ReviewSection(
                isEditing = uiState.userReview != null,
                rating = uiState.editingRating,
                comment = uiState.editingComment,
                onRatingChange = viewModel::setRating,
                onCommentChange = viewModel::setComment,
                onSave = {
                    onSaveReview(
                        album,
                        uiState.editingRating,
                        uiState.editingComment,
                        uiState.userReview?.reviewId
                    )
                }
            )
            AllReviewsList(
                reviews = uiState.album.reviews ?: emptyList(),
                currentUserId = uiState.currentUserId
            )
        }
    }
}

@Composable
fun AlbumHeader(album: Album) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = album.images.firstOrNull()?.url,
            contentDescription = album.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = album.name,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = album.artists.joinToString(", ") { it.name },
                color = TextLight,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Lanzamiento: ${album.release_date ?: "N/A"}",
                color = TextLight,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun AverageRatingSection(averageRating: Double, reviewCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Divider(color = TextFaded.copy(alpha = 0.3f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = String.format("%.1f", averageRating),
                color = Color.Yellow,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                RatingBarStatic(rating = averageRating.toFloat())
                Text(
                    text = "$reviewCount ${if (reviewCount == 1) "Review" else "Review"} totales",
                    color = TextLight,
                    fontSize = 14.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = TextFaded.copy(alpha = 0.3f), thickness = 1.dp)
    }
}

@Composable
fun ReviewSection(
    isEditing: Boolean,
    rating: Int,
    comment: String,
    onRatingChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = if (isEditing) "Tu Review (Editar)" else "Agregar Tu Review",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Usuario: $CURRENT_USER_NAME",
            color = TextLight,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Rating de 5 Estrellas
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            (1..5).forEach { starIndex ->
                IconButton(
                    onClick = { onRatingChange(starIndex) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "$starIndex estrellas",
                        tint = if (starIndex <= rating) Color.Yellow else TextFaded,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Campo de Comentario
        OutlinedTextField(
            value = comment,
            onValueChange = onCommentChange,
            label = { Text("Comentario (Opcional)") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
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

        Spacer(modifier = Modifier.height(16.dp))

        // Botón Guardar
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
        ) {
            Text(if (isEditing) "Guardar Cambios" else "Publicar Review", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AllReviewsList(reviews: List<Review>, currentUserId: String) {
    if (reviews.isEmpty()) return

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Otros Comentarios",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        reviews
            .filter { it.userId != currentUserId }
            .forEach { review ->
                ReviewItem(review)
                Spacer(modifier = Modifier.height(12.dp))
            }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ReviewItem(review: Review) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardColor.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = review.userName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            RatingBarStatic(rating = review.rating.toFloat())
            Spacer(modifier = Modifier.height(8.dp))
            if (review.comment.isNotBlank()) {
                Text(
                    text = review.comment,
                    color = TextLight,
                    fontSize = 14.sp
                )
            } else {
                Text(
                    text = "(Sin comentario)",
                    color = TextFaded,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun RatingBarStatic(rating: Float, maxStars: Int = 5) {
    Row {
        repeat(maxStars) { index ->
            val fillPercentage = (rating - index).coerceIn(0f, 1f)
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = when {
                    fillPercentage >= 1f -> Color.Yellow
                    fillPercentage > 0f -> Color.Yellow.copy(alpha = 0.5f)
                    else -> TextFaded
                },
                modifier = Modifier.size(20.dp)
            )
        }
    }
}