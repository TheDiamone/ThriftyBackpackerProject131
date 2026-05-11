import axios from 'axios'

// Destination-keyed hotel inventory.

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

// city label → array of hotel definitions
const HOTEL_INVENTORY = {
  'Paris, France': [
    { name: 'Hôtel Plaza Athénée', stars: 5, baseRate: 820, amenities: ['Free WiFi', 'Spa', 'Restaurant', 'Bar', 'Concierge', 'Room Service'], rating: '4.9', reviews: 3120, roomType: 'Deluxe Room', imageIndex: 0 },
    { name: 'Le Marais Boutique Hotel', stars: 4, baseRate: 340, amenities: ['Free WiFi', 'Bar', 'Concierge', 'Restaurant'], rating: '4.6', reviews: 1840, roomType: 'Standard Room', imageIndex: 1 },
    { name: 'Novotel Paris Centre', stars: 4, baseRate: 260, amenities: ['Free WiFi', 'Gym', 'Restaurant', 'Parking'], rating: '4.3', reviews: 2200, roomType: 'Standard Room', imageIndex: 2 },
    { name: 'Hôtel du Louvre', stars: 5, baseRate: 590, amenities: ['Free WiFi', 'Spa', 'Restaurant', 'Bar', 'Room Service', 'Concierge'], rating: '4.8', reviews: 980, roomType: 'Junior Suite', imageIndex: 3 },
    { name: 'Ibis Paris Montmartre', stars: 3, baseRate: 130, amenities: ['Free WiFi', 'Restaurant', 'Parking'], rating: '4.1', reviews: 3450, roomType: 'Standard Room', imageIndex: 4 },
  ],
  'London, UK': [
    { name: 'The Savoy', stars: 5, baseRate: 900, amenities: ['Free WiFi', 'Pool', 'Spa', 'Restaurant', 'Bar', 'Concierge'], rating: '4.9', reviews: 2780, roomType: 'Suite', imageIndex: 0 },
    { name: 'Premier Inn London City', stars: 3, baseRate: 160, amenities: ['Free WiFi', 'Restaurant', 'Bar'], rating: '4.2', reviews: 4100, roomType: 'Standard Room', imageIndex: 1 },
    { name: 'Marriott London Grosvenor Square', stars: 5, baseRate: 580, amenities: ['Free WiFi', 'Spa', 'Gym', 'Restaurant', 'Bar', 'Room Service'], rating: '4.7', reviews: 1620, roomType: 'Deluxe Room', imageIndex: 2 },
    { name: 'CitizenM Tower of London', stars: 4, baseRate: 270, amenities: ['Free WiFi', 'Bar', 'Gym'], rating: '4.5', reviews: 3300, roomType: 'Standard Room', imageIndex: 3 },
    { name: 'Hilton London Bankside', stars: 5, baseRate: 490, amenities: ['Free WiFi', 'Pool', 'Spa', 'Gym', 'Restaurant', 'Room Service'], rating: '4.6', reviews: 1950, roomType: 'Junior Suite', imageIndex: 4 },
  ],
  'Tokyo, Japan': [
    { name: 'Park Hyatt Tokyo', stars: 5, baseRate: 750, amenities: ['Free WiFi', 'Pool', 'Spa', 'Gym', 'Restaurant', 'Bar', 'Concierge'], rating: '4.9', reviews: 2100, roomType: 'Deluxe Room', imageIndex: 0 },
    { name: 'Shinjuku Granbell Hotel', stars: 4, baseRate: 280, amenities: ['Free WiFi', 'Bar', 'Concierge'], rating: '4.5', reviews: 3800, roomType: 'Standard Room', imageIndex: 1 },
    { name: 'Dormy Inn Akihabara', stars: 3, baseRate: 140, amenities: ['Free WiFi', 'Spa', 'Restaurant'], rating: '4.3', reviews: 5200, roomType: 'Standard Room', imageIndex: 2 },
    { name: 'The Prince Gallery Tokyo Kioicho', stars: 5, baseRate: 640, amenities: ['Free WiFi', 'Pool', 'Spa', 'Restaurant', 'Bar', 'Room Service'], rating: '4.8', reviews: 890, roomType: 'Suite', imageIndex: 3 },
    { name: 'APA Hotel Shinjuku Kabukicho', stars: 3, baseRate: 110, amenities: ['Free WiFi', 'Gym', 'Concierge'], rating: '4.0', reviews: 6700, roomType: 'Standard Room', imageIndex: 4 },
  ],
  'New York, USA': [
    { name: 'The Plaza Hotel', stars: 5, baseRate: 850, amenities: ['Free WiFi', 'Spa', 'Gym', 'Restaurant', 'Bar', 'Concierge', 'Room Service'], rating: '4.8', reviews: 1950, roomType: 'Junior Suite', imageIndex: 0 },
    { name: 'Pod 51 Hotel', stars: 3, baseRate: 190, amenities: ['Free WiFi', 'Bar', 'Concierge'], rating: '4.2', reviews: 4200, roomType: 'Standard Room', imageIndex: 1 },
    { name: 'Marriott Marquis Times Square', stars: 5, baseRate: 610, amenities: ['Free WiFi', 'Pool', 'Gym', 'Restaurant', 'Bar', 'Room Service'], rating: '4.6', reviews: 2300, roomType: 'Deluxe Room', imageIndex: 2 },
    { name: 'The Standard High Line', stars: 4, baseRate: 420, amenities: ['Free WiFi', 'Bar', 'Restaurant', 'Gym'], rating: '4.5', reviews: 1780, roomType: 'Deluxe Room', imageIndex: 3 },
    { name: 'Arlo NoMad', stars: 3, baseRate: 230, amenities: ['Free WiFi', 'Bar', 'Restaurant'], rating: '4.3', reviews: 3100, roomType: 'Standard Room', imageIndex: 4 },
  ],
  'Los Angeles, USA': [
    { name: 'Shutters on the Beach', stars: 5, baseRate: 780, amenities: ['Free WiFi', 'Pool', 'Spa', 'Restaurant', 'Bar', 'Concierge'], rating: '4.8', reviews: 1640, roomType: 'Suite', imageIndex: 0 },
    { name: 'The LINE LA', stars: 4, baseRate: 310, amenities: ['Free WiFi', 'Pool', 'Bar', 'Restaurant', 'Gym'], rating: '4.5', reviews: 2900, roomType: 'Deluxe Room', imageIndex: 1 },
    { name: 'Freehand Los Angeles', stars: 3, baseRate: 200, amenities: ['Free WiFi', 'Pool', 'Bar', 'Restaurant'], rating: '4.3', reviews: 3300, roomType: 'Standard Room', imageIndex: 2 },
    { name: 'Waldorf Astoria Beverly Hills', stars: 5, baseRate: 920, amenities: ['Free WiFi', 'Pool', 'Spa', 'Gym', 'Restaurant', 'Bar', 'Room Service'], rating: '4.9', reviews: 870, roomType: 'Suite', imageIndex: 3 },
    { name: 'Motel 6 Los Angeles', stars: 3, baseRate: 95, amenities: ['Free WiFi', 'Parking'], rating: '3.8', reviews: 5800, roomType: 'Standard Room', imageIndex: 4 },
  ],
  'Honolulu, USA': [
    { name: 'Royal Hawaiian Hotel', stars: 5, baseRate: 680, amenities: ['Free WiFi', 'Pool', 'Beach Access', 'Spa', 'Restaurant', 'Bar'], rating: '4.8', reviews: 2100, roomType: 'Deluxe Room', imageIndex: 0 },
    { name: 'Sheraton Waikiki', stars: 4, baseRate: 420, amenities: ['Free WiFi', 'Pool', 'Restaurant', 'Bar', 'Gym', 'Concierge'], rating: '4.5', reviews: 3800, roomType: 'Standard Room', imageIndex: 1 },
    { name: 'Outrigger Reef Waikiki Beach', stars: 4, baseRate: 370, amenities: ['Free WiFi', 'Pool', 'Beach Access', 'Restaurant', 'Bar'], rating: '4.4', reviews: 2950, roomType: 'Deluxe Room', imageIndex: 2 },
    { name: 'Hyatt Regency Waikiki', stars: 5, baseRate: 540, amenities: ['Free WiFi', 'Pool', 'Spa', 'Restaurant', 'Bar', 'Concierge'], rating: '4.7', reviews: 1700, roomType: 'Junior Suite', imageIndex: 3 },
    { name: 'Waikiki Beachcomber', stars: 3, baseRate: 230, amenities: ['Free WiFi', 'Pool', 'Restaurant'], rating: '4.1', reviews: 4200, roomType: 'Standard Room', imageIndex: 4 },
  ],
  'Dubai, UAE': [
    { name: 'Burj Al Arab', stars: 5, baseRate: 1800, amenities: ['Free WiFi', 'Pool', 'Spa', 'Beach Access', 'Restaurant', 'Bar', 'Concierge', 'Airport Shuttle'], rating: '5.0', reviews: 4200, roomType: 'Suite', imageIndex: 0 },
    { name: 'Atlantis The Palm', stars: 5, baseRate: 680, amenities: ['Free WiFi', 'Pool', 'Spa', 'Waterpark', 'Restaurant', 'Bar'], rating: '4.7', reviews: 6100, roomType: 'Deluxe Room', imageIndex: 1 },
    { name: 'Rove Downtown Dubai', stars: 3, baseRate: 150, amenities: ['Free WiFi', 'Pool', 'Gym', 'Restaurant'], rating: '4.4', reviews: 5300, roomType: 'Standard Room', imageIndex: 2 },
    { name: 'Address Downtown Dubai', stars: 5, baseRate: 490, amenities: ['Free WiFi', 'Pool', 'Spa', 'Gym', 'Restaurant', 'Bar', 'Room Service'], rating: '4.8', reviews: 2900, roomType: 'Junior Suite', imageIndex: 3 },
    { name: 'Premier Inn Dubai Airport', stars: 3, baseRate: 120, amenities: ['Free WiFi', 'Restaurant', 'Airport Shuttle'], rating: '4.1', reviews: 3700, roomType: 'Standard Room', imageIndex: 4 },
  ],
  'Singapore': [
    { name: 'Marina Bay Sands', stars: 5, baseRate: 620, amenities: ['Free WiFi', 'Pool', 'Spa', 'Gym', 'Casino', 'Restaurant', 'Bar', 'Concierge'], rating: '4.8', reviews: 8200, roomType: 'Deluxe Room', imageIndex: 0 },
    { name: 'The Fullerton Hotel Singapore', stars: 5, baseRate: 480, amenities: ['Free WiFi', 'Pool', 'Spa', 'Restaurant', 'Bar', 'Concierge'], rating: '4.9', reviews: 3100, roomType: 'Junior Suite', imageIndex: 1 },
    { name: 'ibis Singapore on Bencoolen', stars: 3, baseRate: 130, amenities: ['Free WiFi', 'Restaurant'], rating: '4.2', reviews: 6400, roomType: 'Standard Room', imageIndex: 2 },
    { name: 'Capella Singapore', stars: 5, baseRate: 820, amenities: ['Free WiFi', 'Pool', 'Spa', 'Gym', 'Restaurant', 'Bar', 'Room Service'], rating: '4.9', reviews: 1500, roomType: 'Suite', imageIndex: 3 },
    { name: 'Park Hotel Clarke Quay', stars: 4, baseRate: 280, amenities: ['Free WiFi', 'Pool', 'Bar', 'Restaurant', 'Concierge'], rating: '4.5', reviews: 2800, roomType: 'Deluxe Room', imageIndex: 4 },
  ],
  'Bangkok, Thailand': [
    { name: 'Mandarin Oriental Bangkok', stars: 5, baseRate: 550, amenities: ['Free WiFi', 'Pool', 'Spa', 'Restaurant', 'Bar', 'Concierge', 'River View'], rating: '4.9', reviews: 2600, roomType: 'Suite', imageIndex: 0 },
    { name: 'Centara Grand at CentralWorld', stars: 5, baseRate: 310, amenities: ['Free WiFi', 'Pool', 'Spa', 'Gym', 'Restaurant', 'Bar'], rating: '4.6', reviews: 4100, roomType: 'Deluxe Room', imageIndex: 1 },
    { name: 'Ibis Bangkok Siam', stars: 3, baseRate: 70, amenities: ['Free WiFi', 'Restaurant', 'Gym'], rating: '4.2', reviews: 7800, roomType: 'Standard Room', imageIndex: 2 },
    { name: 'SO Bangkok', stars: 5, baseRate: 390, amenities: ['Free WiFi', 'Pool', 'Spa', 'Restaurant', 'Bar', 'Concierge'], rating: '4.7', reviews: 1900, roomType: 'Junior Suite', imageIndex: 3 },
    { name: 'The Quarter Ladprao', stars: 4, baseRate: 130, amenities: ['Free WiFi', 'Pool', 'Restaurant', 'Gym'], rating: '4.4', reviews: 3200, roomType: 'Deluxe Room', imageIndex: 4 },
  ],
  'Sydney, Australia': [
    { name: 'Park Hyatt Sydney', stars: 5, baseRate: 720, amenities: ['Free WiFi', 'Pool', 'Spa', 'Restaurant', 'Bar', 'Concierge', 'Harbour View'], rating: '4.9', reviews: 1800, roomType: 'Suite', imageIndex: 0 },
    { name: 'Sofitel Sydney Darling Harbour', stars: 5, baseRate: 450, amenities: ['Free WiFi', 'Pool', 'Spa', 'Gym', 'Restaurant', 'Bar'], rating: '4.7', reviews: 2400, roomType: 'Deluxe Room', imageIndex: 1 },
    { name: 'Ibis Sydney Airport', stars: 3, baseRate: 145, amenities: ['Free WiFi', 'Restaurant', 'Airport Shuttle'], rating: '4.1', reviews: 5100, roomType: 'Standard Room', imageIndex: 2 },
    { name: 'QT Sydney', stars: 5, baseRate: 520, amenities: ['Free WiFi', 'Spa', 'Restaurant', 'Bar', 'Gym'], rating: '4.8', reviews: 1300, roomType: 'Junior Suite', imageIndex: 3 },
    { name: 'The Old Clare Hotel', stars: 4, baseRate: 290, amenities: ['Free WiFi', 'Pool', 'Bar', 'Restaurant'], rating: '4.5', reviews: 2700, roomType: 'Deluxe Room', imageIndex: 4 },
  ],
  'Seoul, South Korea': [
    { name: 'Lotte Hotel Seoul', stars: 5, baseRate: 380, amenities: ['Free WiFi', 'Pool', 'Spa', 'Gym', 'Restaurant', 'Bar', 'Concierge'], rating: '4.8', reviews: 2200, roomType: 'Deluxe Room', imageIndex: 0 },
    { name: 'Signiel Seoul', stars: 5, baseRate: 620, amenities: ['Free WiFi', 'Pool', 'Spa', 'Restaurant', 'Bar', 'Concierge', 'Room Service'], rating: '4.9', reviews: 1100, roomType: 'Suite', imageIndex: 1 },
    { name: 'Ibis Styles Ambassador Seoul', stars: 3, baseRate: 110, amenities: ['Free WiFi', 'Restaurant', 'Gym'], rating: '4.2', reviews: 5600, roomType: 'Standard Room', imageIndex: 2 },
    { name: 'L7 Myeongdong by Lotte', stars: 4, baseRate: 240, amenities: ['Free WiFi', 'Bar', 'Restaurant', 'Gym', 'Rooftop Bar'], rating: '4.5', reviews: 3800, roomType: 'Deluxe Room', imageIndex: 3 },
    { name: 'Novotel Suites Ambassador Seoul Yongsan', stars: 4, baseRate: 190, amenities: ['Free WiFi', 'Pool', 'Restaurant', 'Gym'], rating: '4.3', reviews: 2900, roomType: 'Standard Room', imageIndex: 4 },
  ],
  'Amsterdam, Netherlands': [
    { name: 'Waldorf Astoria Amsterdam', stars: 5, baseRate: 680, amenities: ['Free WiFi', 'Spa', 'Restaurant', 'Bar', 'Concierge', 'Canal View'], rating: '4.9', reviews: 1400, roomType: 'Suite', imageIndex: 0 },
    { name: 'Sofitel Legend The Grand', stars: 5, baseRate: 490, amenities: ['Free WiFi', 'Spa', 'Pool', 'Restaurant', 'Bar'], rating: '4.8', reviews: 1800, roomType: 'Junior Suite', imageIndex: 1 },
    { name: 'ibis Amsterdam Centre', stars: 3, baseRate: 150, amenities: ['Free WiFi', 'Bar', 'Restaurant'], rating: '4.2', reviews: 4700, roomType: 'Standard Room', imageIndex: 2 },
    { name: 'Andaz Amsterdam Prinsengracht', stars: 5, baseRate: 420, amenities: ['Free WiFi', 'Spa', 'Restaurant', 'Bar', 'Gym'], rating: '4.7', reviews: 2100, roomType: 'Deluxe Room', imageIndex: 3 },
    { name: 'INK Hotel Amsterdam', stars: 4, baseRate: 260, amenities: ['Free WiFi', 'Bar', 'Restaurant'], rating: '4.4', reviews: 3200, roomType: 'Deluxe Room', imageIndex: 4 },
  ],
  'Rome, Italy': [
    { name: 'Hotel de Russie', stars: 5, baseRate: 720, amenities: ['Free WiFi', 'Pool', 'Spa', 'Restaurant', 'Bar', 'Concierge'], rating: '4.9', reviews: 1600, roomType: 'Suite', imageIndex: 0 },
    { name: 'Rome Cavalieri, a Waldorf Astoria', stars: 5, baseRate: 550, amenities: ['Free WiFi', 'Pool', 'Spa', 'Gym', 'Restaurant', 'Bar', 'Room Service'], rating: '4.8', reviews: 1900, roomType: 'Deluxe Room', imageIndex: 1 },
    { name: 'Bettoja Mediterraneo Hotel', stars: 4, baseRate: 200, amenities: ['Free WiFi', 'Restaurant', 'Bar', 'Concierge'], rating: '4.3', reviews: 3400, roomType: 'Standard Room', imageIndex: 2 },
    { name: 'Hotel Artemide', stars: 4, baseRate: 260, amenities: ['Free WiFi', 'Spa', 'Restaurant', 'Bar'], rating: '4.5', reviews: 2800, roomType: 'Deluxe Room', imageIndex: 3 },
    { name: 'Generator Rome', stars: 3, baseRate: 120, amenities: ['Free WiFi', 'Bar', 'Concierge'], rating: '4.1', reviews: 5100, roomType: 'Standard Room', imageIndex: 4 },
  ],
}

