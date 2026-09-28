
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

@Service
public class EmergencyCallService {

    private final EmergencyCallRepository callRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final ZoneRepository zoneRepository;
    private final ZoneDistanceService distanceService;
    private final AmbulanceLocationHistoryRepository historyRepository;
    private final UserAccountRepository userRepository;

    public EmergencyCallService(
            EmergencyCallRepository callRepository,
            AmbulanceRepository ambulanceRepository,
            ZoneRepository zoneRepository,
            ZoneDistanceService distanceService,
            AmbulanceLocationHistoryRepository historyRepository,
            UserAccountRepository userRepository
    ) {
        this.callRepository = callRepository;
        this.ambulanceRepository = ambulanceRepository;
        this.zoneRepository = zoneRepository;
        this.distanceService = distanceService;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    // 1. REGISTER EMERGENCY

    @Transactional
    public EmergencyCall registerCall(
            Long zoneId,
            String priority,
            Long registeredUserId
    ) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Caller zone not found"
                        )
                );

        if (priority == null ||
                !(priority.equals("NORMAL") ||
                        priority.equals("HIGH") ||
                        priority.equals("CRITICAL"))) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid emergency priority"
            );
        }

        EmergencyCall call = new EmergencyCall();

        call.setCallerZone(zone);
        call.setPriority(priority);
        call.setStatus("PENDING");

        call.setReceivedAt(
                LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
        );

        if (registeredUserId != null) {
            UserAccount user = userRepository
                    .findById(registeredUserId)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.UNAUTHORIZED,
                                    "Registered user not found"
                            )
                    );

            call.setRegisteredUser(user);
        }

        EmergencyCall savedCall =
                callRepository.saveAndFlush(call);

        // Automatically assign pending calls.
        assignPendingCalls();

        return callRepository.findById(savedCall.getId())
                .orElseThrow();
    }

    // 2. ASSIGN NEAREST AVAILABLE AMBULANCE

    @Transactional
    public EmergencyCall assignAmbulance(Long callId) {

        EmergencyCall call = callRepository
                .findByIdForUpdate(callId)
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
                ambulanceRepository
                        .findAvailableAmbulancesForUpdate();

        if (available.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No ambulance available"
            );
        }

        Ambulance nearest = null;
        double minimumDistance = Double.MAX_VALUE;

        Long callerZoneId = call.getCallerZone().getId();

        for (Ambulance ambulance : available) {

            if (ambulance.getCurrentZone() == null) {
                continue;
            }

            Long ambulanceZoneId =
                    ambulance.getCurrentZone().getId();

            double distance;

            try {
                distance = distanceService.getDistance(
                        ambulanceZoneId,
                        callerZoneId
                );

            } catch (ResponseStatusException ex) {

                if (ex.getStatusCode().value() == 404) {
                    continue;
                }

                throw ex;
            }

            if (distance < minimumDistance) {
                minimumDistance = distance;
                nearest = ambulance;
            }
        }

        if (nearest == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No available ambulance has a distance mapping"
            );
        }

        nearest.setStatus("BUSY");
        ambulanceRepository.save(nearest);

        call.setAmbulance(nearest);
        call.setStatus("ASSIGNED");

        return callRepository.save(call);
    }

    // 3. MARK AMBULANCE AS ARRIVED

    @Transactional
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

    // 4. COMPLETE EMERGENCY AND SAVE LOCATION HISTORY

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

        LocalDateTime completedTime =
                LocalDateTime.now(ZoneId.of("Asia/Kolkata"));

        call.setCompletedAt(completedTime);

        Ambulance ambulance = call.getAmbulance();

        Zone previousZone = ambulance.getCurrentZone();
        Zone newZone = call.getCallerZone();

        // Save history only when the zone changes.
        if (previousZone != null &&
                !previousZone.getId().equals(newZone.getId())) {

            AmbulanceLocationHistory history =
                    new AmbulanceLocationHistory();

            history.setAmbulance(ambulance);
            history.setFromZone(previousZone);
            history.setToZone(newZone);
            history.setUpdatedAt(completedTime);

            historyRepository.save(history);
        }

        // Release ambulance at the completed call's zone.
        ambulance.setCurrentZone(newZone);
        ambulance.setStatus("AVAILABLE");

        ambulanceRepository.save(ambulance);

        EmergencyCall completedCall =
                callRepository.saveAndFlush(call);

        // Assign waiting calls in priority order.
        assignPendingCalls();

        return completedCall;
    }

    // 5. GET ALL EMERGENCY CALLS
    // Controller restricts this to ADMIN and STAFF.

    public List<EmergencyCall> getAllCalls() {
        return callRepository.findAll();
    }

    // 6. GET EMERGENCY CALL BY ID
    // Controller restricts this to ADMIN and STAFF.

    public EmergencyCall getCallById(Long callId) {

        return callRepository.findById(callId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Emergency call not found"
                        )
                );
    }

    // 7. GET ONLY THE LOGGED-IN PUBLIC USER'S CALLS

    public List<EmergencyCall> getMyCalls(Long userId) {
        return callRepository
                .findByRegisteredUserIdOrderByReceivedAtDesc(
                        userId
                );
    }

    // 8. AUTOMATIC PRIORITY-BASED ASSIGNMENT

    private void assignPendingCalls() {

        List<EmergencyCall> pendingCalls =
                callRepository
                        .findByStatusOrderByReceivedAtAscIdAsc(
                                "PENDING"
                        );

        // CRITICAL -> HIGH -> NORMAL
        // Older calls first when priority is equal.
        pendingCalls.sort((call1, call2) -> {

            int rankCompare = Integer.compare(
                    getPriorityRank(call1.getPriority()),
                    getPriorityRank(call2.getPriority())
            );

            if (rankCompare != 0) {
                return rankCompare;
            }

            int timeCompare = call1.getReceivedAt()
                    .compareTo(call2.getReceivedAt());

            if (timeCompare != 0) {
                return timeCompare;
            }

            return call1.getId().compareTo(call2.getId());
        });

        for (EmergencyCall pendingCall : pendingCalls) {

            try {
                assignAmbulance(pendingCall.getId());

            } catch (ResponseStatusException ex) {

                if (ex.getStatusCode().value() == 409) {
                    continue;
                }

                throw ex;
            }
        }
    }

    // 9. PRIORITY RANKING

    private int getPriorityRank(String priority) {

        if ("CRITICAL".equals(priority)) {
            return 1;
        }

        if ("HIGH".equals(priority)) {
            return 2;
        }

        return 3;
    }
}