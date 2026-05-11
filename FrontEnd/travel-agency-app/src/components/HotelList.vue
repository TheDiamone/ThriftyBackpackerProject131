<script setup>
const props = defineProps({
  hotels: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  error: { type: String, default: null },
  selectedHotel: { type: Object, default: null },
})

const emit = defineEmits(['select'])

function renderStars(count) {
  return '★'.repeat(count) + '☆'.repeat(5 - count)
}

// Deterministic hotel color palettes
const palettes = [
  ['#1a365d', '#2c5282'],
  ['#276749', '#2f855a'],
  ['#744210', '#975a16'],
  ['#553c9a', '#6b46c1'],
  ['#2c7a7b', '#285e61'],
]
</script>

<template>
  <div class="panel-content">
    <template v-if="loading">
      <div v-for="n in 4" :key="n" class="card skeleton" />
    </template>

    <div v-else-if="error" class="state-message state-message--error">
      <span class="state-icon">⚠️</span>
      <p>{{ error }}</p>
    </div>

    <div v-else-if="hotels.length === 0" class="state-message">
      <span class="state-icon">🏨</span>
      <p>No hotels found. Try adjusting your dates.</p>
    </div>

    <div
      v-else
      v-for="(hotel, idx) in hotels"
      :key="hotel.id"
      class="card hotel-card"
      :class="{ 'card--selected': selectedHotel?.id === hotel.id }"
      @click="emit('select', hotel)"
    >
      <div
        class="hotel-card__image"
        :style="{ background: `linear-gradient(135deg, ${palettes[hotel.imageIndex][0]}, ${palettes[hotel.imageIndex][1]})` }"
      >
        <span class="hotel-icon">🏨</span>
      </div>

      <div class="hotel-card__body">
        <div class="hotel-card__top">
          <div>
            <div class="hotel-name">{{ hotel.name }}</div>
            <div class="hotel-location">📍 {{ hotel.location }}</div>
            <div class="hotel-stars">
              <span class="stars">{{ renderStars(hotel.stars) }}</span>
              <span class="rating-badge">{{ hotel.rating }} · {{ hotel.reviews }} reviews</span>
            </div>
          </div>
          <div class="hotel-price-block">
            <span class="price">${{ hotel.pricePerNight }}</span>
            <span class="price-sub">/ night</span>
            <div class="price-total">${{ hotel.totalPrice.toLocaleString() }} total</div>
            <div class="price-nights">{{ hotel.nights }} night{{ hotel.nights > 1 ? 's' : '' }}</div>
          </div>
        </div>

        <div class="hotel-card__amenities">
          <span v-for="amenity in hotel.amenities" :key="amenity" class="amenity-tag">
            {{ amenity }}
          </span>
        </div>

        <div class="hotel-card__footer">
          <span class="tag">{{ hotel.roomType }}</span>
          <span v-if="selectedHotel?.id === hotel.id" class="selected-indicator">✓ Selected</span>
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

.card {
  background: #fff;
  border: 1.5px solid var(--color-border);
  border-radius: 12px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.1s;
  overflow: hidden;
  display: flex;
}

.card:hover {
  border-color: var(--color-primary-light);
  box-shadow: 0 4px 16px rgba(26, 54, 93, 0.1);
  transform: translateY(-1px);
}

.card--selected {
  border-color: var(--color-primary) !important;
  box-shadow: 0 0 0 2px rgba(26, 54, 93, 0.15) !important;
  background: var(--color-primary-bg);
}

.skeleton {
  height: 110px;
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

.hotel-card__image {
  width: 90px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hotel-icon {
  font-size: 2rem;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.3));
}

.hotel-card__body {
  flex: 1;
  padding: 0.9rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.hotel-card__top {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
}

.hotel-name {
  font-weight: 700;
  font-size: 1rem;
  color: var(--color-text);
}

.hotel-location {
  font-size: 0.78rem;
  color: var(--color-text-muted);
  margin: 2px 0;
}

.hotel-stars {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.stars {
  color: #f6ad55;
  font-size: 0.78rem;
  letter-spacing: 1px;
}

.rating-badge {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

.hotel-price-block {
  text-align: right;
  flex-shrink: 0;
}

.price {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-primary);
}

.price-sub {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.price-total {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text);
  margin-top: 2px;
}

.price-nights {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

.hotel-card__amenities {
  display: flex;
  flex-wrap: wrap;
  gap: 0.3rem;
}

.amenity-tag {
  font-size: 0.68rem;
  background: #e8f4fd;
  color: #1a5276;
  padding: 2px 7px;
  border-radius: 20px;
}

.hotel-card__footer {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.tag {
  font-size: 0.72rem;
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  padding: 2px 8px;
  border-radius: 20px;
  font-weight: 500;
}

.selected-indicator {
  margin-left: auto;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-primary);
}
</style>
