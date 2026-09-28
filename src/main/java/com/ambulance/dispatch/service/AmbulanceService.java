
package com.ambulance.dispatch.service;

import com.ambulance.dispatch.entity.Ambulance;
import com.ambulance.dispatch.entity.Zone;
import com.ambulance.dispatch.repository.AmbulanceRepository;
import com.ambulance.dispatch.repository.ZoneRepository;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AmbulanceService {

    private final AmbulanceRepository ambulanceRepository;
    private final ZoneRepository zoneRepository;

    public AmbulanceService(
            AmbulanceRepository ambulanceRepository,
            ZoneRepository zoneRepository) {

        this.ambulanceRepository = ambulanceRepository;
        this.zoneRepository = zoneRepository;
    }

    // Register ambulance
    public Ambulance addAmbulance(Ambulance ambulance) {

        if (ambulance.getAmbulanceNumber() == null ||
                ambulance.getAmbulanceNumber().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ambulance number is required"
            );
        }

        if (ambulance.getHomeZone() == null ||
                ambulance.getHomeZone().getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Home zone is required"
            );
        }

        Long homeZoneId = ambulance.getHomeZone().getId();

        Zone homeZone = zoneRepository.findById(homeZoneId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Home zone not found"
                        )
                );

        ambulance.setId(null);
        ambulance.setHomeZone(homeZone);
        ambulance.setCurrentZone(homeZone);
        ambulance.setStatus("AVAILABLE");

        return ambulanceRepository.save(ambulance);
    }

    // Get all ambulances
    public List<Ambulance> getAllAmbulances() {
        return ambulanceRepository.findAll();
    }

    // Get ambulance by ID
    public Ambulance getAmbulanceById(Long id) {
        return ambulanceRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Ambulance not found"
                        )
                );
    }

    // Get available ambulances
    public List<Ambulance> getAvailableAmbulances() {
        return ambulanceRepository.findByStatus("AVAILABLE");
    }
}