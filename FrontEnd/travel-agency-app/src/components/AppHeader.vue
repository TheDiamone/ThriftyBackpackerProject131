<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { tenantConfig } from '../config/tenantConfig.js'
import { useAuth } from '../composables/useAuth.js'

const router = useRouter()
const route = useRoute()
const {
  userId,
  userEmail,
  isAuthenticated,
  isAuthenticating,
  authError,
  signIn,
  signOut,
} = useAuth()

const isLoginModalOpen = ref(false)
const email = ref('')
const password = ref('')
const formError = ref('')
const isPasswordVisible = ref(false)

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function openSignInModal() {
  email.value = userEmail.value || ''
  password.value = ''
  formError.value = ''
  isPasswordVisible.value = false
  isLoginModalOpen.value = true
}

function closeSignInModal() {
  isLoginModalOpen.value = false
  password.value = ''
  formError.value = ''
  isPasswordVisible.value = false
}

async function handleSubmitSignIn() {
  const normalizedEmail = String(email.value || '').trim().toLowerCase()
  const rawPassword = String(password.value || '')

  if (!emailRegex.test(normalizedEmail)) {
    formError.value = 'Please enter a valid email address.'
    return
  }

  if (!rawPassword) {
    formError.value = 'Please enter your password.'
    return
  }

  formError.value = ''

  try {
    await signIn(normalizedEmail, rawPassword)
    closeSignInModal()
  } catch {
    // Error message is provided by authError
  }
}

function handleAuthButtonClick() {
  if (isAuthenticated.value) {
    signOut()
    if (route.name === 'trips') {
      router.push({ name: 'home' })
    }
    return
  }

  openSignInModal()
}

const navLinkMap = {
  trips: { routeName: 'trips' },
}

const navItems = computed(() => {
  return tenantConfig.navLabels.map((navItem) => {
    const navConfig = navLinkMap[navItem.key]
    const isTripsLink = navItem.key === 'trips'

    return {
      ...navItem,
      routeName: navConfig?.routeName || null,
      disabled: isTripsLink && !isAuthenticated.value,
      active: navConfig?.routeName ? route.name === navConfig.routeName : false,
    }
  })
})

function handleNavClick(navItem) {
  if (navItem.disabled || !navItem.routeName) return
  router.push({ name: navItem.routeName })
}
</script>

<template>
  <header class="app-header">
    <div class="app-header__inner">
      <div class="brand" @click="router.push('/')">
        <span class="brand-logo">{{ tenantConfig.brandLogo }}</span>
        <span class="brand-name">{{ tenantConfig.brandName }}</span>
      </div>
      <nav class="nav">
        <button
          v-for="navItem in navItems"
          :key="navItem.key"
          class="nav-link"
          :class="{ 'nav-link--active': navItem.active, 'nav-link--disabled': navItem.disabled }"
          :title="navItem.label"
          :disabled="navItem.disabled"
          type="button"
          @click="handleNavClick(navItem)"
        >
          <span class="nav-icon">{{ navItem.icon }}</span>
          <span class="nav-text">{{ navItem.label }}</span>
        </button>
      </nav>
      <div class="header-actions">
        <span class="tenant-badge">Agent {{ tenantConfig.agentId }}</span>
        <span v-if="isAuthenticated" class="user-badge" :title="userEmail || 'Signed in user'">
          User {{ userId }}
        </span>
        <button class="btn-signin" @click="handleAuthButtonClick">
          {{ isAuthenticated ? 'Sign Out' : 'Sign In' }}
        </button>
      </div>
    </div>

    <div v-if="isLoginModalOpen" class="auth-modal" @click.self="closeSignInModal">
      <form class="auth-modal__card" @submit.prevent="handleSubmitSignIn">
        <h2 class="auth-modal__title">Sign In</h2>
        <p class="auth-modal__sub">Enter your email and password to continue booking.</p>

        <div class="auth-modal__field">
          <label for="auth-email">Email</label>
          <input
            id="auth-email"
            v-model="email"
            type="email"
            autocomplete="email"
            placeholder="name@email.com"
          >
        </div>

        <div class="auth-modal__field">
          <label for="auth-password">Password</label>
          <div class="auth-modal__password-row">
            <input
              id="auth-password"
              v-model="password"
              :type="isPasswordVisible ? 'text' : 'password'"
              autocomplete="current-password"
              placeholder="Enter your password"
            >
            <button
              type="button"
              class="btn-password-toggle"
              :aria-label="isPasswordVisible ? 'Hide password' : 'Show password'"
              @click="isPasswordVisible = !isPasswordVisible"
            >
              <svg v-if="!isPasswordVisible" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                <line x1="1" y1="1" x2="23" y2="23"/>
              </svg>
            </button>
          </div>
        </div>

        <div v-if="formError || authError" class="auth-modal__error">
          {{ formError || authError }}
        </div>

        <div class="auth-modal__actions">
          <button type="button" class="btn-cancel" @click="closeSignInModal">Cancel</button>
          <button
            type="submit"
            class="btn-submit"
            :disabled="isAuthenticating"
          >
            {{ isAuthenticating ? 'Signing In...' : 'Sign In' }}
          </button>
        </div>
      </form>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  background: var(--color-primary-dark);
  color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  position: relative;
  z-index: 10;
}

