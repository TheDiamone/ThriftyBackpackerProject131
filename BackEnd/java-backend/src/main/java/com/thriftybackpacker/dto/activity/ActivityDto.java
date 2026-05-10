package com.thriftybackpacker.dto.activity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Single activity item — matches Phase 2 ActivityDto.
 * cost uses BigDecimal for precision (most are 0.00 for London free activities).
 */
@Data
@AllArgsConstructor
public class ActivityDto {

    private String name;
    private BigDecimal cost;
    private String category;
}
