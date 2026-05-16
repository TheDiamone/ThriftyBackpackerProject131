package com.thriftybackpacker.service;

import com.thriftybackpacker.dto.booking.*;
import com.thriftybackpacker.dto.user.UserResponse;
import com.thriftybackpacker.exception.ResourceNotFoundException;
import com.thriftybackpacker.model.*;
import com.thriftybackpacker.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Booking business logic — mirrors the Python booking.py endpoint logic.
 *
 * Python equivalents:
 *   read_bookings(skip, limit)           → db.query(Booking).offset(skip).limit(limit).all()
 *   read_booking(id)                     → db.query(Booking).filter(...).first()
 *   create_booking(booking)              → db.add(Booking) + db.add(HotelReservation/FlightReservation)
 *   update_booking(id, update)           → setattr(db_booking, key, value)
 *   delete_booking(id)                   → db.delete(db_booking)
 *   update_hotel_reservation(...)        → partial hotel reservation update
 *   delete_hotel_reservation(...)        → db.delete(hotel_res)
 *   update_flight_reservation(...)       → partial flight reservation update
 *   delete_flight_reservation(...)       → db.delete(flight_res)
 *
 * Note: The Python booking.py validates Hotel_Code against Hotel_Master, Airline_Code against
 * Airline_Master, and airport codes against Airport_Master tables. Those master tables are not
 * seeded in the Python project either, so this Java backend skips those validations (same effective behaviour).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepo;
    private final UserRepository userRepo;
    private final HotelReservationRepository hotelResRepo;
    private final FlightReservationRepository flightResRepo;

    // ── List ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BookingResponse> listBookings(int skip, int limit) {
        return bookingRepo.findAll()
                .stream()
                .skip(skip)
                .limit(limit)
                .map(this::toBookingResponse)
                .collect(Collectors.toList());
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BookingDetailResponse getBooking(Integer bookingId) {
        Booking b = findBookingOrThrow(bookingId);
        return toDetailResponse(b);
    }

    // ── Get by agent + user (NEW — frontend calls GET /bookings/by-agent-user) ─

    @Transactional(readOnly = true)
    public List<BookingDetailResponse> getBookingsByAgentUser(Integer userId, Integer agentId) {
        List<Booking> bookings = (agentId != null)
                ? bookingRepo.findByUserIdAndAgentId(userId, agentId)
                : bookingRepo.findByUserId(userId);

        return bookings.stream()
                .map(this::toDetailResponse)
                .collect(Collectors.toList());
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public BookingDetailResponse createBooking(BookingCreateRequest req) {
        // Verify user exists (matches Python behaviour)
        if (!userRepo.existsById(req.getUserId())) {
            throw new IllegalArgumentException(
                    "Cannot create booking. User_Id " + req.getUserId() + " does not exist.");
        }

        Booking booking = new Booking();
        booking.setUserId(req.getUserId());
        booking.setAgentId(req.getAgentId());
        booking.setStartDate(req.getStartDate());
        booking.setEndDate(req.getEndDate());
        booking = bookingRepo.save(booking);

        // Persist hotel reservations
        for (HotelReservationCreate h : req.getHotelReservations()) {
            HotelReservation hr = new HotelReservation();
            hr.setBookingId(booking.getBookingId());
            hr.setHotelCode(h.getHotelCode());
            hr.setHotelName(h.getHotelName());
            hr.setCheckInDate(h.getCheckInDate());
            hr.setCheckInTime(h.getCheckInTime());
            hr.setCheckOutDate(h.getCheckOutDate());
            hr.setCheckOutTime(h.getCheckOutTime());
            hr.setRate(h.getRate());
            hotelResRepo.save(hr);
        }

        // Persist flight reservations
        for (FlightReservationCreate f : req.getFlightReservations()) {
            FlightReservation fr = new FlightReservation();
            fr.setBookingId(booking.getBookingId());
            fr.setAirlineCode(f.getAirlineCode());
            fr.setFlightNumber(f.getFlightNumber());
            fr.setDepartureDate(f.getDepartureDate());
            fr.setDepartureTime(f.getDepartureTime());
            fr.setArriveDate(f.getArriveDate());
            fr.setArriveTime(f.getArriveTime());
            fr.setRate(f.getRate());
            fr.setOriginAirportCode(f.getOriginAirportCode());
            fr.setDestinationAirportCode(f.getDestinationAirportCode());
            flightResRepo.save(fr);
        }

        Booking saved = findBookingOrThrow(booking.getBookingId());
        return toDetailResponse(saved);
    }

    // ── Update booking ────────────────────────────────────────────────────────

    @Transactional
    public BookingResponse updateBooking(Integer bookingId, BookingUpdate update) {
        Booking b = findBookingOrThrow(bookingId);
        if (update.getStartDate() != null) b.setStartDate(update.getStartDate());
        if (update.getEndDate() != null) b.setEndDate(update.getEndDate());
        if (update.getAgentId() != null) b.setAgentId(update.getAgentId());
        return toBookingResponse(bookingRepo.save(b));
    }

    // ── Delete booking ────────────────────────────────────────────────────────

    @Transactional
    public void deleteBooking(Integer bookingId) {
        Booking b = findBookingOrThrow(bookingId);
        bookingRepo.delete(b);
    }

    // ── Update hotel reservation ──────────────────────────────────────────────

    @Transactional
    public HotelReservationResponse updateHotelReservation(
            Integer bookingId, Integer reservationNo, HotelReservationUpdate update) {

        HotelReservation hr = hotelResRepo
                .findByReservationNoAndBookingId(reservationNo, bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hotel reservation " + reservationNo + " not found for booking " + bookingId));

        if (update.getHotelCode() != null) hr.setHotelCode(update.getHotelCode());
        if (update.getCheckInDate() != null) hr.setCheckInDate(update.getCheckInDate());
        if (update.getCheckInTime() != null) hr.setCheckInTime(update.getCheckInTime());
        if (update.getCheckOutDate() != null) hr.setCheckOutDate(update.getCheckOutDate());
        if (update.getCheckOutTime() != null) hr.setCheckOutTime(update.getCheckOutTime());
        if (update.getRate() != null) hr.setRate(update.getRate());

        return toHotelResResponse(hotelResRepo.save(hr));
    }

    // ── Delete hotel reservation ──────────────────────────────────────────────

    @Transactional
    public void deleteHotelReservation(Integer bookingId, Integer reservationNo) {
        HotelReservation hr = hotelResRepo
                .findByReservationNoAndBookingId(reservationNo, bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hotel reservation " + reservationNo + " not found for booking " + bookingId));
        hotelResRepo.delete(hr);
    }

    // ── Update flight reservation ─────────────────────────────────────────────

    @Transactional
    public FlightReservationResponse updateFlightReservation(
            Integer bookingId, Integer reservationNo, FlightReservationUpdate update) {

        FlightReservation fr = flightResRepo
                .findByReservationNoAndBookingId(reservationNo, bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Flight reservation " + reservationNo + " not found for booking " + bookingId));

        if (update.getAirlineCode() != null) fr.setAirlineCode(update.getAirlineCode());
        if (update.getFlightNumber() != null) fr.setFlightNumber(update.getFlightNumber());
        if (update.getDepartureDate() != null) fr.setDepartureDate(update.getDepartureDate());
        if (update.getDepartureTime() != null) fr.setDepartureTime(update.getDepartureTime());
        if (update.getArriveDate() != null) fr.setArriveDate(update.getArriveDate());
        if (update.getArriveTime() != null) fr.setArriveTime(update.getArriveTime());
        if (update.getRate() != null) fr.setRate(update.getRate());
        if (update.getOriginAirportCode() != null) fr.setOriginAirportCode(update.getOriginAirportCode());
        if (update.getDestinationAirportCode() != null) fr.setDestinationAirportCode(update.getDestinationAirportCode());

        return toFlightResResponse(flightResRepo.save(fr));
    }

    // ── Delete flight reservation ─────────────────────────────────────────────

    @Transactional
    public void deleteFlightReservation(Integer bookingId, Integer reservationNo) {
        FlightReservation fr = flightResRepo
                .findByReservationNoAndBookingId(reservationNo, bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Flight reservation " + reservationNo + " not found for booking " + bookingId));
        flightResRepo.delete(fr);
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private Booking findBookingOrThrow(Integer id) {
        return bookingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking with ID " + id + " not found"));
    }

    BookingResponse toBookingResponse(Booking b) {
        BookingResponse r = new BookingResponse();
        r.setBookingId(b.getBookingId());
        r.setUserId(b.getUserId());
        r.setAgentId(b.getAgentId());
        r.setStartDate(b.getStartDate());
        r.setEndDate(b.getEndDate());
        return r;
    }

    BookingDetailResponse toDetailResponse(Booking b) {
        BookingDetailResponse r = new BookingDetailResponse();
        r.setBookingId(b.getBookingId());
        r.setUserId(b.getUserId());
        r.setAgentId(b.getAgentId());
        r.setStartDate(b.getStartDate());
        r.setEndDate(b.getEndDate());

        // Embed user info
        userRepo.findById(b.getUserId()).ifPresent(u -> {
            UserResponse ur = new UserResponse();
            ur.setFirstName(u.getFirstName());
            ur.setLastName(u.getLastName());
            ur.setEmail(u.getEmail());
            r.setUser(ur);
        });

        // Embed hotel reservations
        r.setHotelReservations(b.getHotelReservations()
                .stream().map(this::toHotelResResponse).collect(Collectors.toList()));

        // Embed flight reservations
        r.setFlightReservations(b.getFlightReservations()
                .stream().map(this::toFlightResResponse).collect(Collectors.toList()));

        return r;
    }

    private HotelReservationResponse toHotelResResponse(HotelReservation hr) {
        HotelReservationResponse r = new HotelReservationResponse();
        r.setReservationNo(hr.getReservationNo());
        r.setHotelCode(hr.getHotelCode());
        r.setHotelName(hr.getHotelName());
        r.setCheckInDate(hr.getCheckInDate());
        r.setCheckInTime(hr.getCheckInTime());
        r.setCheckOutDate(hr.getCheckOutDate());
        r.setCheckOutTime(hr.getCheckOutTime());
        r.setRate(hr.getRate());
        return r;
    }

    private FlightReservationResponse toFlightResResponse(FlightReservation fr) {
        FlightReservationResponse r = new FlightReservationResponse();
        r.setReservationNo(fr.getReservationNo());
        r.setAirlineCode(fr.getAirlineCode());
        r.setFlightNumber(fr.getFlightNumber());
        r.setDepartureDate(fr.getDepartureDate());
        r.setDepartureTime(fr.getDepartureTime());
        r.setArriveDate(fr.getArriveDate());
        r.setArriveTime(fr.getArriveTime());
        r.setRate(fr.getRate());
        r.setOriginAirportCode(fr.getOriginAirportCode());
        r.setDestinationAirportCode(fr.getDestinationAirportCode());
        return r;
    }
}
