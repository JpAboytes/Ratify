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

    suspend fun buscarAlbums(query: String): String {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://api.spotify.com/v1/search?type=album&q=$encodedQuery"
        return executeGetRequest(url)
    }


}
