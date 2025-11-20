<template>
  <div class="my-ratings-container">
    <header class="header">
      <div class="header-content">
        <button @click="goBack" class="back-button">
          ← Volver
        </button>
        <h1 class="title">Ratify</h1>
        <div></div>
      </div>
    </header>

    <main class="main-content">
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>Cargando tus calificaciones...</p>
      </div>

      <div v-else-if="ratings.length === 0" class="empty-state">
        <div class="empty-icon">📀</div>
        <h2>No tienes álbumes calificados aún</h2>
        <p>Comienza a calificar álbumes para verlos aquí</p>
        <button @click="goBack" class="primary-button">
          Buscar Álbumes
        </button>
      </div>

      <div v-else class="ratings-grid">
        <div 
          v-for="rating in ratings" 
          :key="rating.albumId"
          class="rating-card"
        >
          <div class="album-cover">
            <img 
              v-if="rating.albumImage" 
              :src="rating.albumImage" 
              :alt="rating.albumName"
              class="album-image"
            />
            <div v-else class="album-placeholder">
              <span class="music-icon">🎵</span>
            </div>
          </div>
          <div class="rating-info">
            <h3 class="album-name">{{ rating.albumName }}</h3>
            <p class="artist-name">{{ rating.artistName }}</p>
            
            <div class="rating-display">
              <span 
                v-for="star in 5" 
                :key="star"
                class="star"
                :class="{ filled: star <= rating.rating }"
              >
                {{ star <= rating.rating ? '⭐' : '☆' }}
              </span>
              <span class="rating-value">{{ rating.rating }}/5</span>
            </div>

            <div v-if="rating.comment" class="comment-preview">
              <p class="comment-text">{{ rating.comment }}</p>
            </div>

            <div class="rating-date">
              <span v-if="rating.updatedAt">
                {{ formatDate(rating.updatedAt) }}
              </span>
            </div>
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
import { getUserRatings } from '../services/ratingService'

const router = useRouter()
const { user } = useAuth()
const ratings = ref([])
const loading = ref(false)

onMounted(async () => {
  await loadRatings()
})

const loadRatings = async () => {
  if (!user.value?.uid) return
  
  loading.value = true
  try {
    ratings.value = await getUserRatings(user.value.uid)
  } catch (error) {
    console.error('Error al cargar calificaciones:', error)
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/home')
}

const formatDate = (timestamp) => {
  if (!timestamp) return ''
  
  const date = timestamp.toDate?.() || new Date(timestamp)
  const now = new Date()
  const diffTime = Math.abs(now - date)
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24))
  
  if (diffDays === 0) return 'Hoy'
  if (diffDays === 1) return 'Ayer'
  if (diffDays < 7) return `Hace ${diffDays} días`
  if (diffDays < 30) return `Hace ${Math.floor(diffDays / 7)} semanas`
  if (diffDays < 365) return `Hace ${Math.floor(diffDays / 30)} meses`
  
  return date.toLocaleDateString('es-ES', { 
    year: 'numeric', 
    month: 'long', 
    day: 'numeric' 
  })
}
</script>

<style scoped>
.my-ratings-container {
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
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
}

.back-button {
  background: transparent;
  color: #667eea;
  border: 1px solid #667eea;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
  justify-self: start;
}

.back-button:hover {
  background: #667eea;
  color: #fff;
  transform: translateX(-5px);
}

.title {
  margin: 0;
  font-size: 1.8rem;
  color: #fff;
  text-align: center;
  text-shadow: 0 0 20px rgba(102, 126, 234, 0.5);
}

.main-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 30px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  color: #999;
}

.spinner {
  border: 3px solid #333;
  border-top: 3px solid #667eea;
  border-radius: 50%;
  width: 50px;
  height: 50px;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  text-align: center;
  color: #999;
}

.empty-icon {
  font-size: 5rem;
  margin-bottom: 20px;
  opacity: 0.5;
}

.empty-state h2 {
  color: #fff;
  margin: 0 0 10px 0;
  font-size: 1.8rem;
}

.empty-state p {
  margin: 0 0 30px 0;
  font-size: 1.1rem;
}

.primary-button {
  background: #667eea;
  color: #fff;
  border: none;
  padding: 12px 30px;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.primary-button:hover {
  background: #5568d3;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
}

.ratings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 25px;
}

.rating-card {
  background: #1a1a1a;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #333;
  transition: all 0.3s ease;
}

.rating-card:hover {
  transform: translateY(-5px);
  border-color: #667eea;
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.2);
}

.album-cover {
  position: relative;
  width: 100%;
  padding-top: 100%;
  overflow: hidden;
}

.album-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.album-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000;
}

.music-icon {
  font-size: 4rem;
  opacity: 0.3;
}

.rating-info {
  padding: 20px;
}

.album-name {
  margin: 0 0 5px 0;
  font-size: 1.2rem;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.artist-name {
  margin: 0 0 15px 0;
  color: #999;
  font-size: 0.95rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rating-display {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 15px;
}

.star {
  font-size: 1.2rem;
  transition: all 0.2s ease;
}

.star.filled {
  transform: scale(1.1);
}

.rating-value {
  margin-left: 10px;
  color: #667eea;
  font-weight: 600;
  font-size: 1rem;
}

.comment-preview {
  background: #000;
  border-left: 3px solid #667eea;
  padding: 10px 15px;
  margin-bottom: 15px;
  border-radius: 4px;
}

.comment-text {
  margin: 0;
  color: #ccc;
  font-size: 0.9rem;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rating-date {
  color: #666;
  font-size: 0.85rem;
  text-align: right;
}

@media (max-width: 768px) {
  .ratings-grid {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 20px;
  }

  .header-content {
    grid-template-columns: auto 1fr;
    gap: 15px;
  }

  .title {
    font-size: 1.4rem;
    text-align: left;
  }

  .header-content > div:last-child {
    display: none;
  }
}
</style>
