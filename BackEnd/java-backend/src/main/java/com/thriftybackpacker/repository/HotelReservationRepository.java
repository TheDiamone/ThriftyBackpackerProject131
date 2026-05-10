package com.thriftybackpacker.repository;

import com.thriftybackpacker.model.HotelReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HotelReservationRepository extends JpaRepository<HotelReservation, Integer> {

    Optional<HotelReservation> findByReservationNoAndBookingId(Integer reservationNo, Integer bookingId);
}