.app-header__inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 2rem;
  height: 56px;
  display: flex;
  align-items: center;
  gap: 2rem;
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  flex-shrink: 0;
}

.brand-logo {
  font-size: 1.5rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.brand-name {
  font-size: 1.2rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: #fff;
}

.nav {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  flex: 1;
}

.nav-link {
  padding: 0.35rem 0.75rem;
  color: rgba(255, 255, 255, 0.75);
  font-size: 0.88rem;
  font-weight: 500;
  cursor: pointer;
  border-radius: 6px;
  transition: color 0.15s, background 0.15s;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 0.3rem;
  white-space: nowrap;
  background: transparent;
  border: none;
}

.nav-link:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}

.nav-link--active {
  color: #fff;
  background: rgba(255, 255, 255, 0.16);
}

.nav-link--disabled,
.nav-link:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.nav-link:disabled:hover {
  color: rgba(255, 255, 255, 0.75);
  background: transparent;
}

.nav-icon {
  font-size: 0.95rem;
}

.nav-text {
  font-weight: 500;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

.tenant-badge {
  padding: 0.25rem 0.6rem;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.12);
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.user-badge {
  padding: 0.25rem 0.6rem;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.2);
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-signin {
  padding: 0.35rem 1rem;
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-signin:hover {
  background: rgba(255, 255, 255, 0.25);
}

.auth-modal {
  position: fixed;
  inset: 0;
  background: rgba(8, 17, 31, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  z-index: 20;
}

.auth-modal__card {
  width: min(420px, 100%);
  background: #fff;
  border-radius: 12px;
  padding: 1rem;
  color: var(--color-text);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.25);
}

.auth-modal__title {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 700;
}

.auth-modal__sub {
  margin: 0.3rem 0 1rem;
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.auth-modal__field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  margin-bottom: 0.75rem;
}

.auth-modal__field label {
  font-size: 0.8rem;
  font-weight: 600;
}

.auth-modal__field input {
  border: 1px solid #cfd8e3;
  border-radius: 8px;
  padding: 0.55rem 0.65rem;
  font-size: 0.9rem;
  outline: none;
}

.auth-modal__password-row {
  position: relative;
  display: flex;
  align-items: center;
}

.auth-modal__password-row input {
  width: 100%;
  padding-right: 2.5rem;
}

.auth-modal__field input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

.btn-password-toggle {
  position: absolute;
  right: 0.5rem;
  border: none;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  line-height: 1;
}

.btn-password-toggle:hover {
  color: #475569;
}

.auth-modal__error {
  margin-bottom: 0.75rem;
  background: #fdecea;
  border: 1px solid #f5c2c0;
  color: #b42318;
  border-radius: 8px;
  padding: 0.55rem 0.65rem;
  font-size: 0.82rem;
}

.auth-modal__actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

.btn-cancel,
.btn-submit {
  border: none;
  border-radius: 8px;
  padding: 0.5rem 0.85rem;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-cancel {
  background: #eef2f7;
  color: var(--color-text);
}

.btn-submit {
  background: var(--color-primary);
  color: #fff;
}

.btn-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>
