<template>
  <div class="callback-container">
    <div class="loading">
      <div class="spinner"></div>
      <h2>Conectando con Spotify...</h2>
      <p>Espera un momento mientras autenticamos tu cuenta</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

onMounted(() => {
  setTimeout(() => {
    router.push('/home')
  }, 1000)
})
</script>

<style scoped>
.callback-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000;
}

.loading {
  text-align: center;
  color: #fff;
}

.spinner {
  width: 60px;
  height: 60px;
  border: 4px solid #333;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 30px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading h2 {
  margin: 0 0 10px;
  font-size: 1.5rem;
}

.loading p {
  color: #999;
  font-size: 1rem;
}
</style>
