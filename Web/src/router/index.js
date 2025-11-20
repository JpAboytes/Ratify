import { createRouter, createWebHistory } from 'vue-router'
import { getAuth } from 'firebase/auth'
import Login from '../views/Login.vue'
import Home from '../views/Home.vue'
import Callback from '../views/Callback.vue'
import MyRatings from '../views/MyRatings.vue'

const routes = [
  {
    path: '/',
    name: 'Login',
    component: Login,
    meta: { requiresGuest: true }
  },
  {
    path: '/home',
    name: 'Home',
    component: Home,
    meta: { requiresAuth: true }
  },
  {
    path: '/my-ratings',
    name: 'MyRatings',
    component: MyRatings,
    meta: { requiresAuth: true }
  },
  {
    path: '/callback',
    name: 'Callback',
    component: Callback
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Guard para proteger rutas
router.beforeEach((to, from, next) => {
  const auth = getAuth()
  const user = auth.currentUser

  if (to.meta.requiresAuth && !user) {
    next('/')
  } else if (to.meta.requiresGuest && user) {
    next('/home')
  } else {
    next()
  }
})

export default router
