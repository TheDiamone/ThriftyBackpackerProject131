package com.thriftybackpacker.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

/**
 * Partial update body for PATCH /api/v1/bookings/{id}/hotel-reservations/{no}.
 * All fields optional — only provided fields are applied.
 */
@Data
public class HotelReservationUpdate {

    @JsonProperty("Hotel_Code")
    private Integer hotelCode;

    @JsonProperty("Check_In_Date")
    private LocalDate checkInDate;

    @JsonProperty("Check_In_Time")
    private String checkInTime;

    @JsonProperty("Check_Out_Date")
    private LocalDate checkOutDate;

    @JsonProperty("Check_Out_Time")
    private String checkOutTime;

    @JsonProperty("Rate")
    private Double rate;
}
