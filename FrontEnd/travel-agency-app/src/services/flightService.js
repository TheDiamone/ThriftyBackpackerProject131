import axios from 'axios'

const AIRPORT_ALIASES = {
  'NEW YORK': 'JFK',
  'LOS ANGELES': 'LAX',
  'SAN FRANCISCO': 'SFO',
  TOKYO: 'NRT',
  HONOLULU: 'HNL',
  PARIS: 'CDG',
  LONDON: 'LHR',
  CHICAGO: 'ORD',
  MIAMI: 'MIA',
  SEATTLE: 'SEA',
  BOSTON: 'BOS',
  DUBAI: 'DXB',
  SINGAPORE: 'SIN',
  SYDNEY: 'SYD',
  SEOUL: 'ICN',
  BANGKOK: 'BKK',
  FRANKFURT: 'FRA',
  AMSTERDAM: 'AMS',
  TORONTO: 'YYZ',
  VANCOUVER: 'YVR',
  MADRID: 'MAD',
  ROME: 'FCO',
}

const FLIGHT_API_BASE_URL = import.meta.env.VITE_FLIGHT_API_BASE_URL?.replace(/\/$/, '')
const FLIGHT_TIMEOUT_MS = Number(import.meta.env.VITE_FLIGHT_TIMEOUT_MS || 30000)
const FLIGHT_RETRY_COUNT = Number(import.meta.env.VITE_FLIGHT_RETRY_COUNT || 1)

function isTimeoutError(error) {
  return error?.code === 'ECONNABORTED' || /timeout/i.test(String(error?.message || ''))
}

function parseStructuredDetail(detail) {
  if (!detail) return null

  if (Array.isArray(detail)) {
    return {
      message: detail.map((item) => item?.msg || 'invalid request').join('; '),
    }
  }

  if (typeof detail === 'object') {
    const code = detail?.error?.code || detail?.code || null
    const requestId = detail?.error?.requestId || detail?.requestId || null
    return {
      code,
      requestId,
      message: detail?.message || code || null,
    }
  }

  if (typeof detail === 'string') {
    const trimmed = detail.trim()

    try {
      return parseStructuredDetail(JSON.parse(trimmed))
    } catch {
      const codeMatch = trimmed.match(/'code':\s*'([^']+)'|"code":\s*"([^"]+)"/)
      const requestIdMatch = trimmed.match(/'requestId':\s*'([^']+)'|"requestId":\s*"([^"]+)"/)
      return {
        code: codeMatch?.[1] || codeMatch?.[2] || null,
        requestId: requestIdMatch?.[1] || requestIdMatch?.[2] || null,
        message: trimmed,
      }
    }
  }

  return null
}

function getFriendlyFlightError(detail) {
  const parsedDetail = parseStructuredDetail(detail)
  const errorCode = parsedDetail?.code

  if (errorCode === 'SEARCH_SEARCHFLIGHTS_INVALID_INPUT' || errorCode === 'BAD_REQUEST') {
    return 'Flight search input was rejected. Check the airport codes, dates, and passenger counts, and make sure origin and destination are different.'
  }

  if (errorCode === 'SEARCH_SEARCHFLIGHTS_NO_FLIGHTS_FOUND') {
    return 'No flights found for this route and date. Try different airports or dates.'
  }

  return parsedDetail?.message || null
}

function getApiErrorMessage(error, fallbackMessage) {
  const detail = error?.response?.data?.detail
  const detailMessage = getFriendlyFlightError(detail)

  return detailMessage || error?.response?.data?.message || error?.message || fallbackMessage
}

function normalizeAirport(input) {
  const raw = String(input || '').trim().toUpperCase()
  if (!raw) return ''

  const parenCodeMatch = raw.match(/\(([A-Z]{3})\)/)
  if (parenCodeMatch) return parenCodeMatch[1]
  if (/^[A-Z]{3}$/.test(raw)) return raw
  if (AIRPORT_ALIASES[raw]) return AIRPORT_ALIASES[raw]

  const textWithoutParens = raw.replace(/\([^)]*\)/g, ' ').replace(/[.,]/g, ' ')
  const normalizedText = textWithoutParens.replace(/\s+/g, ' ').trim()

  if (AIRPORT_ALIASES[normalizedText]) return AIRPORT_ALIASES[normalizedText]

  const iataToken = normalizedText.match(/\b([A-Z]{3})\b/)
  if (iataToken) return iataToken[1]

  return ''
}

function formatClockTime(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--:--'

  return date.toLocaleTimeString('en-GB', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
}

function formatDuration(totalSeconds) {
  const safeSeconds = Number(totalSeconds)
  if (!Number.isFinite(safeSeconds) || safeSeconds <= 0) return 'N/A'

  const totalMinutes = Math.round(safeSeconds / 60)
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60

  if (hours === 0) return `${minutes}m`
  if (minutes === 0) return `${hours}h`
  return `${hours}h ${minutes}m`
}

function formatCabinClass(value) {
  return String(value || 'ECONOMY')
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

function extractFlightOffers(payload) {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.flightOffers)) return payload.flightOffers
  if (Array.isArray(payload?.results)) return payload.results
  if (Array.isArray(payload?.result)) return payload.result
  if (Array.isArray(payload?.flights)) return payload.flights
  if (Array.isArray(payload?.data)) return payload.data
  return []
}

function getAmountValue(value) {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string') {
    const parsed = Number(value.replace(/[^\d.-]/g, ''))
    return Number.isFinite(parsed) ? parsed : null
  }
  return null
}

