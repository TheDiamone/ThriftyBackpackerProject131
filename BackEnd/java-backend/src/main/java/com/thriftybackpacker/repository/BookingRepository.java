package com.thriftybackpacker.repository;

import com.thriftybackpacker.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    /** Used by GET /api/v1/bookings/by-agent-user — the endpoint the frontend calls. */
    List<Booking> findByUserIdAndAgentId(Integer userId, Integer agentId);

    /** Used as fallback when only userId is provided. */
    List<Booking> findByUserId(Integer userId);
}
