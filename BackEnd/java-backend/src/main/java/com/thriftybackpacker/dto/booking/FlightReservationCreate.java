package com.thriftybackpacker.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * Used inside BookingCreateRequest.flight_reservations[].
 * Field names match Python Pydantic FlightReservationCreate exactly.
 */
@Data
public class FlightReservationCreate {

    @JsonProperty("Airline_Code")
    private String airlineCode;

    @JsonProperty("Flight_Number")
    private String flightNumber;

    @NotNull
    @JsonProperty("Departure_Date")
    private LocalDate departureDate;

    @NotBlank
    @JsonProperty("Departure_Time")
    private String departureTime;

    @NotNull
    @JsonProperty("Arrive_Date")
    private LocalDate arriveDate;

    @NotBlank
    @JsonProperty("Arrive_Time")
    private String arriveTime;

    @JsonProperty("Rate")
    private Double rate;

    @NotBlank
    @JsonProperty("Origin_Airport_Code")
    private String originAirportCode;

    @NotBlank
    @JsonProperty("Destination_Airport_Code")
    private String destinationAirportCode;
}
