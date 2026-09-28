
package com.ambulance.dispatch.controller;

import com.ambulance.dispatch.entity.Ambulance;
import com.ambulance.dispatch.service.AmbulanceService;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@RestController
@RequestMapping("/ambulance")
public class AmbulanceController {

    private final AmbulanceService ambulanceService;

    public AmbulanceController(
            AmbulanceService ambulanceService) {
        this.ambulanceService = ambulanceService;
    }

    // Register ambulance
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Ambulance addAmbulance(
            @RequestBody Ambulance ambulance) {

        return ambulanceService.addAmbulance(ambulance);
    }

    // Get available ambulances
    @GetMapping("/available")
    public List<Ambulance> getAvailableAmbulances() {
        return ambulanceService.getAvailableAmbulances();
    }

    // Get all ambulances
    @GetMapping
    public List<Ambulance> getAllAmbulances() {
        return ambulanceService.getAllAmbulances();
    }

    // Get ambulance by ID
    @GetMapping("/{id}")
    public Ambulance getAmbulanceById(
            @PathVariable Long id) {

        return ambulanceService.getAmbulanceById(id);
    }
}