function normalizeDestination(input) {
  const raw = String(input || '').trim().toUpperCase()
  if (!raw) return input
  // Support "City (IATA)" format
  const parenCodeMatch = raw.match(/\(([A-Z]{3})\)/)
  const code = parenCodeMatch ? parenCodeMatch[1] : (/^[A-Z]{3}$/.test(raw) ? raw : null)
  if (code && DESTINATION_ALIASES[code]) return DESTINATION_ALIASES[code]
  if (DESTINATION_ALIASES[raw]) return DESTINATION_ALIASES[raw]
  return input.trim()
}

function calcNights(fromDate, toDate) {
  const checkIn = new Date(fromDate)
  const checkOut = new Date(toDate)
  return Math.max(1, Math.round((checkOut - checkIn) / (1000 * 60 * 60 * 24)))
}

const HOTEL_API_BASE_URL = import.meta.env.VITE_HOTEL_API_BASE_URL?.replace(/\/$/, '')
const HOTEL_API_TIMEOUT_MS = Number(import.meta.env.VITE_HOTEL_API_TIMEOUT_MS) || 20000

const COUNTRY_ALIASES = {
  USA: 'United States',
  US: 'United States',
  UK: 'United Kingdom',
  UAE: 'United Arab Emirates',
  KOREA: 'South Korea',
}

