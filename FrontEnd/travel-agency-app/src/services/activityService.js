import axios from 'axios'

const DESTINATION_ALIASES = {
  'NEW YORK': 'New York, USA',
  JFK: 'New York, USA',
  LGA: 'New York, USA',
  EWR: 'New York, USA',
  'LOS ANGELES': 'Los Angeles, USA',
  LAX: 'Los Angeles, USA',
  'SAN FRANCISCO': 'San Francisco, USA',
  SFO: 'San Francisco, USA',
  PARIS: 'Paris, France',
  CDG: 'Paris, France',
  ORY: 'Paris, France',
  LONDON: 'London, UK',
  LHR: 'London, UK',
  LGW: 'London, UK',
  TOKYO: 'Tokyo, Japan',
  NRT: 'Tokyo, Japan',
  HND: 'Tokyo, Japan',
  HONOLULU: 'Honolulu, USA',
  HNL: 'Honolulu, USA',
  CHICAGO: 'Chicago, USA',
  ORD: 'Chicago, USA',
  MDW: 'Chicago, USA',
  MIAMI: 'Miami, USA',
  MIA: 'Miami, USA',
  SEATTLE: 'Seattle, USA',
  SEA: 'Seattle, USA',
  BOSTON: 'Boston, USA',
  BOS: 'Boston, USA',
  DUBAI: 'Dubai, UAE',
  DXB: 'Dubai, UAE',
  SINGAPORE: 'Singapore',
  SIN: 'Singapore',
  SYDNEY: 'Sydney, Australia',
  SYD: 'Sydney, Australia',
  SEOUL: 'Seoul, South Korea',
  ICN: 'Seoul, South Korea',
  BANGKOK: 'Bangkok, Thailand',
  BKK: 'Bangkok, Thailand',
  FRANKFURT: 'Frankfurt, Germany',
  FRA: 'Frankfurt, Germany',
  AMSTERDAM: 'Amsterdam, Netherlands',
  AMS: 'Amsterdam, Netherlands',
  TORONTO: 'Toronto, Canada',
  YYZ: 'Toronto, Canada',
  VANCOUVER: 'Vancouver, Canada',
  YVR: 'Vancouver, Canada',
  MADRID: 'Madrid, Spain',
  MAD: 'Madrid, Spain',
  ROME: 'Rome, Italy',
  FCO: 'Rome, Italy',
}

const COUNTRY_ALIASES = {
  USA: 'United States',
  US: 'United States',
  UK: 'United Kingdom',
  UAE: 'United Arab Emirates',
  KOREA: 'South Korea',
}

const CATEGORY_ICONS = {
  culture: '🏛️',
  culinary: '🍷',
  food: '🍷',
  adventure: '🧗',
  relaxation: '🧘',
  nature: '🌿',
  nightlife: '🌃',
  shopping: '🛍️',
  sports: '⛷️',
  family: '👨‍👩‍👧‍👦',
  sightseeing: '📸',
}

const ACTIVITY_API_BASE_URL = import.meta.env.VITE_ACTIVITY_API_BASE_URL?.replace(/\/$/, '')
const ACTIVITY_API_TIMEOUT_MS = Number(import.meta.env.VITE_ACTIVITY_API_TIMEOUT_MS) || 20000

function getApiErrorMessage(error, fallbackMessage) {
  const detail = error?.response?.data?.detail
  const detailMessage = Array.isArray(detail)
    ? detail.map((item) => item?.msg || 'invalid request').join('; ')
    : typeof detail === 'string'
      ? detail
      : ''

  return detailMessage || error?.response?.data?.message || error?.message || fallbackMessage
}

function isTimeoutError(error) {
  return error?.code === 'ECONNABORTED' || String(error?.message || '').toLowerCase().includes('timeout')
}

function normalizeDestination(input) {
  const raw = String(input || '').trim().toUpperCase()
  if (!raw) return ''

  const parenCodeMatch = raw.match(/\(([A-Z]{3})\)/)
  const code = parenCodeMatch ? parenCodeMatch[1] : (/^[A-Z]{3}$/.test(raw) ? raw : null)

  if (code && DESTINATION_ALIASES[code]) return DESTINATION_ALIASES[code]
  if (DESTINATION_ALIASES[raw]) return DESTINATION_ALIASES[raw]
  return String(input || '').trim()
}

