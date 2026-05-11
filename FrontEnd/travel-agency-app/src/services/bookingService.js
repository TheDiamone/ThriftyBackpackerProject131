import api from './api.js'

const BOOKING_USER_ID_STORAGE_KEY = 'booking_user_id'

function normalizeDate(value, fallback = '') {
  if (!value) return fallback
  const normalized = new Date(value)
  if (Number.isNaN(normalized.getTime())) return String(value)
  return normalized.toISOString().split('T')[0]
}

function normalizeTime(value, fallback = '00:00') {
  const raw = String(value || '').trim()
  if (!raw) return `${fallback}:00`

  const clockMatch = raw.match(/^(\d{1,2}):(\d{2})(?::(\d{2}))?$/)
  if (clockMatch) {
    const hours = String(Math.min(23, Math.max(0, Number(clockMatch[1])))).padStart(2, '0')
    const minutes = String(Math.min(59, Math.max(0, Number(clockMatch[2])))).padStart(2, '0')
    const seconds = String(Math.min(59, Math.max(0, Number(clockMatch[3] || 0)))).padStart(2, '0')
    return `${hours}:${minutes}:${seconds}`
  }

  return `${fallback}:00`
}

function splitFlightNumber(value) {
  const raw = String(value || '').trim().toUpperCase()
  const match = raw.match(/^([A-Z]{2,3})\s*-?(\d{1,4}[A-Z]?)$/)
  if (match) {
    return {
      airlineCode: match[1],
      flightNumber: match[2],
    }
  }

  return {
    airlineCode: '',
    flightNumber: raw,
  }
}

function normalizeAirportCode(value) {
  const code = String(value || '').trim().toUpperCase().replace(/[^A-Z]/g, '')
  return code.slice(0, 3)
}

function toFlightReservations(payload) {
  const flights = []

  if (payload.flight?.outbound) {
    flights.push(payload.flight.outbound)
  } else if (payload.flight) {
    flights.push(payload.flight)
  }

  if (payload.flight?.return) {
    flights.push(payload.flight.return)
  } else if (payload.returnFlight) {
    flights.push(payload.returnFlight)
  }

  return flights.map((flight) => {
    const split = splitFlightNumber(flight.flightNumber)
    const airlineCode = split.airlineCode || normalizeAirportCode(flight.airline).slice(0, 2)

    return {
      Airline_Code: airlineCode,
      Flight_Number: split.flightNumber,
      Departure_Date: normalizeDate(flight.date, normalizeDate(payload.searchParams?.fromDate)),
      Departure_Time: normalizeTime(flight.departureTime),
      Arrive_Date: normalizeDate(flight.date, normalizeDate(payload.searchParams?.fromDate)),
      Arrive_Time: normalizeTime(flight.arrivalTime),
      Rate: Number(flight.totalPrice ?? 0),
      Origin_Airport_Code: normalizeAirportCode(flight.origin || payload.searchParams?.origin),
      Destination_Airport_Code: normalizeAirportCode(flight.destination || payload.searchParams?.destination),
    }
  })
}

function toHotelReservations(payload) {
  if (!payload.hotel) return []

  const hotelCode = Number.parseInt(String(payload.hotel.id ?? ''), 10)

  return [
    {
      Hotel_Code: Number.isFinite(hotelCode) ? hotelCode : 0,
      Check_In_Date: normalizeDate(payload.hotel.checkIn, normalizeDate(payload.searchParams?.fromDate)),
      Check_In_Time: normalizeTime(payload.hotel.checkInTime),
      Check_Out_Date: normalizeDate(payload.hotel.checkOut, normalizeDate(payload.searchParams?.toDate || payload.searchParams?.fromDate)),
      Check_Out_Time: normalizeTime(payload.hotel.checkOutTime),
      Rate: Number(payload.hotel.totalPrice ?? 0),
    },
  ]
}

async function resolveUserId(payload) {
  const explicitUserId = Number(payload.userId)
  if (Number.isInteger(explicitUserId) && explicitUserId > 0) return explicitUserId

  const cachedUserId = Number(localStorage.getItem(BOOKING_USER_ID_STORAGE_KEY))
  if (Number.isInteger(cachedUserId) && cachedUserId > 0) return cachedUserId

  throw new Error('Please sign in first before booking.')
}

function buildBookingPayload(payload, userId) {
  const startDate = normalizeDate(payload.searchParams?.fromDate)
  const endDate = normalizeDate(payload.searchParams?.toDate || payload.searchParams?.fromDate)
  const agentId = Number(payload.agentId)

  return {
    User_Id: userId,
    Agent_Id: Number.isInteger(agentId) && agentId > 0 ? agentId : null,
    Start_Date: startDate,
    End_Date: endDate,
    hotel_reservations: toHotelReservations(payload),
    flight_reservations: toFlightReservations(payload),
  }
}

function normalizeBookingResponse(response) {
  const bookingReference = response?.bookingReference
    || response?.Booking_Reference
    || response?.reference
    || response?.Reference
    || response?.booking_id
    || response?.Booking_Id
    || response?.id
    || response?.Id

  return {
    success: true,
    bookingReference: bookingReference ? String(bookingReference) : '',
    message: response?.message || response?.Message || 'Your booking has been confirmed!',
    createdAt: response?.createdAt || response?.Created_At || new Date().toISOString(),
    ...response,
  }
}

function normalizeBookingRecord(record) {
  return {
    bookingId: Number(record?.Booking_Id ?? record?.booking_id ?? record?.id ?? 0) || 0,
    userId: Number(record?.User_Id ?? record?.userId ?? 0) || 0,
    agentId: Number(record?.Agent_Id ?? record?.agentId ?? 0) || 0,
    startDate: normalizeDate(record?.Start_Date ?? record?.startDate),
    endDate: normalizeDate(record?.End_Date ?? record?.endDate),
    user: record?.user || null,
    hotelReservations: Array.isArray(record?.hotel_reservations) ? record.hotel_reservations : [],
    flightReservations: Array.isArray(record?.flight_reservations) ? record.flight_reservations : [],
  }
}

export const bookingService = {
  async createBooking(payload) {
    // Validate required fields before sending
    if (!payload.flight && !payload.hotel) {
      throw new Error('A booking must include at least a flight or a hotel.')
    }

    const userId = await resolveUserId(payload)
    const requestBody = buildBookingPayload(payload, userId)
    const response = await api.post('/bookings/', requestBody)
    return normalizeBookingResponse(response)
  },

  async listBookings({ userId, agentId }) {
    const normalizedUserId = Number(userId)
    const normalizedAgentId = Number(agentId)

    if (!Number.isInteger(normalizedUserId) || normalizedUserId <= 0) {
      throw new Error('Please sign in first to view saved trips.')
    }

    const bookings = await api.get('/bookings/by-agent-user', {
      params: {
        agent_id: normalizedAgentId,
        user_id: normalizedUserId,
      },
    })

    const detailedBookings = (Array.isArray(bookings) ? bookings : []).map((booking) => {
      return normalizeBookingRecord(booking)
    })

    return detailedBookings.sort((left, right) => {
      return new Date(right.startDate).getTime() - new Date(left.startDate).getTime()
    })
  },
}
