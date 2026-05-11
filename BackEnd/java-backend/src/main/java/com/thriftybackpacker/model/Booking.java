package com.thriftybackpacker.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps to the 'bookings' table — identical schema to the Python SQLAlchemy Booking model.
 * Agent_Id is nullable (tenant/agency identifier sent by the frontend).
 */
@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Booking_Id")
    private Integer bookingId;

    @Column(name = "User_Id", nullable = false)
    private Integer userId;

    @Column(name = "Agent_Id")
    private Integer agentId;

    @Column(name = "Start_Date", nullable = false)
    private LocalDate startDate;

    @Column(name = "End_Date", nullable = false)
    private LocalDate endDate;

    /** Booking lifecycle status. Starts as PENDING; can be advanced to CONFIRMED or CANCELLED. */
    @Column(name = "Status", nullable = false)
    private String status = "PENDING";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "User_Id", insertable = false, updatable = false)
    private User user;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HotelReservation> hotelReservations = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FlightReservation> flightReservations = new ArrayList<>();
}