function normalizeCountryName(value) {
  const country = String(value || '').trim()
  if (!country) return 'United States'

  const upper = country.toUpperCase()
  return COUNTRY_ALIASES[upper] || country
}

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

function getApiDestination(destination) {
  const normalizedDestination = normalizeDestination(destination)
  const [destName, countryLabel] = String(normalizedDestination || '').split(',').map((part) => part?.trim())

  return {
    dest_name: destName || destination,
    country_name: normalizeCountryName(countryLabel),
  }
}

function extractHotelItems(payload) {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.result)) return payload.result
  if (Array.isArray(payload?.results)) return payload.results
  if (Array.isArray(payload?.hotels)) return payload.hotels
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

function getHotelTotalPrice(hotel) {
  return getAmountValue(
    hotel.composite_price_breakdown?.gross_amount?.value
      ?? hotel.composite_price_breakdown?.all_inclusive_amount?.value
      ?? hotel.min_total_price
  )
}

function inferAmenities(hotel) {
  const amenities = []

  if (hotel.is_free_cancellable) amenities.push('Free cancellation')
  if (hotel.is_no_prepayment_block) amenities.push('Reserve now, pay later')
  if (hotel.hotel_include_breakfast || hotel.ribbon_text?.toLowerCase().includes('breakfast')) {
    amenities.push('Breakfast included')
  }
  if (hotel.accommodation_type_name) amenities.push(hotel.accommodation_type_name)
  if (hotel.district) amenities.push(hotel.district)

  return amenities.slice(0, 6)
}

