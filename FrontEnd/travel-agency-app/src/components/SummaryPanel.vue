<script setup>
const props = defineProps({
  tripType: { type: String, default: 'ONEWAY' },
  selectedFlight: { type: Object, default: null },
  selectedReturnFlight: { type: Object, default: null },
  selectedHotel: { type: Object, default: null },
  selectedActivities: { type: Array, default: () => [] },
  totalPrice: { type: Number, default: 0 },
  hasSelections: { type: Boolean, default: false },
  isBooking: { type: Boolean, default: false },
  bookingResult: { type: Object, default: null },
  bookingError: { type: String, default: null },
  isAuthenticated: { type: Boolean, default: false },
  userEmail: { type: String, default: '' },
})

const emit = defineEmits(['book', 'clear'])
</script>

<template>
  <aside class="summary-panel">
    <div class="summary-panel__header">
      <div class="summary-panel__header-main">
        <h2 class="summary-title">
          <span>🧳</span> Your Trip
        </h2>
        <div v-if="isAuthenticated && userEmail" class="signed-in-indicator" :title="userEmail">
          Signed in as {{ userEmail }}
        </div>
      </div>
      <button
        v-if="hasSelections"
        class="btn-clear"
        @click="emit('clear')"
        title="Clear all selections"
      >
        ✕ Clear
      </button>
    </div>

    <!-- Empty state -->
    <div v-if="!hasSelections" class="empty-state">
      <div class="empty-icon">✈️</div>
      <p>Search for travel options and select your flight and hotel.</p>
    </div>

    <template v-else>
        <!-- Flight section -->
        <div v-if="selectedFlight" class="summary-section">
          <div class="section-label">
            <span class="section-icon">✈️</span> Outbound Flight
          </div>
          <div class="summary-item">
            <div class="item-title">{{ selectedFlight.airline }}</div>
            <div class="item-sub">{{ selectedFlight.flightNumber }}</div>
            <div class="item-sub">{{ selectedFlight.origin }} → {{ selectedFlight.destination }}</div>
            <div class="item-sub">{{ selectedFlight.departureTime }} – {{ selectedFlight.arrivalTime }} · {{ selectedFlight.duration }}</div>
            <div class="item-sub">{{ selectedFlight.class }} · {{ selectedFlight.stops === 0 ? 'Nonstop' : selectedFlight.stops + ' stop(s)' }}</div>
            <div class="item-price">${{ selectedFlight.totalPrice.toLocaleString() }}</div>
          </div>
        </div>
        <div v-else class="summary-section summary-section--empty">
          <div class="section-label">
            <span class="section-icon">✈️</span> Outbound Flight
          </div>
          <p class="not-selected">No flight selected</p>
        </div>

        <div v-if="tripType === 'ROUNDTRIP'" class="summary-divider" />

        <div v-if="tripType === 'ROUNDTRIP' && selectedReturnFlight" class="summary-section">
          <div class="section-label">
            <span class="section-icon">↩️</span> Return Flight
          </div>
          <div class="summary-item">
            <div class="item-title">{{ selectedReturnFlight.airline }}</div>
            <div class="item-sub">{{ selectedReturnFlight.flightNumber }}</div>
            <div class="item-sub">{{ selectedReturnFlight.origin }} → {{ selectedReturnFlight.destination }}</div>
            <div class="item-sub">{{ selectedReturnFlight.departureTime }} – {{ selectedReturnFlight.arrivalTime }} · {{ selectedReturnFlight.duration }}</div>
            <div class="item-sub">{{ selectedReturnFlight.class }} · {{ selectedReturnFlight.stops === 0 ? 'Nonstop' : selectedReturnFlight.stops + ' stop(s)' }}</div>
            <div class="item-price">${{ selectedReturnFlight.totalPrice.toLocaleString() }}</div>
          </div>
        </div>
        <div v-else-if="tripType === 'ROUNDTRIP'" class="summary-section summary-section--empty">
          <div class="section-label">
            <span class="section-icon">↩️</span> Return Flight
          </div>
          <p class="not-selected">No return flight selected</p>
        </div>

        <div class="summary-divider" />

        <!-- Hotel section -->
        <div v-if="selectedHotel" class="summary-section">
          <div class="section-label">
            <span class="section-icon">🏨</span> Hotel
          </div>
          <div class="summary-item">
            <div class="item-title">{{ selectedHotel.name }}</div>
            <div class="item-sub">{{ selectedHotel.location }}</div>
            <div class="item-sub">{{ selectedHotel.roomType }}</div>
            <div class="item-sub">{{ selectedHotel.nights }} night{{ selectedHotel.nights > 1 ? 's' : '' }} · ${{ selectedHotel.pricePerNight }}/night</div>
            <div class="item-price">${{ selectedHotel.totalPrice.toLocaleString() }}</div>
          </div>
        </div>
        <div v-else class="summary-section summary-section--empty">
          <div class="section-label">
            <span class="section-icon">🏨</span> Hotel
          </div>
          <p class="not-selected">No hotel selected</p>
        </div>

        <div class="summary-divider" />

        <!-- Activities section -->
        <div class="summary-section">
          <div class="section-label">
            <span class="section-icon">🎯</span> Activities
            <span v-if="selectedActivities.length" class="count-badge">{{ selectedActivities.length }}</span>
          </div>
          <div v-if="selectedActivities.length === 0" class="not-selected">No activities selected</div>
          <div
            v-else
            v-for="act in selectedActivities"
            :key="act.id"
            class="summary-item summary-item--activity"
          >
            <div class="activity-row">
              <span class="act-icon">{{ act.icon }}</span>
              <div class="act-info">
                <div class="item-title">{{ act.name }}</div>
                <div class="item-sub">{{ act.duration }}</div>
              </div>
              <div class="item-price">${{ act.totalPrice.toLocaleString() }}</div>
            </div>
          </div>
        </div>

        <!-- Total -->
        <div class="summary-total">
          <span class="total-label">Total</span>
          <span class="total-price">${{ totalPrice.toLocaleString() }}</span>
        </div>

        <!-- Booking error -->
        <div v-if="bookingError" class="booking-error">
          ⚠️ {{ bookingError }}
        </div>

        <!-- Book button -->
        <button
          class="btn-book"
          :disabled="isBooking || !!bookingResult"
          @click="emit('book')"
        >
          <span v-if="isBooking" class="spinner" />
          <span v-else>🎫 Book Now</span>
        </button>

        <!-- Inline booking confirmation -->
        <div v-if="bookingResult" class="booking-confirmation">
          <div class="booking-confirmation__icon">✅</div>
          <div class="booking-confirmation__body">
            <div class="booking-confirmation__title">Booking Confirmed!</div>
            <div class="booking-confirmation__ref">Ref: <strong>{{ bookingResult.bookingReference }}</strong></div>
            <div class="booking-confirmation__sub">A confirmation email has been sent to you.</div>
          </div>
        </div>
      </template>
  </aside>
