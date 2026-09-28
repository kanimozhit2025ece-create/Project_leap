
package com.ambulance.dispatch.service;

import com.ambulance.dispatch.entity.Zone;
import com.ambulance.dispatch.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ZoneService {

    private final ZoneRepository zoneRepository;

    public ZoneService(ZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
    }

    // Add zone
    public Zone addZone(Zone zone) {

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
    public List<Zone> getAllZones() {
        return zoneRepository.findAll();
    }

    // Get zone by ID
    public Zone getZoneById(Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Zone not found"
                        )
                );
    }
}