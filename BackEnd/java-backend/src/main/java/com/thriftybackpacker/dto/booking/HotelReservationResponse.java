package com.thriftybackpacker.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

/**
 * Response shape for a hotel reservation — matches Python HotelReservationResponse exactly.
 */
@Data
public class HotelReservationResponse {

    @JsonProperty("Reservation_No")
    private Integer reservationNo;

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