</template>

<style scoped>
.summary-panel {
  background: #fff;
  border-left: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
}

.summary-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 1rem 1.25rem 0.75rem;
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  background: #fff;
  z-index: 1;
}

.summary-panel__header-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 0.35rem;
}

.summary-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.signed-in-indicator {
  max-width: 100%;
  font-size: 0.72rem;
  font-weight: 600;
  color: #0f5132;
  background: #e9f7ef;
  border: 1px solid #b7e4c7;
  border-radius: 999px;
  padding: 0.2rem 0.5rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.btn-clear {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: color 0.15s, background 0.15s;
}

.btn-clear:hover {
  color: #c0392b;
  background: #fce4ec;
}

/* Empty state */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem 1.5rem;
  text-align: center;
  color: var(--color-text-muted);
  gap: 0.75rem;
}

.empty-icon {
  font-size: 2.5rem;
  opacity: 0.4;
}

.empty-state p {
  margin: 0;
  font-size: 0.85rem;
  line-height: 1.6;
}

/* Section */
.summary-section {
  padding: 0.9rem 1.25rem;
}

.summary-section--empty {
  opacity: 0.5;
}

.section-label {
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-text-muted);
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

.section-icon {
  font-size: 0.85rem;
}

.count-badge {
  background: var(--color-primary);
  color: #fff;
  font-size: 0.65rem;
  padding: 1px 5px;
  border-radius: 10px;
  margin-left: 2px;
}

.summary-item {
  background: var(--color-primary-bg);
  border-radius: 8px;
  padding: 0.65rem 0.75rem;
  margin-bottom: 0.35rem;
  position: relative;
}

.summary-item--activity {
  padding: 0.5rem 0.75rem;
}

.activity-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.act-icon {
  font-size: 1.1rem;
  flex-shrink: 0;
}

.act-info {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-weight: 600;
  font-size: 0.85rem;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-sub {
  font-size: 0.72rem;
  color: var(--color-text-muted);
  margin-top: 1px;
}

.item-price {
  font-weight: 700;
  font-size: 0.9rem;
  color: var(--color-primary);
  text-align: right;
  white-space: nowrap;
  margin-top: 0.3rem;
}

.summary-item--activity .item-price {
  margin-top: 0;
}

.not-selected {
  font-size: 0.78rem;
  color: var(--color-text-muted);
  font-style: italic;
  margin: 0;
}

.summary-divider {
  height: 1px;
  background: var(--color-border);
  margin: 0 1.25rem;
}

/* Total */
.summary-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.25rem;
  border-top: 2px solid var(--color-primary);
  margin-top: 0.5rem;
}

.total-label {
  font-size: 0.85rem;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--color-text);
  letter-spacing: 0.04em;
}

.total-price {
  font-size: 1.4rem;
  font-weight: 800;
  color: var(--color-primary);
}

/* Book button */
.btn-book {
  margin: 0 1.25rem 1.25rem;
  width: calc(100% - 2.5rem);
  padding: 0.85rem;
  background: var(--color-accent);
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s, transform 0.1s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.btn-book:hover:not(:disabled) {
  background: var(--color-accent-dark);
  transform: translateY(-1px);
}

.btn-book:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.booking-error {
  margin: 0 1.25rem 0.75rem;
  padding: 0.5rem 0.75rem;
  background: #fce4ec;
  color: #c62828;
  border-radius: 8px;
  font-size: 0.8rem;
}

/* Inline booking confirmation banner */
.booking-confirmation {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  margin: 0 1.25rem 1.25rem;
  padding: 0.9rem 1rem;
  background: #f0fdf4;
  border: 1.5px solid #86efac;
  border-radius: 10px;
  animation: fadeSlideIn 0.35s ease;
}

@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}

.booking-confirmation__icon {
  font-size: 1.5rem;
  flex-shrink: 0;
  line-height: 1;
}

.booking-confirmation__body {
  flex: 1;
  min-width: 0;
}

.booking-confirmation__title {
  font-size: 0.9rem;
  font-weight: 700;
  color: #15803d;
  margin-bottom: 2px;
}

.booking-confirmation__ref {
  font-size: 0.8rem;
  color: #166534;
  word-break: break-all;
}

.booking-confirmation__sub {
  font-size: 0.72rem;
  color: #4ade80;
  color: #16a34a;
  margin-top: 3px;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  display: inline-block;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
