package com.thriftybackpacker.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Maps to the 'flight_reservations' table — identical schema to the Python SQLAlchemy FlightReservation model.
 */
@Entity
@Table(name = "flight_reservations")
@Data
@NoArgsConstructor
public class FlightReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Reservation_No")
    private Integer reservationNo;

    @Column(name = "Booking_Id", nullable = false)
    private Integer bookingId;

    @Column(name = "Airline_Code", nullable = false)
    private String airlineCode;

    @Column(name = "Flight_Number", nullable = false)
    private String flightNumber;

    @Column(name = "Departure_Date", nullable = false)
    private LocalDate departureDate;

    @Column(name = "Departure_Time", nullable = false)
    private String departureTime;

    @Column(name = "Arrive_Date", nullable = false)
    private LocalDate arriveDate;

    @Column(name = "Arrive_Time", nullable = false)
    private String arriveTime;

    @Column(name = "Rate")
    private Double rate;

    @Column(name = "Origin_Airport_Code", nullable = false)
    private String originAirportCode;

    @Column(name = "Destination_Airport_Code", nullable = false)
    private String destinationAirportCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Booking_Id", insertable = false, updatable = false)
    private Booking booking;
}
