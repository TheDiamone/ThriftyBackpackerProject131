package com.thriftybackpacker.controller;

import com.thriftybackpacker.dto.booking.*;
import com.thriftybackpacker.service.BookingService;
import com.thriftybackpacker.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Booking CRUD endpoints — matches all Python booking.py routes exactly.
 *
 * Additional endpoint added for frontend compatibility (missing from Python backend):
 *   GET /api/v1/bookings/by-agent-user — called by frontend bookingService.js listBookings()
 *
 * Python equivalent files: app/api/v1/endpoints/booking.py
 */
@Tag(name = "Bookings", description = "Booking CRUD and reservation management")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserService userService;

    // ── List bookings (Python: GET /bookings/ with skip/limit) ────────────────

    @Operation(summary = "List all bookings", description = "Returns a paginated list of bookings. Use skip and limit to paginate.")
    @GetMapping("/bookings/")
    public List<BookingResponse> listBookings(
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "100") int limit) {
        return bookingService.listBookings(skip, limit);
    }

    // ── NEW: by-agent-user (frontend calls this, Python does NOT have it) ─────

    @Operation(summary = "Get bookings by user and agent",
            description = "Returns bookings filtered by userId and agentId. Called by the Vue frontend bookingService.js.")
    @GetMapping("/bookings/by-agent-user")
    public List<BookingDetailResponse> getBookingsByAgentUser(
            @RequestParam("user_id") Integer userId,
            @RequestParam(value = "agent_id", required = false) Integer agentId) {
        return bookingService.getBookingsByAgentUser(userId, agentId);
    }

    // ── Get by ID (Python: GET /bookings/{booking_id}) ────────────────────────

    @Operation(summary = "Get booking details", description = "Returns full booking details including user info and all reservations.")
    @GetMapping("/bookings/{bookingId}")
    public BookingDetailResponse getBooking(@PathVariable Integer bookingId) {
        return bookingService.getBooking(bookingId);
    }

    // ── Create (Python: POST /bookings/) ─────────────────────────────────────

    @Operation(summary = "Create a new booking",
            description = "Creates a booking with optional nested hotel and flight reservations. Returns 201 Created.")
    @PostMapping("/bookings/")
    public ResponseEntity<BookingDetailResponse> createBooking(@Valid @RequestBody BookingCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(req));
    }

    // ── Update booking (Python: PATCH /bookings/{booking_id}) ────────────────

    @Operation(summary = "Update an existing booking", description = "Partial update: only provided fields are changed.")
    @PatchMapping("/bookings/{bookingId}")
    public BookingResponse updateBooking(
            @PathVariable Integer bookingId,
            @RequestBody BookingUpdate update) {
        return bookingService.updateBooking(bookingId, update);
    }

    // ── Delete booking (Python: DELETE /bookings/{booking_id}) ───────────────

    @Operation(summary = "Delete a booking", description = "Permanently deletes the booking and its reservations.")
    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Integer bookingId) {
        bookingService.deleteBooking(bookingId);
        return ResponseEntity.noContent().build();
    }

    // ── Update hotel reservation ──────────────────────────────────────────────

    @Operation(summary = "Update a hotel reservation", description = "Partial update of a hotel reservation under a booking.")
    @PatchMapping("/bookings/{bookingId}/hotel-reservations/{reservationNo}")
    public HotelReservationResponse updateHotelReservation(
            @PathVariable Integer bookingId,
            @PathVariable Integer reservationNo,
            @RequestBody HotelReservationUpdate update) {
        return bookingService.updateHotelReservation(bookingId, reservationNo, update);
    }

    // ── Delete hotel reservation ──────────────────────────────────────────────

    @Operation(summary = "Delete a hotel reservation")
    @DeleteMapping("/bookings/{bookingId}/hotel-reservations/{reservationNo}")
    public ResponseEntity<Void> deleteHotelReservation(
            @PathVariable Integer bookingId,
            @PathVariable Integer reservationNo) {
        bookingService.deleteHotelReservation(bookingId, reservationNo);
        return ResponseEntity.noContent().build();
    }

    // ── Update flight reservation ─────────────────────────────────────────────

    @Operation(summary = "Update a flight reservation", description = "Partial update of a flight reservation under a booking.")
    @PatchMapping("/bookings/{bookingId}/flight-reservations/{reservationNo}")
    public FlightReservationResponse updateFlightReservation(
            @PathVariable Integer bookingId,
            @PathVariable Integer reservationNo,
            @RequestBody FlightReservationUpdate update) {
        return bookingService.updateFlightReservation(bookingId, reservationNo, update);
    }

    // ── Delete flight reservation ─────────────────────────────────────────────

    @Operation(summary = "Delete a flight reservation")
    @DeleteMapping("/bookings/{bookingId}/flight-reservations/{reservationNo}")
    public ResponseEntity<Void> deleteFlightReservation(
            @PathVariable Integer bookingId,
            @PathVariable Integer reservationNo) {
        bookingService.deleteFlightReservation(bookingId, reservationNo);
        return ResponseEntity.noContent().build();
    }

    // ── Seed data (Python: GET /setup-seed-data/ — hidden from schema) ────────

    @Operation(hidden = true)
    @GetMapping("/setup-seed-data/")
    public Map<String, String> seedData() {
        var user = userService.createUser("Test", "Subject", "test@example.com", "555-0100", "1234567");
        return Map.of("message", "Seed user ready (ID: " + user.getUserId() + "). Use this User_Id to create bookings.");
    }
}
