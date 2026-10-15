package com.example.rideofferservice.service;

import com.example.rideofferservice.dto.CreateRideRequest;
import com.example.rideofferservice.dto.RideResponse;
import com.example.rideofferservice.entity.RideOffer;
import com.example.rideofferservice.repository.RideRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RideService {

    private static final Logger log = LoggerFactory.getLogger(RideService.class);

    private final RideRepository rideRepository;
    private final RabbitTemplate rabbitTemplate;
    private final boolean rabbitmqAvailable;

    @Value("${app.rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${app.rabbitmq.routing.key}")
    private String routingKey;

    public RideService(RideRepository rideRepository, CachingConnectionFactory connectionFactory) {
        this.rideRepository = rideRepository;
        
        // Prüfe ob RabbitMQ erreichbar ist
        boolean available = false;
        try (Connection conn = connectionFactory.createConnection()) {
            available = true;
        } catch (Exception e) {
            log.warn("RabbitMQ nicht verfügbar: {}", e.getMessage());
        }
        this.rabbitmqAvailable = available;
        
        // Erstelle einen null-safe RabbitTemplate falls RabbitMQ nicht da ist
        RabbitTemplate template;
        try {
            if (available) {
                template = new RabbitTemplate(connectionFactory);
                template.setMessageConverter(new org.springframework.amqp.support.converter.Jackson2JsonMessageConverter());
            } else {
                template = null;
            }
        } catch (Exception e) {
            log.warn("RabbitTemplate konnte nicht erstellt werden, Events werden ignoriert.");
            template = null;
        }
        this.rabbitTemplate = template;
    }

    // --- Öffentliche (nicht authentifizierte) Methoden ---

    public List<RideResponse> getAvailableRides() {
        return rideRepository.findByStatus(RideOffer.RideStatus.OFFERED)
                .stream()
                .map(RideResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Suche Mitfahrten mit exaktem Start und Ziel.
     */
    public List<RideResponse> searchRides(String startLocation, String endLocation) {
        return rideRepository.findByStartAndEndLocation(startLocation, endLocation)
                .stream()
                .map(RideResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Suche Mitfahrten mit exaktem Start und Ziel innerhalb eines Zeitraums.
     */
    public List<RideResponse> searchRides(String startLocation, String endLocation, LocalDateTime from, LocalDateTime to) {
        return rideRepository.findByStartAndEndBetween(startLocation, endLocation, from, to)
                .stream()
                .map(RideResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Suche Mitfahrten mit exaktem Start und Ziel ab einem Zeitpunkt.
     */
    public List<RideResponse> searchRidesAfter(String startLocation, String endLocation, LocalDateTime from) {
        return rideRepository.findByStartAndEndAfter(startLocation, endLocation, from)
                .stream()
                .map(RideResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Suche Mitfahrten mit exaktem Start und Ziel bis zu einem Zeitpunkt.
     */
    public List<RideResponse> searchRidesBefore(String startLocation, String endLocation, LocalDateTime to) {
        return rideRepository.findByStartAndEndBefore(startLocation, endLocation, to)
                .stream()
                .map(RideResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public RideResponse getRideById(Long id) {
        Optional<RideOffer> ride = rideRepository.findById(id);
        return ride.map(RideResponse::fromEntity).orElse(null);
    }

    // --- Authentifizierte Methoden ---

    public List<RideResponse> getMyRides(String username) {
        return rideRepository.findByDriverUsername(username)
                .stream()
                .map(RideResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<RideResponse> getMyOfferedRides(String username) {
        return rideRepository.findByStatusAndDriverUsername(RideOffer.RideStatus.OFFERED, username)
                .stream()
                .map(RideResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public RideResponse createRide(String username, CreateRideRequest request) {
        if (request.getAvailableSeats() == null || request.getAvailableSeats() <= 0) {
            throw new IllegalArgumentException("Verfügbare Sitzplätze müssen größer als 0 sein.");
        }

        RideOffer ride = new RideOffer();
        ride.setDriverUsername(username);
        ride.setStartLocation(request.getStartLocation());
        ride.setEndLocation(request.getEndLocation());
        ride.setDepartureTime(request.getDepartureTime());
        ride.setAvailableSeats(request.getAvailableSeats());
        ride.setStatus(RideOffer.RideStatus.OFFERED);
        ride.setCreatedAt(LocalDateTime.now());

        RideOffer saved = rideRepository.save(ride);

        // Sende RabbitMQ Event für neue Mitfahrt
        publishRideOfferedEvent(saved);

        log.info("Neue Mitfahrt erstellt von {}: {} -> {}", username, request.getStartLocation(), request.getEndLocation());

        return RideResponse.fromEntity(saved);
    }

    @Transactional
    public RideResponse updateAvailableSeats(Long id, String username, int newSeats) {
        if (newSeats <= 0) {
            throw new IllegalArgumentException("Sitzplätze müssen größer als 0 sein.");
        }

        RideOffer ride = rideRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mitfahrt nicht gefunden!"));

        if (!ride.getDriverUsername().equals(username)) {
            throw new SecurityException("Nur der Fahrer kann die Sitzplätze aktualisieren!");
        }

        if (ride.getStatus() == RideOffer.RideStatus.FULFILLED) {
            throw new IllegalStateException("Eine abgeschlossene Mitfahrt kann nicht aktualisiert werden.");
        }

        ride.setAvailableSeats(newSeats);
        RideOffer updated = rideRepository.save(ride);
        log.info("Sitzplätze für Mitfahrt {} aktualisiert auf: {}", id, newSeats);

        return RideResponse.fromEntity(updated);
    }

    @Transactional
    public RideResponse fulfillRide(Long id, String passengerUsername) {
        RideOffer ride = rideRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mitfahrt nicht gefunden!"));

        if (ride.getStatus() != RideOffer.RideStatus.OFFERED) {
            throw new IllegalStateException("Nur eine angebotene Mitfahrt kann erfüllt werden.");
        }

        if (ride.getAvailableSeats() <= 0) {
            throw new IllegalStateException("Keine freien Sitzplätze mehr verfügbar.");
        }

        ride.setStatus(RideOffer.RideStatus.FULFILLED);
        ride.setFulfilledAt(LocalDateTime.now());
        RideOffer updated = rideRepository.save(ride);

        log.info("Mitfahrt {} wurde von {} erfüllt.", id, passengerUsername);

        return RideResponse.fromEntity(updated);
    }

    @Transactional
    public RideResponse cancelRide(Long id, String username) {
        RideOffer ride = rideRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mitfahrt nicht gefunden!"));

        if (!ride.getDriverUsername().equals(username)) {
            throw new SecurityException("Nur der Fahrer kann die Mitfahrt stornieren!");
        }

        if (ride.getStatus() == RideOffer.RideStatus.FULFILLED || ride.getStatus() == RideOffer.RideStatus.CANCELLED) {
            throw new IllegalStateException("Mitfahrt kann nicht mehr storniert werden.");
        }

        ride.setStatus(RideOffer.RideStatus.CANCELLED);
        RideOffer updated = rideRepository.save(ride);

        // Sende RabbitMQ Event für stornierte Mitfahrt
        publishRideCancelledEvent(updated);

        log.info("Mitfahrt {} wurde von {} storniert.", id, username);

        return RideResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteRide(Long id, String username) {
        RideOffer ride = rideRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mitfahrt nicht gefunden!"));

        if (!ride.getDriverUsername().equals(username)) {
            throw new SecurityException("Nur der Fahrer kann die Mitfahrt löschen!");
        }

        if (ride.getStatus() == RideOffer.RideStatus.FULFILLED) {
            throw new IllegalStateException("Eine abgeschlossene Mitfahrt kann nicht gelöscht werden.");
        }

        rideRepository.delete(ride);

        log.info("Mitfahrt {} wurde von {} gelöscht.", id, username);
    }

    // --- RabbitMQ Events ---

    private void publishRideOfferedEvent(RideOffer ride) {
        if (rabbitTemplate == null) {
            log.warn("RabbitTemplate ist null — kann Event nicht senden. Ride gespeichert aber nicht publiziert: {}", ride.getId());
            return;
        }
        try {
            RideOfferedEvent event = new RideOfferedEvent();
            event.setRideId(ride.getId());
            event.setDriverUsername(ride.getDriverUsername());
            event.setStartLocation(ride.getStartLocation());
            event.setEndLocation(ride.getEndLocation());
            event.setDepartureTime(ride.getDepartureTime());
            event.setAvailableSeats(ride.getAvailableSeats());
            event.setTimestamp(LocalDateTime.now());

            rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
            log.info("RideOfferedEvent gesendet: {} -> {}", ride.getStartLocation(), ride.getEndLocation());
        } catch (Exception e) {
            log.warn("Fehler beim Senden von RideOfferedEvent: {}. Ride wurde gespeichert aber Event ignoriert.", e.getMessage());
        }
    }

    private void publishRideCancelledEvent(RideOffer ride) {
        if (rabbitTemplate == null) return;
        try {
            RideCancelledEvent event = new RideCancelledEvent();
            event.setRideId(ride.getId());
            event.setDriverUsername(ride.getDriverUsername());
            event.setTimestamp(LocalDateTime.now());

            rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
            log.info("RideCancelledEvent gesendet: {}", ride.getId());
        } catch (Exception e) {
            log.warn("Fehler beim Senden von RideCancelledEvent: {}. Ride wurde gespeichert.", e.getMessage());
        }
    }

    // --- Event DTOs ---

    public static class RideOfferedEvent {
        private Long rideId;
        private String driverUsername;
        private String startLocation;
        private String endLocation;
        private LocalDateTime departureTime;
        private Integer availableSeats;
        private LocalDateTime timestamp;

        public Long getRideId() { return rideId; }
        public void setRideId(Long rideId) { this.rideId = rideId; }
        public String getDriverUsername() { return driverUsername; }
        public void setDriverUsername(String driverUsername) { this.driverUsername = driverUsername; }
        public String getStartLocation() { return startLocation; }
        public void setStartLocation(String startLocation) { this.startLocation = startLocation; }
        public String getEndLocation() { return endLocation; }
        public void setEndLocation(String endLocation) { this.endLocation = endLocation; }
        public LocalDateTime getDepartureTime() { return departureTime; }
        public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }
        public Integer getAvailableSeats() { return availableSeats; }
        public void setAvailableSeats(Integer availableSeats) { this.availableSeats = availableSeats; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class RideCancelledEvent {
        private Long rideId;
        private String driverUsername;
        private LocalDateTime timestamp;

        public Long getRideId() { return rideId; }
        public void setRideId(Long rideId) { this.rideId = rideId; }
        public String getDriverUsername() { return driverUsername; }
        public void setDriverUsername(String driverUsername) { this.driverUsername = driverUsername; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
