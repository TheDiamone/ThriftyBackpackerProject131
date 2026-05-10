package com.thriftybackpacker.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

/**
 * Response shape for a flight reservation — matches Python FlightReservationResponse exactly.
 */
@Data
public class FlightReservationResponse {

    @JsonProperty("Reservation_No")
    private Integer reservationNo;

    @JsonProperty("Airline_Code")
    private String airlineCode;

    @JsonProperty("Flight_Number")
    private String flightNumber;

    @JsonProperty("Departure_Date")
    private LocalDate departureDate;

    @JsonProperty("Departure_Time")
    private String departureTime;

    @JsonProperty("Arrive_Date")
    private LocalDate arriveDate;

    @JsonProperty("Arrive_Time")
    private String arriveTime;

    @JsonProperty("Rate")
    private Double rate;

    @JsonProperty("Origin_Airport_Code")
    private String originAirportCode;

    @JsonProperty("Destination_Airport_Code")
    private String destinationAirportCode;
}
