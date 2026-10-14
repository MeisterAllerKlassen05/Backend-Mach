package com.example.rideofferservice.controller;

import com.example.rideofferservice.dto.CreateRideRequest;
import com.example.rideofferservice.dto.RideResponse;
import com.example.rideofferservice.entity.RideOffer;
import com.example.rideofferservice.service.RideService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    /**
     * Öffentlicher Endpunkt: Alle verfügbaren (angebotenen) Mitfahrten abrufen.
     */
    @GetMapping("/public/available")
    public ResponseEntity<List<RideResponse>> getAvailableRides() {
        return ResponseEntity.ok(rideService.getAvailableRides());
    }

    /**
     * Öffentlicher Endpunkt: Mitfahrten suchen nach Start, Ziel und Abfahrtszeit.
     */
    @GetMapping("/public/search")
    public ResponseEntity<List<RideResponse>> searchRides(
            @RequestParam String startLocation,
            @RequestParam String endLocation,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to) {
        List<RideResponse> rides;

        if (from != null && to != null) {
            rides = rideService.searchRides(startLocation, endLocation, from, to);
        } else if (from != null) {
            rides = rideService.searchRidesAfter(startLocation, endLocation, from);
        } else if (to != null) {
            rides = rideService.searchRidesBefore(startLocation, endLocation, to);
        } else {
            rides = rideService.searchRides(startLocation, endLocation);
        }

        return ResponseEntity.ok(rides);
    }

    /**
     * Authentifiziert: Eigene Mitfahrten abrufen.
     */
    @GetMapping("/my")
    public ResponseEntity<List<RideResponse>> getMyRides(Principal principal) {
        String username = principal.getName();
        return ResponseEntity.ok(rideService.getMyRides(username));
    }

    /**
     * Authentifiziert: Eigene angebotene Mitfahrten abrufen.
     */
    @GetMapping("/my/offered")
    public ResponseEntity<List<RideResponse>> getMyOfferedRides(Principal principal) {
        String username = principal.getName();
        return ResponseEntity.ok(rideService.getMyOfferedRides(username));
    }

    /**
     * Authentifiziert: Neue Mitfahrt anbieten (jeder registrierte User).
     */
    @PostMapping
    public ResponseEntity<RideResponse> createRide(
            @Valid @RequestBody CreateRideRequest request,
            Principal principal) {
        String username = principal.getName();

        RideResponse created = rideService.createRide(username, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Authentifiziert: Mitfahrt abbrechen/stornieren.
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<RideResponse> cancelRide(
            @PathVariable Long id,
            Principal principal) {
        String username = principal.getName();
        RideResponse cancelled = rideService.cancelRide(id, username);
        return ResponseEntity.ok(cancelled);
    }

    /**
     * Authentifiziert: Mitfahrt löschen (nur Fahrer).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRide(
            @PathVariable Long id,
            Principal principal) {
        String username = principal.getName();
        rideService.deleteRide(id, username);
        return ResponseEntity.noContent().build();
    }

    /**
     * Öffentlicher Endpunkt: Einzelne Mitfahrt nach ID abrufen.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RideResponse> getRide(@PathVariable Long id) {
        RideResponse ride = rideService.getRideById(id);
        if (ride == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ride);
    }
}
