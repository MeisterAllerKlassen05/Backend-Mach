package com.example.bookingservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ride_id", nullable = false)
    private Long rideId;

    @Column(name = "passenger_username", nullable = false, length = 50)
    private String passengerUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum BookingStatus {
        CONFIRMED,
        CANCELLED
    }

    public Booking() {}

    public Booking(Long rideId, String passengerUsername) {
        this.rideId = rideId;
        this.passengerUsername = passengerUsername;
        this.status = BookingStatus.CONFIRMED;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRideId() { return rideId; }
    public void setRideId(Long rideId) { this.rideId = rideId; }

    public String getPassengerUsername() { return passengerUsername; }
    public void setPassengerUsername(String passengerUsername) { this.passengerUsername = passengerUsername; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
