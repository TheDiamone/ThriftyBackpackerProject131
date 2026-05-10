package com.thriftybackpacker.repository;

import com.thriftybackpacker.model.FlightReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FlightReservationRepository extends JpaRepository<FlightReservation, Integer> {

    Optional<FlightReservation> findByReservationNoAndBookingId(Integer reservationNo, Integer bookingId);
}
