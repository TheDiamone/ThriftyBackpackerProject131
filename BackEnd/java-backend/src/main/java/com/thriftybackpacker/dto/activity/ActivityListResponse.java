package com.thriftybackpacker.dto.activity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Response for GET /api/v1/activities?destination=LHR.
 * Matches Phase 2 response contract: { destination, activities[] }.
 */
@Data
@AllArgsConstructor
public class ActivityListResponse {

    private String destination;
    private List<ActivityDto> activities;
}
