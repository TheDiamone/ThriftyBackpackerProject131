package com.thriftybackpacker.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Request body for POST /api/v1/bookings/.
 * Field names and nested list names match the Python Pydantic BookingCreate schema exactly.
 * The frontend bookingService.js sends this exact shape.
 */
@Data
public class BookingCreateRequest {

    @NotNull
    @JsonProperty("User_Id")
    private Integer userId;

    @JsonProperty("Agent_Id")
    private Integer agentId;

    @NotNull
    @JsonProperty("Start_Date")
    private LocalDate startDate;

    @NotNull
    @JsonProperty("End_Date")
    private LocalDate endDate;

    @Valid
    @JsonProperty("hotel_reservations")
    private List<HotelReservationCreate> hotelReservations = new ArrayList<>();

    @Valid
    @JsonProperty("flight_reservations")
    private List<FlightReservationCreate> flightReservations = new ArrayList<>();
}
