package com.example.bookingservice.controller;

import com.example.bookingservice.dto.BookingResponse;
import com.example.bookingservice.dto.CreateBookingRequest;
import com.example.bookingservice.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<?> createBooking(@Valid @RequestBody CreateBookingRequest request, Authentication authentication) {
        try {
            BookingResponse booking = bookingService.createBooking(authentication.getName(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(booking);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Interner Fehler: " + e.getMessage()));
        }
    }

    @GetMapping("/ride/{rideId}")
    public ResponseEntity<List<BookingResponse>> getBookingsForRide(@PathVariable Long rideId) {
        return ResponseEntity.ok(bookingService.getBookingsForRide(rideId));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication authentication) {
        return ResponseEntity.ok(bookingService.getBookingsByPassenger(authentication.getName()));
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<?> cancelBooking(@PathVariable Long bookingId, Authentication authentication) {
        try {
            bookingService.cancelBooking(bookingId, authentication.getName());
            return ResponseEntity.ok(Map.of("message", "Buchung erfolgreich gelöscht!"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
