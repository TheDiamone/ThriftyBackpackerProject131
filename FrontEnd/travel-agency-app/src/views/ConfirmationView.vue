<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="confirmation-view">
    <div class="confirmation-card">
      <div class="check-icon">✅</div>
      <h1>Booking Confirmed!</h1>
      <p>Your trip has been booked successfully. Check your email for full details.</p>
      <button class="btn-home" @click="router.push('/')">Plan Another Trip</button>
    </div>
  </div>
</template>

<style scoped>
.confirmation-view {
  min-height: calc(100vh - 56px);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
  padding: 2rem;
}

.confirmation-card {
  background: #fff;
  border-radius: 16px;
  padding: 3rem 2.5rem;
  text-align: center;
  max-width: 440px;
  box-shadow: 0 8px 32px rgba(26, 54, 93, 0.1);
}

.check-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

h1 {
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--color-primary-dark);
  margin: 0 0 0.75rem;
}

p {
  color: var(--color-text-muted);
  font-size: 0.95rem;
  line-height: 1.6;
  margin: 0 0 2rem;
}

.btn-home {
  padding: 0.75rem 2rem;
  background: var(--color-accent);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-home:hover {
  background: var(--color-accent-dark);
}
</style>
