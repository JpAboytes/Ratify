// Spotify API Configuration
const SPOTIFY_CLIENT_ID = import.meta.env.VITE_SPOTIFY_CLIENT_ID
const SPOTIFY_CLIENT_SECRET = import.meta.env.VITE_SPOTIFY_CLIENT_SECRET

export async function getToken() {
  const storedToken = localStorage.getItem('spotify_token')
  const expiration = localStorage.getItem('spotify_token_expiration')
  
  if (storedToken && expiration) {
    if (new Date().getTime() < parseInt(expiration)) {
      return storedToken
    }
  }

  const result = await fetch("https://accounts.spotify.com/api/token", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      Authorization: "Basic " + btoa(SPOTIFY_CLIENT_ID + ":" + SPOTIFY_CLIENT_SECRET),
    },
    body: "grant_type=client_credentials",
  })

  const data = await result.json()
  
  if (data.access_token) {
    const expirationTime = new Date().getTime() + (3600 * 1000)
    localStorage.setItem('spotify_token', data.access_token)
    localStorage.setItem('spotify_token_expiration', expirationTime)
  }
  
  return data.access_token
}

export function getStoredToken() {
  const token = localStorage.getItem('spotify_token')
  const expiration = localStorage.getItem('spotify_token_expiration')
  
  if (!token || !expiration) return null
  
  if (new Date().getTime() > parseInt(expiration)) {
    localStorage.removeItem('spotify_token')
    localStorage.removeItem('spotify_token_expiration')
    return null
  }
  
  return token
}

export async function fetchSpotifyApi(endpoint, method = 'GET', body = null) {
  const token = await getToken()
  
  if (!token) {
    throw new Error('No se pudo obtener token de Spotify')
  }
  
  const res = await fetch(`https://api.spotify.com/v1/${endpoint}`, {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    method,
    body: body ? JSON.stringify(body) : null
  })
  
  if (!res.ok) {
    throw new Error(`Error de Spotify API: ${res.status}`)
  }
  
  return await res.json()
}

// Endpoints específicos de Spotify
export const spotifyApi = {
  // Obtener categorías
  async getCategories() {
    const token = await getToken()
    const result = await fetch(`https://api.spotify.com/v1/browse/categories`, {
      method: "GET",
      headers: { Authorization: "Bearer " + token },
    })
    const data = await result.json()
    return data.categories.items
  },
  
  // Buscar álbumes
  async searchAlbums(query, limit = 20) {
    return await fetchSpotifyApi(
      `search?q=${encodeURIComponent(query)}&type=album&limit=${limit}`
    )
  },
  
  // Obtener detalles de un álbum
  async getAlbum(albumId) {
    return await fetchSpotifyApi(`albums/${albumId}`)
  },
  
  // Obtener álbumes de un artista
  async getArtistAlbums(artistId, limit = 20) {
    return await fetchSpotifyApi(
      `artists/${artistId}/albums?limit=${limit}`
    )
  },
  
  // Obtener nuevos lanzamientos
  async getNewReleases(limit = 100) {
    return await fetchSpotifyApi(`browse/new-releases?limit=${limit}`)
  },
  
  // Obtener playlists destacadas
  async getFeaturedPlaylists(limit = 20) {
    return await fetchSpotifyApi(`browse/featured-playlists?limit=${limit}`)
  },
  
  // Buscar artistas
  async searchArtists(query, limit = 20) {
    return await fetchSpotifyApi(
      `search?q=${encodeURIComponent(query)}&type=artist&limit=${limit}`
    )
  },
  
  // Buscar tracks
  async searchTracks(query, limit = 20) {
    return await fetchSpotifyApi(
      `search?q=${encodeURIComponent(query)}&type=track&limit=${limit}`
    )
  }
}
