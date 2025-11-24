package com.example.ratify.handlers

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import okhttp3.FormBody
import java.util.Base64
import com.example.ratify.data.SpotifyTokenResponse
import com.google.gson.Gson


import com.google.gson.reflect.TypeToken
import com.example.ratify.data.AlbumListResponse
import com.example.ratify.data.Review
import com.example.ratify.data.Album

import com.example.ratify.viewmodels.CURRENT_USER_ID
import com.example.ratify.viewmodels.CURRENT_USER_NAME

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SpotifyApiHandler(private val httpClient: OkHttpClient) {

    private val CLIENT_ID = "eb68ca0e7c2846b3880eb73be3515fc7"
    private val CLIENT_SECRET = "59bd160c2b1b4762b5c18d59f2279700"
    private val TOKEN_URL = "https://accounts.spotify.com/api/token"
    var spotifyToken: String? = null

    suspend fun getAccessTokenClientCredentials(): SpotifyTokenResponse {
        val credentials = "$CLIENT_ID:$CLIENT_SECRET"
        val base64Credentials = Base64.getEncoder().encodeToString(credentials.toByteArray())

        val requestBody = FormBody.Builder()
            .add("grant_type", "client_credentials")
            .build()
        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(requestBody)
            .header("Authorization", "Basic $base64Credentials")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        return suspendCancellableCoroutine { continuation ->
            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!it.isSuccessful) {
                            val errorBody = it.body?.string() ?: "Error desconocido"
                            continuation.resumeWithException(IOException("Fallo el request: ${it.code}. Body: $errorBody"))
                            return
                        }

                        val responseJson = it.body?.string() ?: throw IOException("Cuerpo vacio")

                        try {
                            val tokenResponse = Gson().fromJson(responseJson, SpotifyTokenResponse::class.java)
                            continuation.resume(tokenResponse)
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        }
                    }
                }
            })

            continuation.invokeOnCancellation {
                httpClient.newCall(request).cancel()
            }
        }
    }
    suspend fun executeGetRequest(url: String): String {
        val token = spotifyToken

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .build()

        return suspendCancellableCoroutine { continuation ->
            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        continuation.resume(it.body?.string() ?: "")
                    }
                }
            })

            continuation.invokeOnCancellation {
                httpClient.newCall(request).cancel()
            }
        }
    }


    /*
    suspend fun buscarAlbums(query: String): String {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://api.spotify.com/v1/search?type=album&q=$encodedQuery"
        return executeGetRequest(url)
    }
    */
    private val gson = Gson()

    private fun injectarReviewsFalsas(albums: List<Album>): List<Album> {
        return albums.mapIndexed { index, album ->
            val otherReviews = listOf(
                Review("rev_a", "user_101", "María G.", 5, "Un clásico de todos los tiempos"),
                Review("rev_b", "user_202", "Pedro R.", 3, "Buen Album, pero prefiero los pasados."),
                Review("rev_c", "user_303", "Laura M.", 2, "No me temrino de gustar.")
            )
            val userReview = if (index == 0) {
                listOf(
                    Review(
                        reviewId = "rev_user_${album.id}",
                        userId = CURRENT_USER_ID,
                        userName = CURRENT_USER_NAME,
                        rating = 5,
                        comment = "Mejor Album de todo el mundo"
                    )
                )
            } else {
                emptyList()
            }

            val allReviews = otherReviews + userReview
            val totalRating = allReviews.sumOf { it.rating }
            val count = allReviews.size
            val average = if (count > 0) totalRating.toDouble() / count else 0.0
            album.copy(
                averageRating = average,
                reviewCount = count,
                reviews = allReviews
            )
        }
    }


    suspend fun buscarAlbums(query: String): String {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://api.spotify.com/v1/search?type=album&q=$encodedQuery"

        val spotifyJson = executeGetRequest(url)

        val type = object : TypeToken<AlbumListResponse>() {}.type
        val releasesResponse: AlbumListResponse = gson.fromJson(spotifyJson, type)

        val modifiedAlbums = injectarReviewsFalsas(releasesResponse.albums.items)


        val modifiedResponse = releasesResponse.copy(
            albums = releasesResponse.albums.copy(items = modifiedAlbums)
        )

        return gson.toJson(modifiedResponse)
    }

    suspend fun obtenerAlbumsNuevos(): String {
        val url = "https://api.spotify.com/v1/browse/new-releases?limit=50"
        return executeGetRequest(url)
    }

    suspend fun getAlbumDetails(albumId: String): String {
        val url = "https://api.spotify.com/v1/albums/$albumId"
        return executeGetRequest(url)
    }


}
