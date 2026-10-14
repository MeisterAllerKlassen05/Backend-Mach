package com.example.rideofferservice.exception;

/**
 * Wird geworfen, wenn die angebotene Mitfahrt ungültig ist
 * (z.B. past departure time, negative seats).
 */
public class InvalidRideException extends RuntimeException {

    public InvalidRideException(String message) {
        super(message);
    }
}
