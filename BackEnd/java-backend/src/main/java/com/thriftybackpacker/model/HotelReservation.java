package com.thriftybackpacker.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Maps to the 'hotel_reservations' table — identical schema to the Python SQLAlchemy HotelReservation model.
 */
@Entity
@Table(name = "hotel_reservations")
@Data
@NoArgsConstructor
public class HotelReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Reservation_No")
    private Integer reservationNo;

    @Column(name = "Booking_Id", nullable = false)
    private Integer bookingId;

    @Column(name = "Hotel_Code", nullable = false)
    private Integer hotelCode;
    
    @Column(name = "Hotel_Name")
    private String hotelName;

    @Column(name = "Check_In_Date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "Check_In_Time")
    private String checkInTime;

    @Column(name = "Check_Out_Date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "Check_Out_Time")
    private String checkOutTime;

    @Column(name = "Rate")
    private Double rate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Booking_Id", insertable = false, updatable = false)
    private Booking booking;
}
