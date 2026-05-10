package com.thriftybackpacker.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thriftybackpacker.dto.user.UserResponse;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Full booking response including nested user info and reservation arrays.
 * Matches Python BookingDetailResponse schema exactly.
 * Used for GET /bookings/{id}, POST /bookings/ (201), and GET /bookings/by-agent-user.
 */
@Data
public class BookingDetailResponse {

    @JsonProperty("Booking_Id")
    private Integer bookingId;

    @JsonProperty("User_Id")
    private Integer userId;

    @JsonProperty("Agent_Id")
    private Integer agentId;

    @JsonProperty("Start_Date")
    private LocalDate startDate;

    @JsonProperty("End_Date")
    private LocalDate endDate;

    private UserResponse user;

    @JsonProperty("hotel_reservations")
    private List<HotelReservationResponse> hotelReservations = new ArrayList<>();

    @JsonProperty("flight_reservations")
    private List<FlightReservationResponse> flightReservations = new ArrayList<>();
}
