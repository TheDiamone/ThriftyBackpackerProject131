import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import { getStoredUserId } from '../composables/useAuth.js'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView,
    meta: { title: 'Search Travel' },
  },
  {
    path: '/booking/confirmation',
    name: 'confirmation',
    component: () => import('../views/ConfirmationView.vue'),
    meta: { title: 'Booking Confirmed' },
  },
  {
    path: '/trips',
    name: 'trips',
    component: () => import('../views/MyTripsView.vue'),
    meta: { title: 'My Trips', requiresAuth: true },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to) => {
  if (to.meta?.requiresAuth && !getStoredUserId()) {
    return { name: 'home' }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} — TravelEase` : 'TravelEase'
})

export default router
