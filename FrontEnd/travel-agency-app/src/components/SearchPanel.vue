<script setup>
import { ref } from 'vue'

const props = defineProps({
  searchParams: { type: Object, required: true },
  loading: { type: Boolean, default: false },
})

const emit = defineEmits(['search', 'clear'])

const validationError = ref('')

const today = new Date().toISOString().split('T')[0]

function handleTripTypeChange() {
  if (props.searchParams.tripType === 'ONEWAY') {
    props.searchParams.toDate = ''
  }
}

function handleSearch() {
  validationError.value = ''
  try {
    emit('search')
  } catch (e) {
    validationError.value = e.message
  }
}

function handleClear() {
  validationError.value = ''
  emit('clear')
}
</script>

<template>
  <div class="search-panel">
    <div class="search-panel__inner">
      <div class="search-panel__grid">
        <!-- Trip Type -->
        <div class="field field--trip-type">
          <label class="field__label">
            <span class="field__icon">🧭</span> Trip
          </label>
          <select
            v-model="searchParams.tripType"
            class="field__input"
            @change="handleTripTypeChange"
          >
            <option value="ROUNDTRIP">Round trip</option>
            <option value="ONEWAY">One-way</option>
          </select>
        </div>

        <!-- Origin -->
        <div class="field">
          <label class="field__label">
            <span class="field__icon">✈️</span> Origin
          </label>
          <input
            v-model="searchParams.origin"
            class="field__input"
            type="text"
            placeholder="e.g. New York (JFK)"
            autocomplete="off"
          />
        </div>

        <!-- Destination -->
        <div class="field">
          <label class="field__label">
            <span class="field__icon">📍</span> Destination
          </label>
          <input
            v-model="searchParams.destination"
            class="field__input"
            type="text"
            placeholder="e.g. Paris (CDG)"
            autocomplete="off"
          />
        </div>

        <!-- From Date -->
        <div class="field">
          <label class="field__label">
            <span class="field__icon">📅</span> Departure
          </label>
          <input
            v-model="searchParams.fromDate"
            class="field__input"
            type="date"
            :min="today"
          />
        </div>

        <!-- To Date -->
        <div class="field">
          <label class="field__label">
            <span class="field__icon">📅</span> Return
          </label>
          <input
            v-model="searchParams.toDate"
            class="field__input"
            type="date"
            :min="searchParams.fromDate || today"
            :disabled="searchParams.tripType === 'ONEWAY'"
          />
        </div>

        <!-- Adults -->
        <div class="field field--narrow">
          <label class="field__label">
            <span class="field__icon">👤</span> Adults
          </label>
          <input
            v-model.number="searchParams.adults"
            class="field__input"
            type="number"
            min="1"
            max="9"
          />
        </div>

        <!-- Children -->
        <div class="field field--narrow">
          <label class="field__label">
            <span class="field__icon">🧒</span> Children
          </label>
          <input
            v-model.number="searchParams.children"
            class="field__input"
            type="number"
            min="0"
            max="9"
          />
        </div>

        <!-- Search Button -->
        <div class="field field--action">
          <div class="action-buttons">
            <button
              class="btn-clear"
              :disabled="loading"
              @click="handleClear"
            >
              Clear
            </button>
            <button
              class="btn-search"
              :disabled="loading"
              @click="handleSearch"
            >
              <span v-if="loading" class="spinner" />
              <span v-else>🔍 Search</span>
            </button>
          </div>
        </div>
      </div>

      <p v-if="validationError" class="search-panel__error">{{ validationError }}</p>
    </div>
  </div>
</template>

<style scoped>
.search-panel {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  padding: 1.5rem 2rem;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.search-panel__inner {
  max-width: 1400px;
  margin: 0 auto;
}

.search-panel__grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: flex-end;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  flex: 1;
  min-width: 140px;
}

.field--narrow {
  flex: 0 0 90px;
  min-width: 80px;
}

.field--trip-type {
  flex: 0 0 140px;
}

.field--action {
  flex: 0 0 auto;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.field__label {
  font-size: 0.72rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  white-space: nowrap;
}

.field__icon {
  font-size: 0.85rem;
}

.field__input {
  height: 42px;
  padding: 0 0.75rem;
  border: 1.5px solid rgba(255, 255, 255, 0.3);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  font-size: 0.9rem;
  outline: none;
  transition: border-color 0.2s, background 0.2s;
  width: 100%;
  box-sizing: border-box;
}

.field__input::placeholder {
  color: rgba(255, 255, 255, 0.45);
}

.field__input:focus {
  border-color: rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.2);
}

.field__input::-webkit-calendar-picker-indicator {
  filter: invert(1) opacity(0.7);
  cursor: pointer;
}

.btn-search {
  height: 42px;
  padding: 0 1.5rem;
  background: var(--color-accent);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, transform 0.1s;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.btn-clear {
  height: 42px;
  padding: 0 1.2rem;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  border: 1.5px solid rgba(255, 255, 255, 0.35);
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s, transform 0.1s;
  white-space: nowrap;
}

.btn-clear:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.6);
  transform: translateY(-1px);
}

.btn-clear:active:not(:disabled) {
  transform: translateY(0);
}

.btn-clear:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-search:hover:not(:disabled) {
  background: var(--color-accent-dark);
  transform: translateY(-1px);
}

.btn-search:active:not(:disabled) {
  transform: translateY(0);
}

.btn-search:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  display: inline-block;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.search-panel__error {
  color: #ffcdd2;
  margin-top: 0.5rem;
  font-size: 0.85rem;
}
</style>