function mapHotelFromApi(hotel, index, searchParams) {
  const nights = calcNights(searchParams.fromDate, searchParams.toDate)
  const normalizedDestination = normalizeDestination(searchParams.destination)
  const totalPrice = getHotelTotalPrice(hotel)
  const fallbackNightly = totalPrice ? Math.max(1, Math.round(totalPrice / nights)) : null
  const nightlyPrice = Math.round(
    getAmountValue(
      hotel.composite_price_breakdown?.gross_amount_per_night?.value
        ?? hotel.price_breakdown?.gross_price?.value
        ?? fallbackNightly
    ) || 0
  )

  return {
    id: String(hotel.hotel_id ?? hotel.id ?? `HT-${index}`),
    name: hotel.hotel_name || hotel.hotel_name_trans || 'Hotel',
    location: normalizedDestination,
    stars: Math.max(1, Math.min(5, Math.round(Number(hotel.class) || 3))),
    pricePerNight: nightlyPrice,
    totalPrice: Math.round(totalPrice ?? nightlyPrice * nights),
    nights,
    checkIn: searchParams.fromDate,
    checkOut: searchParams.toDate,
    amenities: inferAmenities(hotel),
    rating: Number(hotel.review_score || 0).toFixed(1),
    reviews: Number(hotel.review_nr || 0),
    roomType: hotel.urgency_room_msg || hotel.accommodation_type_name || 'Room',
    imageIndex: index % 5,
    imageUrl: hotel.max_photo_url || hotel.main_photo_url || null,
  }
}

