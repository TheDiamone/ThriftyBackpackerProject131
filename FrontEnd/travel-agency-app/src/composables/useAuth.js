import { computed, ref } from 'vue'
import { authService } from '../services/authService.js'

export const AUTH_USER_ID_STORAGE_KEY = 'booking_user_id'
const AUTH_EMAIL_STORAGE_KEY = 'auth_email'

function parseStoredUserId(value) {
  const userId = Number(value)
  return Number.isInteger(userId) && userId > 0 ? userId : null
}

export function getStoredUserId() {
  return parseStoredUserId(localStorage.getItem(AUTH_USER_ID_STORAGE_KEY))
}

const userId = ref(getStoredUserId())
const userEmail = ref(localStorage.getItem(AUTH_EMAIL_STORAGE_KEY) || '')
const isAuthenticating = ref(false)
const authError = ref('')

export function useAuth() {
  const isAuthenticated = computed(() => Number.isInteger(userId.value) && userId.value > 0)

  async function signIn(email, password) {
    isAuthenticating.value = true
    authError.value = ''

    try {
      const result = await authService.login(email, password)
      userId.value = result.userId
      userEmail.value = String(email || '').trim()
      localStorage.setItem(AUTH_USER_ID_STORAGE_KEY, String(result.userId))
      localStorage.setItem(AUTH_EMAIL_STORAGE_KEY, userEmail.value)
      return result
    } catch (error) {
      authError.value = error.message || 'Sign in failed.'
      throw error
    } finally {
      isAuthenticating.value = false
    }
  }

  function signOut() {
    userId.value = null
    userEmail.value = ''
    authError.value = ''
    localStorage.removeItem(AUTH_USER_ID_STORAGE_KEY)
    localStorage.removeItem(AUTH_EMAIL_STORAGE_KEY)
  }

  return {
    userId,
    userEmail,
    isAuthenticated,
    isAuthenticating,
    authError,
    signIn,
    signOut,
  }
}