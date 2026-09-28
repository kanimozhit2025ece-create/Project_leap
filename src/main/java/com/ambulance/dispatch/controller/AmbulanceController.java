
package com.ambulance.dispatch.controller;

import com.ambulance.dispatch.entity.Ambulance;
import com.ambulance.dispatch.entity.AmbulanceLocationHistory;
import com.ambulance.dispatch.service.AmbulanceService;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/ambulance")
public class AmbulanceController {

    private final AmbulanceService ambulanceService;

    public AmbulanceController(
            AmbulanceService ambulanceService) {

        this.ambulanceService = ambulanceService;
    }

    // 1. REGISTER AMBULANCE

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Ambulance addAmbulance(
            @RequestBody Ambulance ambulance) {

        return ambulanceService.addAmbulance(ambulance);
    }

    // 2. GET AVAILABLE AMBULANCES

    @GetMapping("/available")
    public List<Ambulance> getAvailableAmbulances() {

        return ambulanceService.getAvailableAmbulances();
    }

    // 3. GET ALL AMBULANCES

    @GetMapping
    public List<Ambulance> getAllAmbulances() {

        return ambulanceService.getAllAmbulances();
    }

    // 4. GET AMBULANCE BY ID

    @GetMapping("/{id}")
    public Ambulance getAmbulanceById(
            @PathVariable Long id) {

        return ambulanceService.getAmbulanceById(id);
    }

    // 5. UPDATE AMBULANCE LOCATION

    @PutMapping("/{ambulanceId}/location/{zoneId}")
    public Ambulance updateAmbulanceLocation(
            @PathVariable Long ambulanceId,
            @PathVariable Long zoneId) {

        return ambulanceService.updateAmbulanceLocation(
                ambulanceId,
                zoneId
        );
    }

    // 6. GET AMBULANCE LOCATION HISTORY

    @GetMapping("/{ambulanceId}/history")
    public List<AmbulanceLocationHistory> getLocationHistory(
            @PathVariable Long ambulanceId) {

        return ambulanceService.getLocationHistory(
                ambulanceId
        );
    }
}