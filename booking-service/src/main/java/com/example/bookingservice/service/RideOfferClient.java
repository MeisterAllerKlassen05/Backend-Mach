package com.example.bookingservice.service;

import com.example.bookingservice.dto.RideDetailsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class RideOfferClient {

    private static final Logger log = LoggerFactory.getLogger(RideOfferClient.class);

    @Value("${app.ride-offer-service.url:http://localhost:8081}")
    private String rideOfferServiceUrl;

    private final RestTemplate restTemplate;

    public RideOfferClient() {
        // Spring Boot 3.x registriert automatisch Jackson2HttpMessageConverter mit JavaTimeModule.
        // Ein manuell erstellter RestTemplate ohne Custom-Config nutzt diese Standardkonverter.
        this.restTemplate = new RestTemplate();
    }

    /**
     * Hole eine einzelne Mitfahrt über den Ride-Offer-Service.
     */
    public RideDetailsDto getRideById(Long rideId) {
        String url = rideOfferServiceUrl + "/api/rides/" + rideId;
        try {
            ResponseEntity<RideDetailsDto> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, RideDetailsDto.class);
            
            log.info("Erfolgreiche Abfrage von Ride {}.", rideId);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Ride {} nicht gefunden.", rideId);
            return null;
        } catch (Exception e) {
            log.error("Fehler beim Abruf der Mitfahrt {}: {}", rideId, e.getMessage());
            throw new RuntimeException("Kann Ride-Offer-Service nicht erreichen: " + e.getMessage());
        }
    }

    /**
     * Storniere eine Buchung im Ride-Offer-Service (optional).
     */
    public void notifyRideCancelled(Long rideId) {
        String url = rideOfferServiceUrl + "/api/rides/" + rideId + "/cancel";
        try {
            restTemplate.patchForObject(url, null, RideDetailsDto.class);
            log.info("Ride {} storniert über Ride-Offer-Service", rideId);
        } catch (Exception e) {
            log.warn("Fehler beim Stornieren der Mitfahrt {}: {}", rideId, e.getMessage());
        }
    }
}
