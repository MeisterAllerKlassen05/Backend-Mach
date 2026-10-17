package com.example.bookingservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreateBookingRequest {

    @NotNull(message = "Ride ID is required")
    @Positive(message = "Ride ID must be positive")
    private Long rideId;

    public CreateBookingRequest() {}

    public CreateBookingRequest(Long rideId) {
        this.rideId = rideId;
    }

    public Long getRideId() { return rideId; }
    public void setRideId(Long rideId) { this.rideId = rideId; }
}
