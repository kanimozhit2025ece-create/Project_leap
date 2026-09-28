
package com.ambulance.dispatch.service;

import com.ambulance.dispatch.entity.*;
import com.ambulance.dispatch.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Comparator;

@Service
public class EmergencyCallService {

    private final EmergencyCallRepository callRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final ZoneRepository zoneRepository;
    private final ZoneDistanceService distanceService;

    public EmergencyCallService(
            EmergencyCallRepository callRepository,
            AmbulanceRepository ambulanceRepository,
            ZoneRepository zoneRepository,
            ZoneDistanceService distanceService) {

        this.callRepository = callRepository;
        this.ambulanceRepository = ambulanceRepository;
        this.zoneRepository = zoneRepository;
        this.distanceService = distanceService;
    }

    // Register emergency call
    public EmergencyCall registerCall(Long zoneId) {

        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Caller zone not found"
                        )
                );

        EmergencyCall call = new EmergencyCall();

        call.setCallerZone(zone);
        call.setStatus("PENDING");
        call.setReceivedAt(
                LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
        );

        return callRepository.save(call);
    }


    @Transactional
    public EmergencyCall assignAmbulance(Long callId) {

        EmergencyCall call =
                callRepository.findByIdForUpdate(callId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Emergency call not found"
                                )
                        );

        if (!"PENDING".equals(call.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Call is already assigned or completed"
            );
        }

        List<Ambulance> available =
                ambulanceRepository.findAvailableAmbulancesForUpdate();

        Ambulance nearest = available.stream()
                .min(Comparator.comparingDouble(a ->
                        distanceService.getDistance(
                                a.getCurrentZone().getId(),
                                call.getCallerZone().getId()
                        )
                ))
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "No ambulance available"
                        )
                );

        nearest.setStatus("BUSY");
        ambulanceRepository.save(nearest);

        call.setAmbulance(nearest);
        call.setStatus("ASSIGNED");

        return callRepository.save(call);
    }

    // Mark ambulance as arrived
    public EmergencyCall markArrived(Long callId) {

        EmergencyCall call = callRepository.findById(callId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Emergency call not found"
                        )
                );

        if (!"ASSIGNED".equals(call.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Call must be assigned first"
            );
        }

        call.setStatus("ARRIVED");
        call.setArrivedAt(
                LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
        );
        return callRepository.save(call);
    }

    // Complete emergency call
    @Transactional
    public EmergencyCall completeCall(Long callId) {

        EmergencyCall call = callRepository.findById(callId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Emergency call not found"
                        )
                );

        if (!"ARRIVED".equals(call.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Call must be marked ARRIVED first"
            );
        }

        call.setStatus("COMPLETED");
        call.setCompletedAt(
                LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
        );
        Ambulance ambulance = call.getAmbulance();
        ambulance.setStatus("AVAILABLE");
        ambulance.setCurrentZone(call.getCallerZone());

        ambulanceRepository.save(ambulance);

        return callRepository.save(call);
    }

    // Get all emergency calls
    public List<EmergencyCall> getAllCalls() {
        return callRepository.findAll();
    }
}