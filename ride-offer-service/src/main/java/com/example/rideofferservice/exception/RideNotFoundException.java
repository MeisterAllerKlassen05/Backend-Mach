package com.example.rideofferservice.exception;

/**
 * Wird geworfen, wenn eine Mitfahrt mit der gegebenen ID nicht gefunden wird.
 */
public class RideNotFoundException extends RuntimeException {

    public RideNotFoundException(Long rideId) {
        super("Mitfahrt mit ID " + rideId + " nicht gefunden!");
    }

    public RideNotFoundException(String message) {
        super(message);
    }
}
