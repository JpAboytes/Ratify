package com.example.ratify
import android.content.ContentValues.TAG
import android.util.Log
import com.example.ratify.data.AlbumListResponse
import okhttp3.OkHttpClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.ratify.handlers.SpotifyApiHandler
suspend fun runSpotifyApiTest() {
    Log.d(TAG,"*******PRueba Api**********")

    val httpClient = OkHttpClient()
    val spotifyApiHandler = SpotifyApiHandler(httpClient)
    val gson = Gson()

    try {
        Log.d(TAG,"\n1")
        val tokenResponse = spotifyApiHandler.getAccessTokenClientCredentials()

        spotifyApiHandler.spotifyToken = tokenResponse.accessToken
        Log.d(TAG,"Token: ${tokenResponse.accessToken})")

    } catch (e: Exception) {
        Log.d(TAG,"ERROOOOOR ${e.message}")
    }

    try {
        Log.d(TAG,"\n2")
        val releasesJson = spotifyApiHandler.buscarAlbums("Enjambre")
         val type = object : TypeToken<AlbumListResponse>() {}.type
         val releasesResponse: AlbumListResponse = gson.fromJson(releasesJson, type)
         val albums = releasesResponse.albums.items

        Log.d(TAG,"Fragmento de la respuesta: ${releasesJson}...")
        Log.d(TAG,"Albums totales: ${albums.size}")
        Log.d(TAG,"Primer Album: ${albums[0].toString()}")

    } catch (e: Exception) {
        Log.d(TAG,"ERROOOOOR: ${e.message}")
    }

    Log.d(TAG,"\nTodo Fine fine fine")
}