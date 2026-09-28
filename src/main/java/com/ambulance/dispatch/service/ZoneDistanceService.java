
package com.ambulance.dispatch.service;

import com.ambulance.dispatch.entity.Zone;
import com.ambulance.dispatch.entity.ZoneDistance;
import com.ambulance.dispatch.repository.ZoneRepository;
import com.ambulance.dispatch.repository.ZoneDistanceRepository;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ZoneDistanceService {

    private final ZoneDistanceRepository distanceRepository;
    private final ZoneRepository zoneRepository;

    public ZoneDistanceService(
            ZoneDistanceRepository distanceRepository,
            ZoneRepository zoneRepository) {

        this.distanceRepository = distanceRepository;
        this.zoneRepository = zoneRepository;
    }

    // Save distance between two zones
    public ZoneDistance addDistance(ZoneDistance distance) {

        if (distance.getFromZone() == null ||
                distance.getToZone() == null ||
                distance.getFromZone().getId() == null ||
                distance.getToZone().getId() == null ||
                distance.getDistanceKm() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Zone IDs and distance are required"
            );
        }

        Long fromId = distance.getFromZone().getId();
        Long toId = distance.getToZone().getId();

        Zone fromZone = zoneRepository.findById(fromId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "From zone not found"
                        )
                );

        Zone toZone = zoneRepository.findById(toId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "To zone not found"
                        )
                );

        if (distance.getDistanceKm() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Distance cannot be negative"
            );
        }

        if (fromId.equals(toId) &&
                distance.getDistanceKm() != 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Same zone distance must be zero"
            );
        }

        distance.setId(null);
        distance.setFromZone(fromZone);
        distance.setToZone(toZone);

        return distanceRepository.save(distance);
    }

    // Get all distance records
    public List<ZoneDistance> getAllDistances() {
        return distanceRepository.findAll();
    }

    // Find distance between two zones
    public Double getDistance(Long fromId, Long toId) {

        if (fromId.equals(toId)) {
            return 0.0;
        }

        return distanceRepository
                .findByFromZoneIdAndToZoneId(fromId, toId)
                .map(ZoneDistance::getDistanceKm)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Distance mapping not found"
                        )
                );
    }
}