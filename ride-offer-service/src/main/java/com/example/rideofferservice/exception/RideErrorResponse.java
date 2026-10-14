package com.example.rideofferservice.exception;

/**
 * Konsistenter Error-Response für alle Exceptions im Ride-Offer-Service.
 */
public class RideErrorResponse {

    private int statusCode;
    private String error;
    private String message;
    private long timestamp;

    public RideErrorResponse() {}

    public RideErrorResponse(int statusCode, String error, String message) {
        this.statusCode = statusCode;
        this.error = error;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