async function searchHotelsViaApi(searchParams) {
  if (!HOTEL_API_BASE_URL) {
    throw new Error('VITE_HOTEL_API_BASE_URL is not configured.')
  }

  const { dest_name, country_name } = getApiDestination(searchParams.destination)
  let response

  try {
    response = await axios.get(`${HOTEL_API_BASE_URL}/hotels/search`, {
      params: {
        page_number: 0,
        dest_type: 'city',
        dest_name,
        country_name,
        units: 'metric',
        children_number: Number(searchParams.children) || 0,
        locale: 'en-gb',
        include_adjacency: true,
        filter_by_currency: 'AED',
        order_by: 'popularity',
        checkin_date: searchParams.fromDate,
        checkout_date: searchParams.toDate,
        room_number: 1,
        adults_number: Number(searchParams.adults) || 1,
      },
      headers: {
        accept: 'application/json',
      },
      timeout: HOTEL_API_TIMEOUT_MS,
    })
  } catch (error) {
    if (isTimeoutError(error)) {
      throw new Error('Hotel search timed out. Please try again or narrow your destination/dates.')
    }
    throw new Error(getApiErrorMessage(error, 'Hotel search failed.'))
  }

  return extractHotelItems(response.data)
    .map((hotel, index) => mapHotelFromApi(hotel, index, searchParams))
    .filter((hotel) => hotel.pricePerNight > 0)
    .sort((left, right) => left.pricePerNight - right.pricePerNight)
}

export const hotelService = {
  async search({ destination, fromDate, toDate, adults, children }) {
    const searchParams = { destination, fromDate, toDate, adults, children }

    try {
      const hotels = await searchHotelsViaApi(searchParams)
      if (hotels.length === 0) {
        throw new Error('No hotels returned from the hotel API.')
      }

      return hotels
    } catch (error) {
      if (isTimeoutError(error)) {
        throw new Error('Hotel search timed out. Please try again or narrow your destination/dates.')
      }
      throw error
    }
  },
}
