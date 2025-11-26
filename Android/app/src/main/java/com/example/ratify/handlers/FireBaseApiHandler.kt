package com.example.ratify.handlers

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.util.Log
import com.example.ratify.data.AlbumRatings
import com.example.ratify.data.Review
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.GoogleAuthProvider
import com.example.ratify.data.UserAlbumRating
import com.example.ratify.data.UserProfileData
import com.google.firebase.firestore.FieldValue
import kotlin.collections.set

class FirestoreApiHandler {

    private val db = FirebaseFirestore.getInstance()
    private val RATINGS_COLLECTION = "ratings"
    private val USERS_COLLECTION = "users"
    private val TAG = "FirestoreApiHandler"

    suspend fun getAlbumRatings(albumId: String): AlbumRatings {
        val documentRef = db.collection(RATINGS_COLLECTION).document(albumId)
        return suspendCancellableCoroutine { continuation ->
            documentRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        try {
                            Log.d(TAG, "Se encontro el album: $albumId")
                            val albumRatings = document.toObject(AlbumRatings::class.java)
                            continuation.resume(albumRatings ?: AlbumRatings())
                        } catch (e: Exception) {
                            Log.e(TAG, "Error mapeando calificaciones para $albumId", e)
                            continuation.resumeWithException(e)
                        }
                    } else {
                        continuation.resume(AlbumRatings())
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error al obtener calificaciones para $albumId", exception)
                    continuation.resumeWithException(exception)
                }
        }
    }
    suspend fun postReview(albumId: String, newReview: Review,albumImage: String, albumName: String, artistName: String) {

        val albumDocRef = db.collection(RATINGS_COLLECTION).document(albumId)
        val userDocRef = db.collection(USERS_COLLECTION).document(newReview.userId)

        try {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(albumDocRef)
                val currentRatings = snapshot.toObject(AlbumRatings::class.java) ?: AlbumRatings()

                val mutableReviews = currentRatings.reviews.toMutableList()
                val existingReviewIndex = mutableReviews.indexOfFirst { it.reviewId == newReview.reviewId }

                if (existingReviewIndex != -1) {
                    mutableReviews.removeAt(existingReviewIndex)
                    mutableReviews.add(newReview)
                } else {
                    mutableReviews.add(newReview)
                }

                val totalCount = mutableReviews.size
                val totalSum = mutableReviews.sumOf { it.rating }
                val newAverage = if (totalCount > 0) totalSum.toDouble() / totalCount else 0.0

                val updatedAlbumData = hashMapOf<String, Any>(
                    "reviewCount" to totalCount,
                    "averageRating" to newAverage,
                    "reviews" to mutableReviews,
                    "albumImage" to albumImage,
                    "albumName" to albumName,
                    "artistName" to artistName,
                    "lastUpdatedAt" to FieldValue.serverTimestamp()
                )
                if (!snapshot.exists()) {
                    updatedAlbumData["createdAt"] = FieldValue.serverTimestamp()
                }
                transaction.set(albumDocRef, updatedAlbumData)
                null
            }.await()

        } catch (e: Exception) {
            Log.e(TAG, "FALLO en Transacción de Álbum para $albumId", e)
            throw e
        }

        try {
            db.runTransaction { transaction ->
                val userSnapshot = transaction.get(userDocRef)

                val currentUserProfile = userSnapshot.toObject(UserProfileData::class.java) ?: UserProfileData()

                val mutableAlbums = currentUserProfile.albums.toMutableList()
                val existingAlbumIndex = mutableAlbums.indexOfFirst { it.albumId == albumId }
                val newUserRating = UserAlbumRating(albumId = albumId, rating = newReview.rating)

                if (existingAlbumIndex != -1) {
                    mutableAlbums[existingAlbumIndex] = newUserRating
                } else {
                    mutableAlbums.add(newUserRating)
                }

                val updatedProfileData = mapOf("albums" to mutableAlbums)
                transaction.set(userDocRef, updatedProfileData)

                null
            }.await()

            Log.d(TAG, "Review de ${newReview.userId} guardada con éxito en álbum $albumId y perfil actualizado.")

        } catch (e: Exception) {
            Log.e(TAG, "FALLO en Transacción de Perfil para ${newReview.userId}", e)
            throw e
        }
    }
    suspend fun saveUserName(userId: String, userName: String) {
        val userDocRef = db.collection(USERS_COLLECTION).document(userId)
        try {
            userDocRef.set(mapOf("userName" to userName), com.google.firebase.firestore.SetOptions.merge())
                .await()
            Log.d(TAG, "Nombre de usuario '$userName' guardado con éxito para $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar el nombre de usuario para $userId", e)
            throw e
        }
    }
    suspend fun getUserProfile(userId: String): UserProfileData {
        val documentRef = db.collection(USERS_COLLECTION).document(userId)
        return try {
            val document = documentRef.get().await()

            if (document.exists()) {
                document.toObject(UserProfileData::class.java) ?: UserProfileData()
            } else {
                val initialProfile = UserProfileData()
                documentRef.set(initialProfile).await()
                Log.d(TAG, "Perfil de usuario inicial creado para $userId")
                initialProfile
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener/crear perfil de usuario para $userId", e)
            throw e
        }
    }
}
class AuthHandler {

    val auth = FirebaseAuth.getInstance()
    private val TAG = "AuthHandler"

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    suspend fun signUp(email: String, password: String): String {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            Log.d(TAG, "Registro exitoso para: $email")
            auth.currentUser?.uid ?: throw IllegalStateException("UID nulo después del registro exitoso.")
        } catch (e: Exception) {
            Log.e(TAG, "Error durante el registro", e)
            throw e
        }
    }

    suspend fun signIn(email: String, password: String): String {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Log.d(TAG, "Login exitoso para: $email")
            auth.currentUser?.uid ?: throw IllegalStateException("UID nulo después del inicio de sesión exitoso.")
        } catch (e: Exception) {
            Log.e(TAG, "Error durante el login", e)
            throw e
        }
    }
    suspend fun signInWithGoogleToken(idToken: String): String {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            auth.currentUser?.uid ?: throw IllegalStateException("UID nulo después del login de Google.")
        } catch (e: Exception) {
            Log.e(TAG, "Error durante el login con Google", e)
            throw e
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}