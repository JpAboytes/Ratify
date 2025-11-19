import { ref, onMounted } from 'vue'
import { 
  signInWithPopup, 
  GoogleAuthProvider,
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  signOut as firebaseSignOut,
  onAuthStateChanged 
} from 'firebase/auth'
import { auth } from '../firebase'

const user = ref(null)

export function useAuth() {
  const loading = ref(false)
  const error = ref(null)

  const signInWithGoogle = async () => {
    loading.value = true
    error.value = null
    try {
      const provider = new GoogleAuthProvider()
      const result = await signInWithPopup(auth, provider)
      user.value = result.user
      return result.user
    } catch (err) {
      error.value = err.message
      console.error('Error al iniciar sesión:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  const signUpWithEmail = async (email, password, username) => {
    loading.value = true
    error.value = null
    try {
      const result = await createUserWithEmailAndPassword(auth, email, password)
      user.value = result.user
      return result.user
    } catch (err) {
      error.value = err.message
      console.error('Error al registrarse:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  const signInWithEmail = async (email, password) => {
    loading.value = true
    error.value = null
    try {
      const result = await signInWithEmailAndPassword(auth, email, password)
      user.value = result.user
      return result.user
    } catch (err) {
      error.value = err.message
      console.error('Error al iniciar sesión:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  const signOut = async () => {
    loading.value = true
    error.value = null
    try {
      await firebaseSignOut(auth)
      user.value = null
    } catch (err) {
      error.value = err.message
      console.error('Error al cerrar sesión:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  const initAuth = () => {
    onAuthStateChanged(auth, (currentUser) => {
      user.value = currentUser
    })
  }

  return {
    user,
    loading,
    error,
    signInWithGoogle,
    signUpWithEmail,
    signInWithEmail,
    signOut,
    initAuth
  }
}
