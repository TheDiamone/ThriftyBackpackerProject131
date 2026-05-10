package com.thriftybackpacker.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

/**
 * Flat booking response — matches Python BookingResponse schema.
 * Used for PATCH and list responses that don't embed nested reservations.
 */
@Data
public class BookingResponse {

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
}
