<script setup>
import { ref } from 'vue'
import SearchPanel from '../components/SearchPanel.vue'
import FlightList from '../components/FlightList.vue'
import HotelList from '../components/HotelList.vue'
import ActivityList from '../components/ActivityList.vue'
import SummaryPanel from '../components/SummaryPanel.vue'
import { useSearch } from '../composables/useSearch.js'
import { useBooking } from '../composables/useBooking.js'
import { useAuth } from '../composables/useAuth.js'

const { searchParams, results, loading, errors, hasSearched, search, resetSearch } = useSearch()
const {
  selectedFlight,
  selectedReturnFlight,
  selectedHotel,
  selectedActivities,
  totalPrice,
  hasSelections,
  isBooking,
  bookingResult,
  bookingError,
  selectFlight,
  selectReturnFlight,
  selectHotel,
  clearSelections,
  book,
} = useBooking()
const { isAuthenticated, userEmail } = useAuth()

const activeTab = ref('flights')

const tabs = [
  { key: 'flights', label: 'Flights', icon: '✈️' },
  { key: 'hotels', label: 'Hotels', icon: '🏨' },
  { key: 'activities', label: 'Activities', icon: '🎯' },
]

function handleSearch() {
  search()
  clearSelections()
}

function handleBook() {
  book(searchParams)
}

function handleClear() {
  resetSearch()
  clearSelections()
  activeTab.value = 'flights'
}
</script>

<template>
  <div class="home-view">
    <!-- Search Panel -->
    <SearchPanel
      :search-params="searchParams"
      :loading="loading.flights || loading.returnFlights || loading.hotels || loading.activities"
      @search="handleSearch"
      @clear="handleClear"
    />

    <!-- Main Content -->
    <div class="main-content" :class="{ 'main-content--searched': hasSearched }">
      <!-- Welcome / Hero -->
      <div v-if="!hasSearched" class="hero">
        <div class="hero__inner">
          <h1 class="hero__title">Plan Your Perfect Journey</h1>
          <p class="hero__sub">Search flights, hotels, and activities — book everything in one place.</p>
          <div class="hero__features">
            <div v-for="feat in [
              { icon: '✈️', label: 'Flights' },
              { icon: '🏨', label: 'Hotels' },
              { icon: '🎯', label: 'Activities' },
              { icon: '🎫', label: 'All-in-One Booking' },
            ]" :key="feat.label" class="hero__feature">
              <span class="hero__feature-icon">{{ feat.icon }}</span>
              <span>{{ feat.label }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Results layout -->
      <div v-else class="results-layout">
        <!-- Left: Tabbed panels -->
        <div class="results-main">
          <!-- Tabs -->
          <div class="tabs">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              class="tab-btn"
              :class="{ 'tab-btn--active': activeTab === tab.key }"
              @click="activeTab = tab.key"
            >
              <span class="tab-icon">{{ tab.icon }}</span>
              {{ tab.label }}
              <span
                v-if="tab.key === 'flights' && (loading.flights || results.flights.length)"
                class="tab-count"
              >
                {{ loading.flights ? '…' : (searchParams.tripType === 'ROUNDTRIP' ? results.flights.length + results.returnFlights.length : results.flights.length) }}
              </span>
              <span
                v-if="tab.key === 'hotels' && (loading.hotels || results.hotels.length)"
                class="tab-count"
              >
                {{ loading.hotels ? '…' : results.hotels.length }}
              </span>
              <span
                v-if="tab.key === 'activities' && (loading.activities || results.activities.length)"
                class="tab-count"
              >
                {{ loading.activities ? '…' : results.activities.length }}
              </span>
            </button>
          </div>

          <!-- Tab panels -->
          <div class="tab-panel">
            <div v-show="activeTab === 'flights'" class="flight-panels">
              <div class="flight-panels__section">
                <h3 class="flight-panels__title">Outbound flight</h3>
                <FlightList
                  :flights="results.flights"
                  :loading="loading.flights"
                  :error="errors.flights"
                  :selected-flight="selectedFlight"
                  @select="selectFlight"
                />
              </div>

              <div v-if="searchParams.tripType === 'ROUNDTRIP'" class="flight-panels__section">
                <h3 class="flight-panels__title">Return flight</h3>
                <FlightList
                  :flights="results.returnFlights"
                  :loading="loading.returnFlights"
                  :error="errors.returnFlights"
                  :selected-flight="selectedReturnFlight"
                  @select="selectReturnFlight"
                />
              </div>
            </div>
            <HotelList
              v-show="activeTab === 'hotels'"
              :hotels="results.hotels"
              :loading="loading.hotels"
              :error="errors.hotels"
              :selected-hotel="selectedHotel"
              @select="selectHotel"
            />
            <ActivityList
              v-show="activeTab === 'activities'"
              :activities="results.activities"
              :loading="loading.activities"
              :error="errors.activities"
            />
          </div>
        </div>

        <!-- Right: Summary panel -->
        <SummaryPanel
          :trip-type="searchParams.tripType"
          :selected-flight="selectedFlight"
          :selected-return-flight="selectedReturnFlight"
          :selected-hotel="selectedHotel"
          :selected-activities="selectedActivities"
          :total-price="totalPrice"
          :has-selections="hasSelections"
          :is-booking="isBooking"
          :booking-result="bookingResult"
          :booking-error="bookingError"
          :is-authenticated="isAuthenticated"
          :user-email="userEmail"
          @book="handleBook"
          @clear="clearSelections"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-view {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 56px);
  background: var(--color-bg);
}

