import { ref, computed } from 'vue'
import { bookingService } from '../services/bookingService.js'
import { tenantConfig } from '../config/tenantConfig.js'
import { getStoredUserId } from './useAuth.js'

export function useBooking() {
  const selectedFlight = ref(null)
  const selectedReturnFlight = ref(null)
  const selectedHotel = ref(null)
  const selectedActivities = ref([])

  const isBooking = ref(false)
  const bookingResult = ref(null)
  const bookingError = ref(null)

  const totalPrice = computed(() => {
    let total = 0
    if (selectedFlight.value) total += selectedFlight.value.totalPrice
    if (selectedReturnFlight.value) total += selectedReturnFlight.value.totalPrice
    if (selectedHotel.value) total += selectedHotel.value.totalPrice
    selectedActivities.value.forEach((a) => (total += a.totalPrice))
    return total
  })

  const hasSelections = computed(
    () => Boolean(
      selectedFlight.value
      || selectedReturnFlight.value
      || selectedHotel.value
      || selectedActivities.value.length > 0
    )
  )

  function selectFlight(flight) {
    selectedFlight.value = selectedFlight.value?.id === flight.id ? null : flight
  }

  function selectReturnFlight(flight) {
    selectedReturnFlight.value = selectedReturnFlight.value?.id === flight.id ? null : flight
  }

  function selectHotel(hotel) {
    selectedHotel.value = selectedHotel.value?.id === hotel.id ? null : hotel
  }

  function toggleActivity(activity) {
    const idx = selectedActivities.value.findIndex((a) => a.id === activity.id)
    if (idx === -1) {
      selectedActivities.value.push(activity)
    } else {
      selectedActivities.value.splice(idx, 1)
    }
  }

  function isActivitySelected(activityId) {
    return selectedActivities.value.some((a) => a.id === activityId)
  }

  function clearSelections() {
    selectedFlight.value = null
    selectedReturnFlight.value = null
    selectedHotel.value = null
    selectedActivities.value = []
    bookingResult.value = null
    bookingError.value = null
  }

  async function book(searchParams) {
    if (!hasSelections.value) return

    const loggedInUserId = getStoredUserId()
    if (!loggedInUserId) {
      bookingError.value = 'Please sign in first before clicking Book Now.'
      return
    }

    if (searchParams.tripType === 'ROUNDTRIP' && (!selectedFlight.value || !selectedReturnFlight.value)) {
      bookingError.value = 'Please select both outbound and return flights for a round trip.'
      return
    }

    isBooking.value = true
    bookingError.value = null
    bookingResult.value = null

    try {
      const payload = {
        searchParams: { ...searchParams },
        userId: loggedInUserId,
        agentId: tenantConfig.agentId,
        flight: searchParams.tripType === 'ROUNDTRIP'
          ? {
              outbound: selectedFlight.value,
              return: selectedReturnFlight.value,
            }
          : selectedFlight.value,
        returnFlight: selectedReturnFlight.value,
        hotel: selectedHotel.value,
        activities: [...selectedActivities.value],
        totalPrice: totalPrice.value,
      }
      bookingResult.value = await bookingService.createBooking(payload)
    } catch (e) {
      bookingError.value = e.message
    } finally {
      isBooking.value = false
    }
  }

  return {
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
    toggleActivity,
    isActivitySelected,
    clearSelections,
    book,
  }
}
