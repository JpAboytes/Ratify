<template>
  <div class="modal-overlay" @click="closeModal">
    <div class="modal-content" @click.stop>
      <button class="close-button" @click="closeModal">✕</button>
      
      <div class="album-header">
        <img 
          v-if="album.images && album.images[0]" 
          :src="album.images[0].url" 
          :alt="album.name"
          class="album-cover-large"
        />
        <div class="album-details">
          <h2 class="album-title">{{ album.name }}</h2>
          <p class="album-artist">
            {{ album.artists?.map(a => a.name).join(', ') }}
          </p>
          <p class="album-info">
            <span class="album-year">{{ album.release_date?.split('-')[0] }}</span>
            <span class="separator">•</span>
            <span class="album-tracks">{{ album.total_tracks }} canciones</span>
          </p>
          
          <!-- Rating Section -->
          <div class="rating-section">
            <h3>Califica este álbum</h3>
            <div class="stars-rating">
              <button
                v-for="star in 5"
                :key="star"
                @click="setRating(star)"
                @mouseenter="hoverRating = star"
                @mouseleave="hoverRating = 0"
                class="star-button"
                :class="{ 
                  active: star <= (hoverRating || userRating),
                  filled: star <= userRating 
                }"
              >
                {{ star <= (hoverRating || userRating) ? '⭐' : '☆' }}
              </button>
            </div>
            <p v-if="userRating" class="rating-text">
              Tu calificación: {{ userRating }} / 5
            </p>
          </div>

          <!-- Comment Section -->
          <div class="comment-section">
            <h3>Deja tu comentario</h3>
            <textarea 
              v-model="userComment"
              placeholder="Escribe tu opinión sobre este álbum..."
              class="comment-input"
              rows="4"
            ></textarea>
            <button 
              @click="saveComment"
              :disabled="!userComment.trim()"
              class="save-comment-button"
            >
              Guardar Comentario
            </button>
            <p v-if="commentSaved" class="comment-saved">✓ Comentario guardado</p>
          </div>
        </div>
      </div>

      <!-- Tracks List -->
      <div v-if="tracks.length > 0" class="tracks-section">
        <h3>Canciones</h3>
        <div class="tracks-list">
          <div 
            v-for="(track, index) in tracks" 
            :key="track.id"
            class="track-item"
          >
            <span class="track-number">{{ index + 1 }}</span>
            <div class="track-info">
              <p class="track-name">{{ track.name }}</p>
              <p class="track-artists">
                {{ track.artists?.map(a => a.name).join(', ') }}
              </p>
            </div>
            <span class="track-duration">
              {{ formatDuration(track.duration_ms) }}
            </span>
          </div>
        </div>
      </div>

      <!-- All Reviews Section -->
      <div v-if="albumReviews && albumReviews.reviews?.length > 0" class="all-reviews-section">
        <div class="reviews-header">
          <h3>Reseñas de la comunidad</h3>
          <div class="album-rating-summary">
            <span class="average-rating">{{ albumReviews.averageRating }}</span>
            <div class="rating-stars">
              <span v-for="star in 5" :key="star" class="star">
                {{ star <= Math.round(albumReviews.averageRating) ? '⭐' : '☆' }}
              </span>
            </div>
            <span class="review-count">{{ albumReviews.reviewCount }} {{ albumReviews.reviewCount === 1 ? 'reseña' : 'reseñas' }}</span>
          </div>
        </div>
        
        <div class="reviews-list">
          <div 
            v-for="review in albumReviews.reviews" 
            :key="review.reviewId"
            class="review-item"
            :class="{ 'own-review': review.userId === user?.uid }"
          >
            <div class="review-header">
              <span class="reviewer-name">{{ review.userName }}</span>
              <div class="review-rating">
                <span v-for="star in review.rating" :key="star" class="star-filled">⭐</span>
              </div>
            </div>
            <p v-if="review.comment" class="review-comment">{{ review.comment }}</p>
          </div>
        </div>
      </div>

      <div v-if="loading" class="loading-tracks">
        <div class="spinner"></div>
        <p>Cargando canciones...</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { spotifyApi } from '../services/spotify'
import { useAuth } from '../composables/useAuth'
import { saveRating, getAlbumRating, getAlbumReviews } from '../services/ratingService'

