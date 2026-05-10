package com.thriftybackpacker.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Embedded user info returned inside BookingDetailResponse.
 * Matches Python UserResponse schema — never exposes passwords.
 */
@Data
public class UserResponse {

    @JsonProperty("First_Name")
    private String firstName;

    @JsonProperty("Last_Name")
    private String lastName;

    @JsonProperty("Email")
    private String email;
}
