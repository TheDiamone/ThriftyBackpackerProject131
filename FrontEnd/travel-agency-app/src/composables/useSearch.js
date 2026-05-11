import { ref, reactive } from 'vue'
import { flightService } from '../services/flightService.js'
import { hotelService } from '../services/hotelService.js'
import { activityService } from '../services/activityService.js'

export function useSearch() {
  const initialSearchParams = {
    tripType: 'ROUNDTRIP',
    origin: '',
    destination: '',
    fromDate: '',
    toDate: '',
    adults: 1,
    children: 0,
  }

  const searchParams = reactive({
    ...initialSearchParams,
  })

  const results = reactive({
    flights: [],
    returnFlights: [],
    hotels: [],
    activities: [],
  })

  const loading = reactive({
    flights: false,
    returnFlights: false,
    hotels: false,
    activities: false,
  })

  const errors = reactive({
    flights: null,
    returnFlights: null,
    hotels: null,
    activities: null,
  })

  const hasSearched = ref(false)

  function validateParams() {
    const isRoundTrip = searchParams.tripType === 'ROUNDTRIP'

    if (!searchParams.origin.trim()) throw new Error('Origin is required.')
    if (!searchParams.destination.trim()) throw new Error('Destination is required.')
    if (!searchParams.fromDate) throw new Error('Departure date is required.')
    if (isRoundTrip && !searchParams.toDate) throw new Error('Return date is required for round trips.')
    if (
      isRoundTrip
      && new Date(searchParams.toDate) <= new Date(searchParams.fromDate)
    ) {
      throw new Error('Return date must be after departure date.')
    }
    if (searchParams.adults < 1) throw new Error('At least 1 adult is required.')
  }

  function addDays(dateString, days) {
    const date = new Date(dateString)
    if (Number.isNaN(date.getTime())) return dateString
    date.setDate(date.getDate() + days)
    return date.toISOString().split('T')[0]
  }

  function getEffectiveSearchParams() {
    const effective = { ...searchParams }

    if (effective.tripType === 'ONEWAY') {
      // Non-flight services still expect a toDate/check-out value.
      effective.toDate = effective.toDate || addDays(effective.fromDate, 1)
    }

    return effective
  }

  async function search() {
    validateParams()
    hasSearched.value = true
    const effectiveSearchParams = getEffectiveSearchParams()

    // Run all three searches concurrently
    const searchFlight = async () => {
      loading.flights = true
      loading.returnFlights = true
      errors.flights = null
      errors.returnFlights = null

      const isRoundTrip = effectiveSearchParams.tripType === 'ROUNDTRIP'
      try {
        const outboundParams = {
          ...effectiveSearchParams,
          tripType: 'ONEWAY',
          toDate: '',
        }

        const outboundSearch = flightService.search(outboundParams)
        const returnSearch = isRoundTrip
          ? flightService.search({
              ...effectiveSearchParams,
              tripType: 'ONEWAY',
              origin: effectiveSearchParams.destination,
              destination: effectiveSearchParams.origin,
              fromDate: effectiveSearchParams.toDate,
              toDate: '',
            })
          : Promise.resolve([])

        const [outboundResult, returnResult] = await Promise.allSettled([outboundSearch, returnSearch])

        if (outboundResult.status === 'fulfilled') {
          results.flights = outboundResult.value
        } else {
          errors.flights = outboundResult.reason?.message || 'Outbound flight search failed.'
          results.flights = []
        }

        if (isRoundTrip) {
          if (returnResult.status === 'fulfilled') {
            results.returnFlights = returnResult.value
          } else {
            errors.returnFlights = returnResult.reason?.message || 'Return flight search failed.'
            results.returnFlights = []
          }
        } else {
          results.returnFlights = []
        }
      } catch (e) {
        errors.flights = e.message
        results.flights = []
        results.returnFlights = []
        if (isRoundTrip) {
          errors.returnFlights = e.message
        }
      } finally {
        loading.flights = false
        loading.returnFlights = false
      }
    }

    const searchHotel = async () => {
      loading.hotels = true
      errors.hotels = null
      try {
        results.hotels = await hotelService.search(effectiveSearchParams)
      } catch (e) {
        errors.hotels = e.message
      } finally {
        loading.hotels = false
      }
    }

    const searchActivity = async () => {
      loading.activities = true
      errors.activities = null
      try {
        results.activities = await activityService.search(effectiveSearchParams)
      } catch (e) {
        errors.activities = e.message
      } finally {
        loading.activities = false
      }
    }

    await Promise.all([searchFlight(), searchHotel(), searchActivity()])
  }

  function resetSearch() {
    Object.assign(searchParams, initialSearchParams)
    results.flights = []
    results.returnFlights = []
    results.hotels = []
    results.activities = []
    errors.flights = null
    errors.returnFlights = null
    errors.hotels = null
    errors.activities = null
    loading.flights = false
    loading.returnFlights = false
    loading.hotels = false
    loading.activities = false
    hasSearched.value = false
  }

  return {
    searchParams,
    results,
    loading,
    errors,
    hasSearched,
    search,
    resetSearch,
  }
}
