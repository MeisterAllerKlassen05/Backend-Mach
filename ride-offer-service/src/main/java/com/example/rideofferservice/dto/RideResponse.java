package com.example.rideofferservice.dto;

import com.example.rideofferservice.entity.RideOffer;
import java.time.LocalDateTime;

public class RideResponse {

    private Long id;
    private String driverUsername;
    private String startLocation;
    private String endLocation;
    private LocalDateTime departureTime;
    private Integer availableSeats;
    private String additionalInfo;
    private RideOffer.RideStatus status;

    public RideResponse() {}

    public RideResponse(Long id, String driverUsername, String startLocation, String endLocation, 
                        LocalDateTime departureTime, Integer availableSeats, String additionalInfo, 
                        RideOffer.RideStatus status) {
        this.id = id;
        this.driverUsername = driverUsername;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.departureTime = departureTime;
        this.availableSeats = availableSeats;
        this.additionalInfo = additionalInfo;
        this.status = status;
    }

    public static RideResponse fromEntity(RideOffer entity) {
        return new RideResponse(
            entity.getId(),
            entity.getDriverUsername(),
            entity.getStartLocation(),
            entity.getEndLocation(),
            entity.getDepartureTime(),
            entity.getAvailableSeats(),
            entity.getAdditionalInfo(),
            entity.getStatus()
        );
    }

    public Long getId() { return id; }
    public String getDriverUsername() { return driverUsername; }
    public String getStartLocation() { return startLocation; }
    public String getEndLocation() { return endLocation; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public Integer getAvailableSeats() { return availableSeats; }
    public String getAdditionalInfo() { return additionalInfo; }
    public RideOffer.RideStatus getStatus() { return status; }
}
