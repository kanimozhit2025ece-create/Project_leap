
package com.ambulance.dispatch.service;

import com.ambulance.dispatch.entity.Ambulance;
import com.ambulance.dispatch.entity.Zone;
import com.ambulance.dispatch.entity.AmbulanceLocationHistory;

import com.ambulance.dispatch.repository.AmbulanceRepository;
import com.ambulance.dispatch.repository.ZoneRepository;
import com.ambulance.dispatch.repository.AmbulanceLocationHistoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class AmbulanceService {

    private final AmbulanceRepository ambulanceRepository;
    private final ZoneRepository zoneRepository;
    private final AmbulanceLocationHistoryRepository historyRepository;

    public AmbulanceService(
            AmbulanceRepository ambulanceRepository,
            ZoneRepository zoneRepository,
            AmbulanceLocationHistoryRepository historyRepository) {

        this.ambulanceRepository = ambulanceRepository;
        this.zoneRepository = zoneRepository;
        this.historyRepository = historyRepository;
    }

    // 1. REGISTER AMBULANCE

    @Transactional
    public Ambulance addAmbulance(Ambulance ambulance) {

        if (ambulance == null ||
                ambulance.getAmbulanceNumber() == null ||
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

    // 2. GET ALL AMBULANCES

    public List<Ambulance> getAllAmbulances() {
        return ambulanceRepository.findAll();
    }

    // 3. GET AMBULANCE BY ID

    public Ambulance getAmbulanceById(Long id) {

        return ambulanceRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Ambulance not found"
                        )
                );
    }

    // 4. GET AVAILABLE AMBULANCES

    public List<Ambulance> getAvailableAmbulances() {
        return ambulanceRepository.findByStatus("AVAILABLE");
    }

    // 5. UPDATE LOCATION AND SAVE HISTORY

    @Transactional
    public Ambulance updateAmbulanceLocation(
            Long ambulanceId,
            Long zoneId) {

        Ambulance ambulance = ambulanceRepository
                .findById(ambulanceId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Ambulance not found"
                        )
                );

        Zone newZone = zoneRepository
                .findById(zoneId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Zone not found"
                        )
                );

        Zone previousZone = ambulance.getCurrentZone();

        if (previousZone == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ambulance current zone is not set"
            );
        }

        // No movement if both zones are identical.
        if (previousZone.getId().equals(newZone.getId())) {
            return ambulance;
        }

        // Update current location.
        ambulance.setCurrentZone(newZone);

        Ambulance updatedAmbulance =
                ambulanceRepository.save(ambulance);

        // Save movement history.
        AmbulanceLocationHistory history =
                new AmbulanceLocationHistory();

        history.setAmbulance(updatedAmbulance);
        history.setFromZone(previousZone);
        history.setToZone(newZone);

        history.setUpdatedAt(
                LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
        );

        historyRepository.save(history);

        return updatedAmbulance;
    }

    // 6. GET LOCATION HISTORY

    public List<AmbulanceLocationHistory> getLocationHistory(
            Long ambulanceId) {

        // Return 404 for an invalid ambulance ID.
        getAmbulanceById(ambulanceId);

        return historyRepository
                .findByAmbulanceIdOrderByUpdatedAtDescIdDesc(
                        ambulanceId
                );
    }
}