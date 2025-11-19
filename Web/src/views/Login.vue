<template>
  <div class="login-container">
    <div class="login-content">
      <!-- Logo -->
      <div class="logo">
        <h1> Ratify</h1>
        <p class="tagline">Califica tus álbumes favoritos de Spotify</p>
      </div>

      <div class="auth-form">
        <div class="tabs">
          <button 
            @click="isLogin = true" 
            :class="{ active: isLogin }"
            class="tab"
          >
            Iniciar Sesión
          </button>
          <button 
            @click="isLogin = false" 
            :class="{ active: !isLogin }"
            class="tab"
          >
            Registrarse
          </button>
        </div>

        <form @submit.prevent="handleSubmit" class="form">
          <div v-if="!isLogin" class="input-group">
            <label for="username">Nombre de Usuario</label>
            <input 
              type="text" 
              id="username"
              v-model="username"
              placeholder="Tu nombre de usuario"
              required
            />
          </div>

          <div class="input-group">
            <label for="email">Correo Electrónico</label>
            <input 
              type="email" 
              id="email"
              v-model="email"
              placeholder="tu@email.com"
              required
            />
          </div>

          <div class="input-group">
            <label for="password">Contraseña</label>
            <input 
              type="password" 
              id="password"
              v-model="password"
              placeholder="••••••••"
              required
              minlength="6"
            />
          </div>

          <button 
            type="submit" 
            :disabled="loading"
            class="submit-button"
          >
            <span v-if="loading">Cargando...</span>
            <span v-else>{{ isLogin ? 'Iniciar Sesión' : 'Crear Cuenta' }}</span>
          </button>
        </form>

        <div class="divider">
          <span>o continúa con</span>
        </div>

        <button 
          @click="handleGoogleLogin" 
          :disabled="loading"
          class="google-button"
        >
          <svg class="google-icon" viewBox="0 0 24 24">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
          </svg>
          <span>Google</span>
        </button>

        <p v-if="error" class="error-message">{{ error }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'

const router = useRouter()
const { signInWithGoogle, signUpWithEmail, signInWithEmail, loading, error } = useAuth()

const isLogin = ref(true)
const email = ref('')
const password = ref('')
const username = ref('')

const handleSubmit = async () => {
  try {
    if (isLogin.value) {
      await signInWithEmail(email.value, password.value)
    } else {
      await signUpWithEmail(email.value, password.value, username.value)
    }
    router.push('/home')
  } catch (err) {
    console.error('Error en autenticación:', err)
  }
}

const handleGoogleLogin = async () => {
  try {
    await signInWithGoogle()
    router.push('/home')
  } catch (err) {
    console.error('Error en login con Google:', err)
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000;
  padding: 20px;
}

.login-content {
  max-width: 450px;
  width: 100%;
}

.logo {
  text-align: center;
  margin-bottom: 40px;
}

.logo h1 {
  margin: 0;
  font-size: 3.5rem;
  font-weight: bold;
  color: #fff;
  text-shadow: 0 0 20px rgba(102, 126, 234, 0.5);
}

.tagline {
  margin: 10px 0 0;
  font-size: 1rem;
  color: #999;
}

/* Auth Form */
.auth-form {
  background: #1a1a1a;
  border: 1px solid #333;
  border-radius: 16px;
  padding: 40px;
}

/* Tabs */
.tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 30px;
}

.tab {
  flex: 1;
  padding: 12px;
  background: transparent;
  border: 1px solid #333;
  border-radius: 8px;
  color: #999;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tab:hover {
  border-color: #667eea;
  color: #667eea;
}

.tab.active {
  background: #667eea;
  border-color: #667eea;
  color: #fff;
}

/* Form */
.form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-group label {
  color: #fff;
  font-size: 0.9rem;
  font-weight: 600;
}

.input-group input {
  padding: 14px 16px;
  background: #000;
  border: 1px solid #333;
  border-radius: 8px;
  color: #fff;
  font-size: 1rem;
  transition: all 0.3s ease;
}

.input-group input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.input-group input::placeholder {
  color: #666;
}

/* Submit Button */
.submit-button {
  width: 100%;
  padding: 14px 24px;
  background: #667eea;
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 10px;
}

.submit-button:hover:not(:disabled) {
  background: #5568d3;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.submit-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Divider */
.divider {
  margin: 30px 0;
  text-align: center;
  position: relative;
}

.divider::before,
.divider::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 40%;
  height: 1px;
  background: #333;
}

.divider::before {
  left: 0;
}

.divider::after {
  right: 0;
}

.divider span {
  background: #1a1a1a;
  padding: 0 15px;
  color: #666;
  font-size: 0.9rem;
}

/* Google Button */
.google-button {
  width: 100%;
  padding: 14px 24px;
  background: #fff;
  border: 1px solid #333;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #000;
}

.google-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 255, 255, 0.1);
}

.google-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.google-icon {
  width: 20px;
  height: 20px;
}

/* Error Message */
.error-message {
  color: #ff6b6b;
  margin-top: 15px;
  padding: 12px;
  background: rgba(255, 107, 107, 0.1);
  border: 1px solid rgba(255, 107, 107, 0.3);
  border-radius: 8px;
  font-size: 0.9rem;
  text-align: center;
}

/* Responsive */
@media (max-width: 500px) {
  .auth-form {
    padding: 30px 20px;
  }
  
  .logo h1 {
    font-size: 2.5rem;
  }
}
</style>
