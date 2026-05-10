package com.thriftybackpacker.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

/**
 * Partial update body for PATCH /api/v1/bookings/{id}.
 * All fields are optional — only provided fields are applied.
 * Matches Python BookingUpdate schema.
 */
@Data
public class BookingUpdate {

    @JsonProperty("Start_Date")
    private LocalDate startDate;

    @JsonProperty("End_Date")
    private LocalDate endDate;

    @JsonProperty("Agent_Id")
    private Integer agentId;
}
