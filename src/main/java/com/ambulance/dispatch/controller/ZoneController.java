
package com.ambulance.dispatch.controller;

import com.ambulance.dispatch.entity.Zone;
import com.ambulance.dispatch.repository.ZoneRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/zone")
public class ZoneController {

    private final ZoneRepository zoneRepository;

    public ZoneController(ZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
    }

    // Create a new zone
    @PostMapping
    public Zone addZone(@RequestBody Zone zone) {
        if (zone.getZoneName() == null ||
                zone.getZoneName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Zone name is required"
            );
        }

        zone.setId(null);
        return zoneRepository.save(zone);
    }

    // Get all zones
    @GetMapping
    public List<Zone> getAllZones() {
        return zoneRepository.findAll();
    }

    // Get zone by ID
    @GetMapping("/{id}")
    public Zone getZoneById(@PathVariable Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Zone not found"
                        )
                );
    }
}