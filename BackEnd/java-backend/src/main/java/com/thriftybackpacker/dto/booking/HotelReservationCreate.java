package com.thriftybackpacker.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * Used inside BookingCreateRequest.hotel_reservations[].
 * Field names match the Python Pydantic HotelReservationCreate schema exactly.
 */
@Data
public class HotelReservationCreate {

    @NotNull
    @JsonProperty("Hotel_Code")
    private Integer hotelCode;

    @JsonProperty("Hotel_Name")
    private String hotelName;

    @NotNull
    @JsonProperty("Check_In_Date")
    private LocalDate checkInDate;

    @JsonProperty("Check_In_Time")
    private String checkInTime;

    @NotNull
    @JsonProperty("Check_Out_Date")
    private LocalDate checkOutDate;

    @JsonProperty("Check_Out_Time")
    private String checkOutTime;

    @JsonProperty("Rate")
    private Double rate;
}