const props = defineProps({
  album: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['close', 'rate', 'comment'])

const { user } = useAuth()
const tracks = ref([])
const loading = ref(false)
const userRating = ref(0)
const hoverRating = ref(0)
const userComment = ref('')
const commentSaved = ref(false)
const saving = ref(false)
const albumReviews = ref(null)

onMounted(async () => {
  await loadAlbumDetails()
  await loadExistingRating()
  await loadAllReviews()
})

const loadAlbumDetails = async () => {
  loading.value = true
  try {
    const response = await spotifyApi.getAlbum(props.album.id)
    tracks.value = response.tracks.items
  } catch (error) {
    console.error('Error al cargar detalles del álbum:', error)
  } finally {
    loading.value = false
  }
}

const loadExistingRating = async () => {
  if (!user.value?.uid) return
  
  try {
    const existingRating = await getAlbumRating(user.value.uid, props.album.id)
    if (existingRating) {
      userRating.value = existingRating.rating
      userComment.value = existingRating.comment || ''
    }
  } catch (error) {
    console.error('Error al cargar rating existente:', error)
  }
}

const loadAllReviews = async () => {
  try {
    const reviews = await getAlbumReviews(props.album.id)
    albumReviews.value = reviews
  } catch (error) {
    console.error('Error al cargar reviews:', error)
  }
}

const setRating = async (rating) => {
  userRating.value = rating
  
  if (!user.value?.uid) return
  
  try {
    await saveRating({
      userId: user.value.uid,
      userName: user.value.displayName || user.value.email?.split('@')[0] || 'Usuario',
      albumId: props.album.id,
      albumName: props.album.name,
      artistName: props.album.artists?.map(a => a.name).join(', ') || 'Unknown',
      albumImage: props.album.images?.[0]?.url || '',
      rating,
      comment: userComment.value
    })
    emit('rate', { albumId: props.album.id, rating })
    await loadAllReviews() // Recargar reviews después de guardar
  } catch (error) {
    console.error('Error al guardar rating:', error)
  }
}

const saveComment = async () => {
  if (!userComment.value.trim() || !user.value?.uid) return
  
  saving.value = true
  try {
    await saveRating({
      userId: user.value.uid,
      userName: user.value.displayName || user.value.email?.split('@')[0] || 'Usuario',
      albumId: props.album.id,
      albumName: props.album.name,
      artistName: props.album.artists?.map(a => a.name).join(', ') || 'Unknown',
      albumImage: props.album.images?.[0]?.url || '',
      rating: userRating.value,
      comment: userComment.value
    })
    
    emit('comment', { 
      albumId: props.album.id, 
      comment: userComment.value,
      rating: userRating.value 
    })
    
    commentSaved.value = true
    setTimeout(() => {
      commentSaved.value = false
    }, 3000)
    
    await loadAllReviews() // Recargar reviews después de guardar
  } catch (error) {
    console.error('Error al guardar comentario:', error)
  } finally {
    saving.value = false
  }
}

const closeModal = () => {
  emit('close')
}

const formatDuration = (ms) => {
  const minutes = Math.floor(ms / 60000)
  const seconds = ((ms % 60000) / 1000).toFixed(0)
  return `${minutes}:${seconds.padStart(2, '0')}`
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
  overflow-y: auto;
}

.modal-content {
  background: #1a1a1a;
  border: 1px solid #333;
  border-radius: 16px;
  max-width: 900px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  position: relative;
  padding: 40px;
}

.close-button {
  position: absolute;
  top: 20px;
  right: 20px;
  background: transparent;
  border: none;
  color: #999;
  font-size: 2rem;
  cursor: pointer;
  transition: all 0.3s ease;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.close-button:hover {
  background: #333;
  color: #fff;
}

.album-header {
  display: flex;
  gap: 30px;
  margin-bottom: 40px;
}

.album-cover-large {
  width: 300px;
  height: 300px;
  border-radius: 12px;
  object-fit: cover;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}

.album-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.album-title {
  font-size: 2.5rem;
  color: #fff;
  margin: 0 0 10px;
}

.album-artist {
  font-size: 1.5rem;
  color: #999;
  margin: 0 0 15px;
}

.album-info {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #666;
  font-size: 1rem;
  margin-bottom: 30px;
}

.separator {
  color: #444;
}

/* Rating Section */
.rating-section {
  margin-top: 20px;
}

.rating-section h3 {
  color: #fff;
  font-size: 1.2rem;
  margin: 0 0 15px;
}

.stars-rating {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.star-button {
  background: transparent;
  border: none;
  font-size: 2.5rem;
  cursor: pointer;
  transition: all 0.2s ease;
  opacity: 0.3;
}

.star-button:hover,
.star-button.active {
  opacity: 1;
  transform: scale(1.2);
}

.star-button.filled {
  opacity: 1;
}

.rating-text {
  color: #667eea;
  font-weight: 600;
  font-size: 1rem;
}

/* Comment Section */
.comment-section {
  margin-top: 30px;
  padding-top: 30px;
  border-top: 1px solid #333;
}

.comment-section h3 {
  color: #fff;
  font-size: 1.2rem;
  margin: 0 0 15px;
}

.comment-input {
  width: 100%;
  background: #000;
  border: 1px solid #333;
  border-radius: 8px;
  padding: 12px;
  color: #fff;
  font-size: 1rem;
  font-family: inherit;
  resize: vertical;
  transition: all 0.3s ease;
}

.comment-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.comment-input::placeholder {
  color: #666;
}

.save-comment-button {
  margin-top: 15px;
  padding: 10px 24px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.save-comment-button:hover:not(:disabled) {
  background: #5568d3;
  transform: translateY(-2px);
}

.save-comment-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.comment-saved {
  margin-top: 10px;
  color: #1DB954;
  font-weight: 600;
}

/* Tracks Section */
.tracks-section {
  border-top: 1px solid #333;
  padding-top: 30px;
}

.tracks-section h3 {
  color: #fff;
  font-size: 1.5rem;
  margin: 0 0 20px;
}

.tracks-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.track-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.track-item:hover {
  background: #222;
}

.track-number {
  color: #666;
  font-size: 1rem;
  min-width: 30px;
  text-align: right;
}

.track-info {
  flex: 1;
}

.track-name {
  color: #fff;
  margin: 0 0 4px;
  font-weight: 500;
}

.track-artists {
  color: #999;
  margin: 0;
  font-size: 0.9rem;
}

.track-duration {
  color: #666;
  font-size: 0.9rem;
}

/* All Reviews Section */
.all-reviews-section {
  border-top: 1px solid #333;
  padding-top: 30px;
  margin-top: 30px;
}

.reviews-header {
  margin-bottom: 25px;
}

.reviews-header h3 {
  color: #fff;
  font-size: 1.5rem;
  margin: 0 0 15px;
}

.album-rating-summary {
  display: flex;
  align-items: center;
  gap: 15px;
  background: #222;
  padding: 15px 20px;
  border-radius: 8px;
  border-left: 4px solid #667eea;
}

.average-rating {
  font-size: 2.5rem;
  font-weight: bold;
  color: #667eea;
}

.rating-stars {
  display: flex;
  gap: 2px;
}

.rating-stars .star {
  font-size: 1.2rem;
}

.review-count {
  color: #999;
  font-size: 0.95rem;
  margin-left: auto;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.review-item {
  background: #1a1a1a;
  border: 1px solid #333;
  border-radius: 8px;
  padding: 20px;
  transition: all 0.3s ease;
}

.review-item:hover {
  border-color: #667eea;
  transform: translateX(5px);
}

.review-item.own-review {
  border-left: 4px solid #667eea;
  background: rgba(102, 126, 234, 0.05);
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.reviewer-name {
  color: #fff;
  font-weight: 600;
  font-size: 1rem;
}

.review-item.own-review .reviewer-name {
  color: #667eea;
}

.review-rating {
  display: flex;
  gap: 2px;
}

.star-filled {
  font-size: 1rem;
}

.review-comment {
  color: #ccc;
  margin: 0;
  line-height: 1.5;
  font-size: 0.95rem;
}

.loading-tracks {
  text-align: center;
  padding: 40px;
  color: #999;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #333;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 15px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Responsive */
@media (max-width: 768px) {
  .modal-content {
    padding: 30px 20px;
  }

  .album-header {
    flex-direction: column;
    gap: 20px;
  }

  .album-cover-large {
    width: 100%;
    height: auto;
    aspect-ratio: 1;
  }

  .album-title {
    font-size: 2rem;
  }

  .album-artist {
    font-size: 1.2rem;
  }

  .track-item {
    gap: 10px;
  }
}
</style>
