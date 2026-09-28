
package com.ambulance.dispatch.controller;

import com.ambulance.dispatch.entity.EmergencyCall;
import com.ambulance.dispatch.service.EmergencyCallService;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/emergency")
public class EmergencyCallController {

    private final EmergencyCallService callService;

    public EmergencyCallController(
            EmergencyCallService callService) {
        this.callService = callService;
    }

    // Register emergency call
    @PostMapping("/{zoneId}")
    @ResponseStatus(HttpStatus.CREATED)
    public EmergencyCall registerCall(
            @PathVariable Long zoneId) {

        return callService.registerCall(zoneId);
    }

    // Assign nearest available ambulance
    @PostMapping("/{callId}/assign")
    public EmergencyCall assignAmbulance(
            @PathVariable Long callId) {

        return callService.assignAmbulance(callId);
    }

    // Mark ambulance as arrived
    @PutMapping("/{callId}/arrived")
    public EmergencyCall markArrived(
            @PathVariable Long callId) {

        return callService.markArrived(callId);
    }

    // Complete emergency call
    @PutMapping("/{callId}/complete")
    public EmergencyCall completeCall(
            @PathVariable Long callId) {

        return callService.completeCall(callId);
    }

    // Get all emergency calls
    @GetMapping
    public List<EmergencyCall> getAllCalls() {
        return callService.getAllCalls();
    }
}