<template>
  <div class="home-container">
    <header class="header">
      <div class="header-content">
        <h1 class="logo"> Ratify</h1>
        
        <div class="user-info">
          <img 
            v-if="user?.photoURL" 
            :src="user.photoURL" 
            :alt="user.displayName"
            class="user-avatar"
          />
          <span class="user-name">{{ user?.displayName }}</span>
          <button @click="handleLogout" class="logout-button">
            Cerrar sesión
          </button>
        </div>
      </div>
    </header>

    <main class="main-content">
      <div class="welcome-section">
        <h2>¡Bienvenido, {{ user?.displayName?.split(' ')[0] }}! 👋</h2>
        <p>Comienza a calificar tus álbumes favoritos de Spotify</p>
      </div>

      <!-- Search Section -->
      <div class="search-section">
        <div class="search-bar">
          <span class="search-icon">🔍</span>
          <input 
            type="text" 
            placeholder="Buscar álbumes, artistas..." 
            class="search-input"
            v-model="searchQuery"
            @keyup.enter="searchAlbums"
          />
          <button @click="searchAlbums" class="search-button">Buscar</button>
        </div>
      </div>

      <!-- Albums -->
      <div class="albums-section">
        <h3>{{ searchQuery ? 'Resultados de búsqueda' : 'Nuevos Lanzamientos' }}</h3>
        
        <div v-if="loading" class="loading-state">
          <div class="spinner"></div>
          <p>Cargando álbumes...</p>
        </div>
        
        <div v-else class="albums-grid">
          <div 
            v-for="album in albums" 
            :key="album.id" 
            @click="openAlbumModal(album)"
            class="album-card"
          >
            <div class="album-cover">
              <img 
                v-if="album.images && album.images[0]" 
                :src="album.images[0].url" 
                :alt="album.name"
                class="album-image"
              />
              <div v-else class="album-placeholder">
                <span class="music-icon">🎵</span>
              </div>
            </div>
            <div class="album-info">
              <h4 class="album-title">{{ album.name }}</h4>
              <p class="album-artist">
                {{ album.artists.map(a => a.name).join(', ') }}
              </p>
              <p class="album-year">{{ album.release_date?.split('-')[0] }}</p>
              <div class="rating">
                <span v-for="star in 5" :key="star" class="star">⭐</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Album Modal -->
      <AlbumModal 
        v-if="selectedAlbum"
        :album="selectedAlbum"
        @close="selectedAlbum = null"
        @rate="handleRating"
        @comment="handleComment"
      />

      <!-- Stats Section -->
      <div class="stats-section">
        <h3>Tus Estadísticas</h3>
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon">📀</div>
            <div class="stat-value">{{ stats.totalRatings }}</div>
            <div class="stat-label">Álbumes Calificados</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">⭐</div>
            <div class="stat-value">{{ stats.averageRating }}</div>
            <div class="stat-label">Calificación Promedio</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">🎧</div>
            <div class="stat-value">{{ stats.uniqueArtists }}</div>
            <div class="stat-label">Artistas Únicos</div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { spotifyApi } from '../services/spotify'
import { getUserStats } from '../services/ratingService'
import AlbumModal from '../components/AlbumModal.vue'

const router = useRouter()
const { user, signOut } = useAuth()
const searchQuery = ref('')
const albums = ref([])
const loading = ref(false)
const selectedAlbum = ref(null)
const stats = ref({
  totalRatings: 0,
  averageRating: 0,
  uniqueArtists: 0
})

onMounted(async () => {
  await loadNewReleases()
  await loadUserStats()
})

const loadNewReleases = async () => {
  loading.value = true
  try {
    const response = await spotifyApi.getNewReleases(50)
    albums.value = response.albums.items
  } catch (error) {
    console.error('Error al cargar álbumes:', error)
  } finally {
    loading.value = false
  }
}

const searchAlbums = async () => {
  if (!searchQuery.value.trim()) {
    await loadNewReleases()
    return
  }
  
  loading.value = true
  try {
    const response = await spotifyApi.searchAlbums(searchQuery.value)
    albums.value = response.albums.items
  } catch (error) {
    console.error('Error al buscar álbumes:', error)
  } finally {
    loading.value = false
  }
}

const loadUserStats = async () => {
  if (!user.value?.uid) return
  
  try {
    const userStats = await getUserStats(user.value.uid)
    stats.value = userStats
  } catch (error) {
    console.error('Error al cargar estadísticas:', error)
  }
}

const openAlbumModal = (album) => {
  selectedAlbum.value = album
}

const handleRating = async ({ albumId, rating }) => {
  console.log(`Álbum ${albumId} calificado con ${rating} estrellas`)
  await loadUserStats()
}

