import { getFirestore, doc, getDoc, setDoc, updateDoc, serverTimestamp } from 'firebase/firestore'
import { app } from '../firebase'

const db = getFirestore(app)

/**
 * Estructura de Firestore:
 * users/{userId}/ratings = {
 *   albums: {
 *     {albumId}: {
 *       albumName, artistName, albumImage, rating, comment, updatedAt
 *     }
 *   }
 * }
 */

/**
 * Guarda o actualiza un rating de un álbum
 * @param {Object} ratingData - Datos del rating
 * @param {string} ratingData.userId - ID del usuario
 * @param {string} ratingData.albumId - ID del álbum en Spotify
 * @param {string} ratingData.albumName - Nombre del álbum
 * @param {string} ratingData.artistName - Nombre del artista
 * @param {string} ratingData.albumImage - URL de la imagen del álbum
 * @param {number} ratingData.rating - Calificación (1-5)
 * @param {string} ratingData.comment - Comentario opcional
 */
export async function saveRating(ratingData) {
  try {
    const { userId, albumId, albumName, artistName, albumImage, rating, comment = '' } = ratingData

    const userRatingsRef = doc(db, 'users', userId)
    
    // Obtener el documento actual del usuario
    const userDoc = await getDoc(userRatingsRef)
    
    const albumData = {
      albumName,
      artistName,
      albumImage,
      rating,
      comment,
      updatedAt: serverTimestamp()
    }

    if (userDoc.exists()) {
      // Actualizar el documento existente con el nuevo rating
      const currentAlbums = userDoc.data().albums || {}
      currentAlbums[albumId] = albumData
      
      await updateDoc(userRatingsRef, {
        albums: currentAlbums,
        lastUpdated: serverTimestamp()
      })
      console.log('Rating actualizado correctamente')
    } else {
      // Crear nuevo documento para el usuario
      await setDoc(userRatingsRef, {
        albums: {
          [albumId]: albumData
        },
        createdAt: serverTimestamp(),
        lastUpdated: serverTimestamp()
      })
      console.log('Rating guardado correctamente')
    }

    return { success: true }
  } catch (error) {
    console.error('Error al guardar rating:', error)
    throw error
  }
}

/**
 * Obtiene todos los ratings de un usuario
 * @param {string} userId - ID del usuario
 * @returns {Promise<Array>} Array de ratings
 */
export async function getUserRatings(userId) {
  try {
    const userRatingsRef = doc(db, 'users', userId)
    const userDoc = await getDoc(userRatingsRef)

    if (!userDoc.exists() || !userDoc.data().albums) {
      return []
    }

    const albums = userDoc.data().albums
    const ratings = []

    // Convertir el objeto de álbumes a un array
    for (const [albumId, albumData] of Object.entries(albums)) {
      ratings.push({
        albumId,
        ...albumData
      })
    }

    // Ordenar por fecha de actualización (más recientes primero)
    ratings.sort((a, b) => {
      const dateA = a.updatedAt?.toDate?.() || new Date(0)
      const dateB = b.updatedAt?.toDate?.() || new Date(0)
      return dateB - dateA
    })

    return ratings
  } catch (error) {
    console.error('Error al cargar ratings:', error)
    throw error
  }
}

/**
 * Obtiene el rating de un álbum específico para un usuario
 * @param {string} userId - ID del usuario
 * @param {string} albumId - ID del álbum en Spotify
 * @returns {Promise<Object|null>} Rating encontrado o null
 */
export async function getAlbumRating(userId, albumId) {
  try {
    const userRatingsRef = doc(db, 'users', userId)
    const userDoc = await getDoc(userRatingsRef)

    if (!userDoc.exists() || !userDoc.data().albums) {
      return null
    }

    const albumRating = userDoc.data().albums[albumId]
    
    if (albumRating) {
      return {
        albumId,
        ...albumRating
      }
    }

    return null
  } catch (error) {
    console.error('Error al cargar rating del álbum:', error)
    throw error
  }
}

/**
 * Calcula las estadísticas de ratings de un usuario
 * @param {string} userId - ID del usuario
 * @returns {Promise<Object>} Objeto con estadísticas
 */
export async function getUserStats(userId) {
  try {
    const ratings = await getUserRatings(userId)

    if (ratings.length === 0) {
      return {
        totalRatings: 0,
        averageRating: 0,
        uniqueArtists: 0
      }
    }

    // Calcular promedio de ratings
    const totalRating = ratings.reduce((sum, r) => sum + r.rating, 0)
    const averageRating = (totalRating / ratings.length).toFixed(1)

    // Contar artistas únicos
    const uniqueArtists = new Set(ratings.map(r => r.artistName)).size

    return {
      totalRatings: ratings.length,
      averageRating: parseFloat(averageRating),
      uniqueArtists
    }
  } catch (error) {
    console.error('Error al calcular estadísticas:', error)
    throw error
  }
}