function normalizeCountryName(value) {
  const country = String(value || '').trim()
  if (!country) return 'United States'

  const upper = country.toUpperCase()
  return COUNTRY_ALIASES[upper] || country
}

function getApiDestination(destination) {
  const normalizedDestination = normalizeDestination(destination)
  const [cityName, countryLabel] = normalizedDestination.split(',').map((part) => part?.trim())

  return {
    dest_name: cityName || normalizedDestination || destination,
    country_name: normalizeCountryName(countryLabel),
    normalizedDestination,
  }
}

function extractAttractions(payload) {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.products)) return payload.products
  if (Array.isArray(payload?.result)) return payload.result
  if (Array.isArray(payload?.results)) return payload.results
  if (Array.isArray(payload?.attractions)) return payload.attractions
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.items)) return payload.items
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

function getPricePerPerson(item) {
  return getAmountValue(
    item.amount
      ?? item.price
      ?? item.price_per_person
      ?? item.ticket_price
      ?? item.min_price
      ?? item.retail_price
      ?? item.representativePrice?.publicAmount
      ?? item.representativePrice?.chargeAmount
      ?? item.pricing?.amount
      ?? item.pricing?.price
      ?? item.pricing?.value
      ?? item.priceBreakdown?.total?.units
      ?? item.unifiedPriceBreakdown?.price?.units
  )
}

function getCategory(item) {
  return item.category || item.category_name || item.type || item.attraction_type || 'Experience'
}

function getIcon(category) {
  return CATEGORY_ICONS[String(category || '').toLowerCase()] || '🎯'
}

function mapAttraction(item, index, searchParams, normalizedDestination) {
  const adultCount = Number(searchParams.adults) || 1
  const childCount = Math.max(0, Number(searchParams.children) || 0)
  const passengers = Math.max(1, adultCount + childCount)
  const category = getCategory(item)
  const pricePerPerson = Math.round((getPricePerPerson(item) || 0) * 100) / 100

  return {
    id: String(item.id ?? item.attraction_id ?? item.slug ?? `ACT-${index}`),
    name: item.name || item.title || item.attraction_name || 'Activity',
    category,
    icon: getIcon(category),
    location: normalizedDestination,
    duration: item.duration || item.duration_text || item.length || '2h',
    pricePerPerson,
    totalPrice: Math.round(pricePerPerson * passengers * 100) / 100,
    maxGroupSize: Number(item.max_group_size || 20),
    rating: Number(
      item.rating
        || item.review_score
        || item.numericReviewsStats?.average
        || item.reviewsStats?.combinedNumericStats?.average
        || 0
    ).toFixed(1),
    reviews: Number(
      item.reviews
        || item.review_nr
        || item.review_count
        || item.numericReviewsStats?.total
        || item.reviewsStats?.allReviewsCount
        || 0
    ),
    description: item.description || item.shortDescription || item.short_description || 'Enjoy a top-rated local experience.',
  }
}

async function searchActivitiesViaApi(searchParams) {
  if (!ACTIVITY_API_BASE_URL) {
    throw new Error('VITE_ACTIVITY_API_BASE_URL is not configured.')
  }

  const { dest_name, country_name, normalizedDestination } = getApiDestination(searchParams.destination)

  let response

  try {
    response = await axios.get(`${ACTIVITY_API_BASE_URL}/attractions/search`, {
      params: {
        start_date: searchParams.fromDate,
        end_date: searchParams.toDate,
        dest_name,
        country_name,
        locale: 'en-gb',
        page_number: 0,
        currency: 'AED',
        order_by: 'attr_book_score',
      },
      headers: {
        accept: 'application/json',
      },
      timeout: ACTIVITY_API_TIMEOUT_MS,
    })
  } catch (error) {
    if (isTimeoutError(error)) {
      throw new Error('Activity search timed out. Please try again.')
    }
    throw new Error(getApiErrorMessage(error, 'Activity search failed.'))
  }

  return extractAttractions(response.data)
    .map((item, index) => mapAttraction(item, index, searchParams, normalizedDestination))
    .filter((activity) => activity.pricePerPerson >= 0)
    .sort((left, right) => left.pricePerPerson - right.pricePerPerson)
}

export const activityService = {
  async search({ destination, fromDate, toDate, adults, children }) {
    const activities = await searchActivitiesViaApi({
      destination,
      fromDate,
      toDate,
      adults,
      children,
    })

    if (activities.length === 0) {
      throw new Error('No activities returned from the activity API.')
    }

    return activities
  },
}