/* Hero */
.hero {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  background: var(--hero-gradient);
}

.hero__inner {
  text-align: center;
  max-width: 600px;
}

.hero__title {
  font-size: 2.5rem;
  font-weight: 800;
  color: var(--color-primary-dark);
  margin: 0 0 0.75rem;
  line-height: 1.15;
}

.hero__sub {
  font-size: 1.05rem;
  color: var(--color-text-muted);
  margin: 0 0 2.5rem;
  line-height: 1.6;
}

.hero__features {
  display: flex;
  justify-content: center;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.hero__feature {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.4rem;
  background: #fff;
  padding: 1rem 1.25rem;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(26, 54, 93, 0.08);
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text);
  min-width: 90px;
}

.hero__feature-icon {
  font-size: 1.8rem;
}

/* Results */
.main-content {
  flex: 1;
}

.main-content--searched {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.results-layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  height: calc(100vh - 56px - 90px); /* viewport - header - search panel */
  overflow: hidden;
}

.results-main {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-right: 1px solid var(--color-border);
}

/* Tabs */
.tabs {
  display: flex;
  background: #fff;
  border-bottom: 1px solid var(--color-border);
  padding: 0 1rem;
  gap: 0.25rem;
  flex-shrink: 0;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.75rem 1rem;
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--color-text-muted);
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s;
  margin-bottom: -1px;
  white-space: nowrap;
}

.tab-btn:hover {
  color: var(--color-text);
}

.tab-btn--active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.tab-icon {
  font-size: 1rem;
}

.tab-count {
  background: var(--color-primary-bg);
  color: var(--color-primary);
  font-size: 0.7rem;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 12px;
}

.tab-btn--active .tab-count {
  background: var(--color-primary);
  color: #fff;
}

/* Tab panel */
.tab-panel {
  flex: 1;
  overflow-y: auto;
  background: var(--color-bg);
}

.flight-panels {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.flight-panels__section {
  background: #fff;
  border-bottom: 1px solid var(--color-border);
}

.flight-panels__title {
  margin: 0;
  padding: 0.85rem 1rem;
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--color-text);
  border-bottom: 1px solid var(--color-border);
  background: #fafcff;
}
</style>
