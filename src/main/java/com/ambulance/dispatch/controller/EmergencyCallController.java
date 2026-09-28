
package com.ambulance.dispatch.controller;

import com.ambulance.dispatch.entity.EmergencyCall;
import com.ambulance.dispatch.service.EmergencyCallService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/emergency")
public class EmergencyCallController {

    private final EmergencyCallService callService;

    public EmergencyCallController(
            EmergencyCallService callService
    ) {
        this.callService = callService;
    }

    // 1. REGISTER EMERGENCY

    @PostMapping("/{zoneId}")
    @ResponseStatus(HttpStatus.CREATED)
    public EmergencyCall registerCall(
            @PathVariable Long zoneId,
            @RequestParam(defaultValue = "NORMAL")
            String priority,
            HttpServletRequest request
    ) {
        HttpSession session = requireSession(request);

        String role = (String) session.getAttribute("role");
        Long userId = (Long) session.getAttribute("userId");

        // Public emergencies belong to the logged-in user.
        // Admin emergencies remain unlinked, as before.
        Long registeredUserId =
                "PUBLIC".equals(role) ? userId : null;

        return callService.registerCall(
                zoneId,
                priority,
                registeredUserId
        );
    }

    // 2. GET MY EMERGENCY CALLS

    @GetMapping("/my")
    public List<EmergencyCall> getMyCalls(
            HttpServletRequest request
    ) {
        HttpSession session = requireSession(request);

        return callService.getMyCalls(
                (Long) session.getAttribute("userId")
        );
    }

    // 3. MANUALLY ASSIGN AMBULANCE
    // Existing ADMIN-only permission remains in interceptor.

    @PostMapping("/{callId}/assign")
    public EmergencyCall assignAmbulance(
            @PathVariable Long callId
    ) {
        return callService.assignAmbulance(callId);
    }

    // 4. MARK ARRIVED

    @PutMapping("/{callId}/arrived")
    public EmergencyCall markArrived(
            @PathVariable Long callId
    ) {
        return callService.markArrived(callId);
    }

    // 5. COMPLETE CALL

    @PutMapping("/{callId}/complete")
    public EmergencyCall completeCall(
            @PathVariable Long callId
    ) {
        return callService.completeCall(callId);
    }

    // 6. GET ALL CALLS
    // PUBLIC is blocked by LoginInterceptor.

    @GetMapping
    public List<EmergencyCall> getAllCalls() {
        return callService.getAllCalls();
    }

    // 7. GET CALL BY ID
    // PUBLIC is blocked by LoginInterceptor.

    @GetMapping("/{callId}")
    public EmergencyCall getCallById(
            @PathVariable Long callId
    ) {
        return callService.getCallById(callId);
    }

    // 8. SESSION HELPER

    private HttpSession requireSession(
            HttpServletRequest request
    ) {
        HttpSession session = request.getSession(false);

        if (session == null ||
                !(session.getAttribute("userId")
                        instanceof Long)) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please login"
            );
        }

        return session;
    }
}