const handleComment = async ({ albumId, comment, rating }) => {
  console.log(`Comentario para álbum ${albumId}:`, comment, `Rating: ${rating}`)
  await loadUserStats()
}

const handleLogout = async () => {
  try {
    await signOut()
    localStorage.removeItem('spotify_token')
    localStorage.removeItem('spotify_token_expiration')
    router.push('/')
  } catch (err) {
    console.error('Error al cerrar sesión:', err)
  }
}
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background: #000;
}

.header {
  background: #1a1a1a;
  border-bottom: 1px solid #333;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.5);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  margin: 0;
  font-size: 2rem;
  color: #fff;
  text-shadow: 0 0 20px rgba(102, 126, 234, 0.5);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid #667eea;
}

.user-name {
  font-weight: 600;
  color: #fff;
}

.logout-button {
  padding: 8px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s ease;
}

.logout-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

/* Main Content */
.main-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 30px;
}

.welcome-section {
  margin-bottom: 40px;
}

.welcome-section h2 {
  font-size: 2.5rem;
  color: #fff;
  margin: 0 0 10px;
}

.welcome-section p {
  font-size: 1.2rem;
  color: #999;
  margin-bottom: 20px;
}

.spotify-connect-button {
  padding: 12px 30px;
  background: #1DB954;
  color: white;
  border: none;
  border-radius: 30px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  transition: all 0.3s ease;
}

.spotify-connect-button:hover {
  background: #1ed760;
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(29, 185, 84, 0.4);
}

.spotify-icon {
  font-size: 1.2rem;
}

.spotify-connected {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: rgba(29, 185, 84, 0.1);
  border: 1px solid #1DB954;
  border-radius: 30px;
  color: #1DB954;
  font-weight: 600;
}

.check-icon {
  font-size: 1.2rem;
}

/* Search Section */
.search-section {
  margin-bottom: 50px;
}

.search-bar {
  max-width: 700px;
  display: flex;
  align-items: center;
  background: #1a1a1a;
  border: 1px solid #333;
  border-radius: 12px;
  padding: 15px 20px;
  gap: 15px;
}

.search-icon {
  font-size: 1.5rem;
  color: #666;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 1rem;
  color: #fff;
  background: transparent;
}

.search-input::placeholder {
  color: #666;
}

.search-button {
  padding: 8px 20px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s ease;
}

.search-button:hover {
  background: #5568d3;
}

/* Loading */
.loading-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Albums Section */
.albums-section {
  margin-bottom: 50px;
}

.albums-section h3 {
  font-size: 1.8rem;
  color: #fff;
  margin-bottom: 25px;
}

.albums-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 25px;
}

.album-card {
  background: #1a1a1a;
  border: 1px solid #333;
  border-radius: 12px;
  padding: 15px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.album-card:hover {
  transform: translateY(-5px);
  border-color: #667eea;
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.3);
}

.album-cover {
  margin-bottom: 12px;
  position: relative;
  overflow: hidden;
  border-radius: 8px;
}

.album-image {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.album-card:hover .album-image {
  transform: scale(1.05);
}

.album-placeholder {
  width: 100%;
  aspect-ratio: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.music-icon {
  font-size: 3rem;
  opacity: 0.3;
}

.album-info {
  text-align: center;
}

.album-title {
  font-size: 1rem;
  font-weight: 600;
  color: #fff;
  margin: 0 0 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.album-artist {
  font-size: 0.9rem;
  color: #999;
  margin: 0 0 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.album-year {
  font-size: 0.85rem;
  color: #666;
  margin: 0 0 10px;
}

.rating {
  display: flex;
  justify-content: center;
  gap: 3px;
}

.star {
  font-size: 0.8rem;
  opacity: 0.3;
}

/* Stats Section */
.stats-section h3 {
  font-size: 1.8rem;
  color: #fff;
  margin-bottom: 25px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 25px;
}

.stat-card {
  background: #1a1a1a;
  border: 1px solid #333;
  border-radius: 12px;
  padding: 30px;
  text-align: center;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-3px);
  border-color: #667eea;
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.3);
}

.stat-icon {
  font-size: 3rem;
  margin-bottom: 15px;
}

.stat-value {
  font-size: 2.5rem;
  font-weight: bold;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 10px;
}

.stat-label {
  color: #999;
  font-size: 1rem;
}

/* Responsive */
@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    gap: 15px;
  }
  
  .user-info {
    width: 100%;
    justify-content: center;
  }
  
  .welcome-section h2 {
    font-size: 1.8rem;
  }
  
  .albums-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 15px;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
