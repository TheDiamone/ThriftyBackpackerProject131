<script setup>
defineProps({
  activities: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  error: { type: String, default: null },
})
</script>

<template>
  <div class="panel-content">
    <template v-if="loading">
      <div v-for="n in 5" :key="n" class="card skeleton" />
    </template>

    <div v-else-if="error" class="state-message state-message--error">
      <span class="state-icon">⚠️</span>
      <p>{{ error }}</p>
    </div>

    <div v-else-if="activities.length === 0" class="state-message">
      <span class="state-icon">🎯</span>
      <p>No activities found for this destination.</p>
    </div>

    <p v-else class="activities-hint">Browse activities available for this destination.</p>

    <div
      v-for="activity in activities"
      :key="activity.id"
      class="card activity-card"
    >
      <div class="activity-card__icon">{{ activity.icon }}</div>

      <div class="activity-card__body">
        <div class="activity-card__top">
          <div>
            <div class="activity-name">{{ activity.name }}</div>
            <div class="activity-meta">
              <span class="category-badge">{{ activity.category }}</span>
              <span class="meta-dot">·</span>
              <span class="meta-text">⏱ {{ activity.duration }}</span>
              <span class="meta-dot">·</span>
              <span class="meta-text">⭐ {{ activity.rating }} ({{ activity.reviews }})</span>
            </div>
            <div class="activity-desc">{{ activity.description }}</div>
          </div>
          <div class="activity-price-block">
            <span class="price">${{ activity.pricePerPerson }}</span>
            <span class="price-sub">/person</span>
            <div class="price-total">${{ activity.totalPrice.toLocaleString() }} total</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.panel-content {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.activities-hint {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  margin: 0;
  font-style: italic;
}

.card {
  background: #fff;
  border: 1.5px solid var(--color-border);
  border-radius: 12px;
  transition: border-color 0.2s, box-shadow 0.2s;
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem;
}

.card:hover {
  border-color: var(--color-primary-light);
  box-shadow: 0 4px 16px rgba(26, 54, 93, 0.1);
}

.skeleton {
  height: 90px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
  cursor: default;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.state-message {
  text-align: center;
  padding: 3rem 1rem;
  color: var(--color-text-muted);
}

.state-message--error { color: #c0392b; }

.state-icon {
  font-size: 2.5rem;
  display: block;
  margin-bottom: 0.5rem;
}

.activity-card__icon {
  font-size: 1.5rem;
  flex-shrink: 0;
}

.activity-card__body {
  flex: 1;
}

.activity-card__top {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
}

.activity-name {
  font-weight: 700;
  font-size: 0.95rem;
  color: var(--color-text);
  margin-bottom: 3px;
}

.activity-meta {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.category-badge {
  font-size: 0.68rem;
  background: #ede9fe;
  color: #5b21b6;
  padding: 2px 7px;
  border-radius: 20px;
  font-weight: 600;
}

.meta-dot {
  color: var(--color-border-dark);
  font-size: 0.75rem;
}

.meta-text {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.activity-desc {
  font-size: 0.78rem;
  color: var(--color-text-muted);
  line-height: 1.4;
}

.activity-price-block {
  text-align: right;
  flex-shrink: 0;
}

.price {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-primary);
}

.price-sub {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

.price-total {
  font-size: 0.78rem;
  color: var(--color-text);
  font-weight: 600;
  margin-top: 2px;
}
</style>
