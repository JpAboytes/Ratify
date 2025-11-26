import { getFirestore, doc, getDoc, setDoc, updateDoc, arrayUnion, arrayRemove, serverTimestamp } from 'firebase/firestore'
import { app } from '../firebase'

const db = getFirestore(app)


/**
 * Guarda o actualiza un rating de un álbum
 * @param {Object} ratingData - Datos del rating
 * @param {string} ratingData.userId - ID del usuario
 * @param {string} ratingData.userName - Nombre del usuario
 * @param {string} ratingData.albumId - ID del álbum en Spotify
 * @param {string} ratingData.albumName - Nombre del álbum
 * @param {string} ratingData.artistName - Nombre del artista
 * @param {string} ratingData.albumImage - URL de la imagen del álbum
 * @param {number} ratingData.rating - Calificación (1-5)
 * @param {string} ratingData.comment - Comentario opcional
 */
export async function saveRating(ratingData) {
  try {
    const { userId, userName, albumId, albumName, artistName, albumImage, rating, comment = '' } = ratingData

    // 1. Actualizar o crear documento del álbum en ratings
    const albumRatingRef = doc(db, 'ratings', albumId)
    const albumDoc = await getDoc(albumRatingRef)

    const reviewId = `${userId}_${albumId}`
    const now = new Date()
    const newReview = {
      userId,
      userName,
      rating,
      comment,
      reviewId,
      createdAt: now 
    }

    if (albumDoc.exists()) {
      const currentData = albumDoc.data()
      const reviews = currentData.reviews || []
      
      // Buscar si el usuario ya tiene un review
      const existingReviewIndex = reviews.findIndex(r => r.userId === userId)
      
      if (existingReviewIndex !== -1) {
        // Actualizar review existente, manteniendo la fecha de creación original si existe
        reviews[existingReviewIndex] = {
          ...newReview,
          createdAt: reviews[existingReviewIndex].createdAt || now
        }
      } else {
        // Agregar nuevo review
        reviews.push(newReview)
      }

      // Recalcular promedio
      const totalRating = reviews.reduce((sum, r) => sum + r.rating, 0)
      const averageRating = totalRating / reviews.length

      await updateDoc(albumRatingRef, {
        reviews,
        averageRating: parseFloat(averageRating.toFixed(1)),
        reviewCount: reviews.length,
        lastUpdated: serverTimestamp()
      })
    } else {
      // Crear nuevo documento para el álbum
      await setDoc(albumRatingRef, {
        albumName,
        artistName,
        albumImage,
        averageRating: rating,
        reviewCount: 1,
        reviews: [newReview],
        createdAt: serverTimestamp(),
        lastUpdated: serverTimestamp()
      })
    }

    // 2. Actualizar lista de álbumes calificados del usuario
    const userRef = doc(db, 'users', userId)
    const userDoc = await getDoc(userRef)

    if (userDoc.exists()) {
      await updateDoc(userRef, {
        Albums: arrayUnion(albumId),
        userName,
      })
    } else {
      await setDoc(userRef, {
        Albums: [albumId],
        userName
      })
    }

    console.log('Rating guardado correctamente')
    return { success: true }
  } catch (error) {
    console.error('Error al guardar rating:', error)
    throw error
  }
}

/**
 * Obtiene todos los ratings de un usuario
 * @param {string} userId - ID del usuario
 * @returns {Promise<Array>} Array de ratings con info completa del álbum
 */
export async function getUserRatings(userId) {
  try {
    const userRef = doc(db, 'users', userId)
    const userDoc = await getDoc(userRef)

    if (!userDoc.exists() || !userDoc.data().Albums) {
      return []
    }

    const ratedAlbumIds = userDoc.data().Albums
    const ratings = []

    // Obtener información completa de cada álbum calificado
    for (const albumId of ratedAlbumIds) {
      const albumRatingRef = doc(db, 'ratings', albumId)
      const albumDoc = await getDoc(albumRatingRef)

      if (albumDoc.exists()) {
        const albumData = albumDoc.data()
        
        // Encontrar el review específico de este usuario
        const userReview = albumData.reviews?.find(r => r.userId === userId)

        if (userReview) {
          ratings.push({
            albumId,
            albumName: albumData.albumName,
            artistName: albumData.artistName,
            albumImage: albumData.albumImage,
            rating: userReview.rating,
            comment: userReview.comment,
            updatedAt: userReview.createdAt,
            // Incluir también los datos globales del álbum
            averageRating: albumData.averageRating,
            reviewCount: albumData.reviewCount
          })
        }
      }
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
    const albumRatingRef = doc(db, 'ratings', albumId)
    const albumDoc = await getDoc(albumRatingRef)

    if (!albumDoc.exists() || !albumDoc.data().reviews) {
      return null
    }

    const reviews = albumDoc.data().reviews
    const userReview = reviews.find(r => r.userId === userId)
    
    if (userReview) {
      return {
        albumId,
        rating: userReview.rating,
        comment: userReview.comment,
        createdAt: userReview.createdAt
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

    // Calcular promedio de ratings del usuario
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

/**
 * Obtiene todos los reviews de un álbum (de todos los usuarios)
 * @param {string} albumId - ID del álbum en Spotify
 * @returns {Promise<Object|null>} Datos completos del álbum con todos sus reviews
 */
export async function getAlbumReviews(albumId) {
  try {
    const albumRatingRef = doc(db, 'ratings', albumId)
    const albumDoc = await getDoc(albumRatingRef)

    if (!albumDoc.exists()) {
      return null
    }

    return {
      albumId,
      ...albumDoc.data()
    }
  } catch (error) {
    console.error('Error al cargar reviews del álbum:', error)
    throw error
  }
}