function getTotalPrice(offer) {
  return getAmountValue(
    offer.priceBreakdown?.total?.units
      ?? offer.unifiedPriceBreakdown?.price?.units
      ?? offer.unifiedPriceBreakdown?.price
  )
}

function getCarrierName(leg) {
  return leg.carriersData?.[0]?.name || leg.flightInfo?.carrierInfo?.marketingCarrier || 'Airline'
}

function getFlightNumber(leg) {
  const carrierCode = leg.carriersData?.[0]?.code || leg.flightInfo?.carrierInfo?.marketingCarrier || ''
  const flightNumber = leg.flightInfo?.flightNumber
  return flightNumber ? `${carrierCode}${flightNumber}` : carrierCode || 'N/A'
}

function mapOfferToFlight(offer, index, searchParams) {
  const segment = offer.segments?.[0]
  const firstLeg = segment?.legs?.[0]
  if (!segment || !firstLeg) return null

  const adultCount = Number(searchParams.adults) || 1
  const childCount = Number(searchParams.children) || 0
  const passengers = Math.max(1, adultCount + childCount)
  const totalPrice = getTotalPrice(offer)
  const stops = Math.max(0, (segment.legs?.length || 1) - 1)

  return {
    id: offer.token || `FL-${index}`,
    airline: getCarrierName(firstLeg),
    flightNumber: getFlightNumber(firstLeg),
    aircraft: firstLeg.flightInfo?.planeType || 'Aircraft',
    departureTime: formatClockTime(segment.departureTime),
    arrivalTime: formatClockTime(segment.arrivalTime),
    duration: formatDuration(segment.totalTime),
    stops,
    class: formatCabinClass(firstLeg.cabinClass),
    seatsAvailable: null,
    origin: segment.departureAirport?.code || normalizeAirport(searchParams.origin),
    destination: segment.arrivalAirport?.code || normalizeAirport(searchParams.destination),
    date: searchParams.fromDate,
    returnDate: searchParams.tripType === 'ROUNDTRIP' ? searchParams.toDate : null,
    adults: adultCount,
    children: childCount,
    pricePerPerson: totalPrice ? Math.round((totalPrice / passengers) * 100) / 100 : 0,
    totalPrice: totalPrice ? Math.round(totalPrice * 100) / 100 : 0,
  }
}

async function searchFlightsViaApi(searchParams) {
  if (!FLIGHT_API_BASE_URL) {
    throw new Error('VITE_FLIGHT_API_BASE_URL is not configured.')
  }

  const fromDate = searchParams.fromDate
  const toDate = searchParams.toDate
  const requestedTripType = String(searchParams.tripType || 'ROUNDTRIP').toUpperCase()
  const isRoundTrip = requestedTripType === 'ROUNDTRIP'
    && Boolean(fromDate && toDate && new Date(toDate) > new Date(fromDate))
  const adultCount = Number(searchParams.adults) || 1
  const childCount = Math.max(0, Number(searchParams.children) || 0)
  const normalizedOrigin = normalizeAirport(searchParams.origin)
  const normalizedDestination = normalizeAirport(searchParams.destination)

  if (!normalizedOrigin || !normalizedDestination) {
    throw new Error('Please enter valid origin and destination airport codes (e.g. JFK, LAX, CDG).')
  }

  if (normalizedOrigin === normalizedDestination) {
    throw new Error('Origin and destination must be different airports.')
  }

  const params = {
    depart_date: fromDate,
    from_code: `${normalizedOrigin}.AIRPORT`,
    to_code: `${normalizedDestination}.AIRPORT`,
    adults: adultCount,
    children: childCount,
    children_number: childCount,
    locale: 'en-gb',
    page_number: 0,
    currency: 'AED',
    order_by: 'BEST',
    flight_type: isRoundTrip ? 'ROUNDTRIP' : 'ONEWAY',
    cabin_class: 'ECONOMY',
  }

  if (isRoundTrip) {
    params.return_date = toDate
  }

  const maxAttempts = Math.max(1, Number.isFinite(FLIGHT_RETRY_COUNT) ? Math.floor(FLIGHT_RETRY_COUNT) + 1 : 2)
  const requestTimeoutMs = Number.isFinite(FLIGHT_TIMEOUT_MS) && FLIGHT_TIMEOUT_MS > 0
    ? Math.floor(FLIGHT_TIMEOUT_MS)
    : 30000

  let lastError = null

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    try {
      const response = await axios.get(`${FLIGHT_API_BASE_URL}/flights/search`, {
        params,
        headers: {
          accept: 'application/json',
        },
        timeout: requestTimeoutMs,
      })

      return extractFlightOffers(response.data)
        .map((offer, index) => mapOfferToFlight(offer, index, searchParams))
        .filter((flight) => flight && flight.totalPrice > 0)
        .sort((left, right) => left.totalPrice - right.totalPrice)
    } catch (error) {
      lastError = error
      const canRetry = attempt < maxAttempts && isTimeoutError(error)
      if (canRetry) continue

      if (isTimeoutError(error)) {
        throw new Error('Flight search timed out. Please try again in a moment.')
      }

      throw new Error(getApiErrorMessage(error, 'Flight search failed.'))
    }
  }

  if (isTimeoutError(lastError)) {
    throw new Error('Flight search timed out. Please try again in a moment.')
  }

  throw new Error(getApiErrorMessage(lastError, 'Flight search failed.'))
}

export const flightService = {
  async search({ origin, destination, fromDate, toDate, adults, children, tripType }) {
    const flights = await searchFlightsViaApi({
      origin,
      destination,
      fromDate,
      toDate,
      adults,
      children,
      tripType,
    })

    if (flights.length === 0) {
      throw new Error('No flights returned from the flight API.')
    }

    return flights
  },
}
