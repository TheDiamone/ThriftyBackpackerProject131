<script setup>
const props = defineProps({
  flights: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  error: { type: String, default: null },
  selectedFlight: { type: Object, default: null },
})

const emit = defineEmits(['select'])

function stopsLabel(stops) {
  if (stops === 0) return 'Nonstop'
  if (stops === 1) return '1 stop'
  return `${stops} stops`
}

function stopsClass(stops) {
  if (stops === 0) return 'badge--green'
  if (stops === 1) return 'badge--yellow'
  return 'badge--red'
}
</script>

<template>
  <div class="panel-content">
    <!-- Skeleton loading -->
    <template v-if="loading">
      <div v-for="n in 4" :key="n" class="card skeleton" />
    </template>

    <!-- Error state -->
    <div v-else-if="error" class="state-message state-message--error">
      <span class="state-icon">⚠️</span>
      <p>{{ error }}</p>
    </div>

    <!-- Empty state -->
    <div v-else-if="flights.length === 0" class="state-message">
      <span class="state-icon">✈️</span>
      <p>No flights found. Try adjusting your search.</p>
    </div>

    <!-- Flight cards -->
    <div
      v-else
      v-for="flight in flights"
      :key="flight.id"
      class="card flight-card"
      :class="{ 'card--selected': selectedFlight?.id === flight.id }"
      @click="emit('select', flight)"
    >
      <div class="flight-card__header">
        <div class="flight-card__airline">
          <span class="airline-logo">✈</span>
          <div>
            <div class="airline-name">{{ flight.airline }}</div>
            <div class="flight-number">{{ flight.flightNumber }} · {{ flight.aircraft }}</div>
          </div>
        </div>
        <div class="flight-card__price">
          <span class="price">${{ flight.totalPrice.toLocaleString() }}</span>
          <span class="price-sub">${{ flight.pricePerPerson }}/person</span>
        </div>
      </div>

      <div class="flight-card__route">
        <div class="route-point">
          <div class="route-time">{{ flight.departureTime }}</div>
          <div class="route-city">{{ flight.origin }}</div>
        </div>
        <div class="route-middle">
          <span :class="['badge', stopsClass(flight.stops)]">{{ stopsLabel(flight.stops) }}</span>
          <div class="route-line">
            <span class="route-dot" />
            <span class="route-track" />
            <span class="route-plane">✈</span>
          </div>
          <div class="route-duration">{{ flight.duration }}</div>
        </div>
        <div class="route-point route-point--right">
          <div class="route-time">{{ flight.arrivalTime }}</div>
          <div class="route-city">{{ flight.destination }}</div>
        </div>
      </div>

      <div class="flight-card__footer">
        <span class="tag">{{ flight.class }}</span>
        <span v-if="flight.seatsAvailable != null" class="tag">{{ flight.seatsAvailable }} seats left</span>
        <span v-else class="tag">Live availability</span>
        <span v-if="selectedFlight?.id === flight.id" class="selected-indicator">✓ Selected</span>
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
  padding: 1rem 1.25rem;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.1s;
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

.state-message--error {
  color: #c0392b;
}

.state-icon {
  font-size: 2.5rem;
  display: block;
  margin-bottom: 0.5rem;
}

/* Flight card internals */
.flight-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.75rem;
}

.flight-card__airline {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.airline-logo {
  width: 36px;
  height: 36px;
  background: var(--color-primary-bg);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  color: var(--color-primary);
}

.airline-name {
  font-weight: 600;
  font-size: 0.95rem;
  color: var(--color-text);
}

.flight-number {
  font-size: 0.78rem;
  color: var(--color-text-muted);
  margin-top: 2px;
}

.flight-card__price {
  text-align: right;
}

.price {
  display: block;
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--color-primary);
}

.price-sub {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.flight-card__route {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.route-point {
  min-width: 60px;
}

.route-point--right {
  text-align: right;
}

.route-time {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-text);
}

.route-city {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin-top: 1px;
}

.route-middle {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.route-line {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 2px;
}

.route-dot {
  width: 6px;
  height: 6px;
  background: var(--color-border-dark);
  border-radius: 50%;
  flex-shrink: 0;
}

.route-track {
  flex: 1;
  height: 1px;
  background: var(--color-border-dark);
}

.route-plane {
  font-size: 0.85rem;
  color: var(--color-primary);
  flex-shrink: 0;
}

.route-duration {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

.flight-card__footer {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
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

.badge {
  font-size: 0.68rem;
  font-weight: 600;
  padding: 2px 7px;
  border-radius: 20px;
}

.badge--green { background: #e8f5e9; color: #2e7d32; }
.badge--yellow { background: #fff8e1; color: #f57f17; }
.badge--red { background: #fce4ec; color: #c62828; }
</style>
