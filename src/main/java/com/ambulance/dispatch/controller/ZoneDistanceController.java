
package com.ambulance.dispatch.controller;

import com.ambulance.dispatch.entity.ZoneDistance;
import com.ambulance.dispatch.service.ZoneDistanceService;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/distance")
public class ZoneDistanceController {

    private final ZoneDistanceService distanceService;

    public ZoneDistanceController(
            ZoneDistanceService distanceService) {
        this.distanceService = distanceService;
    }

    // Add distance
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ZoneDistance addDistance(
            @RequestBody ZoneDistance distance) {


        return distanceService.addDistance(distance);
    }

    // Get all distances
    @GetMapping
    public List<ZoneDistance> getAllDistances() {
        return distanceService.getAllDistances();
    }

    // Get distance between two zones
    @GetMapping("/{fromId}/{toId}")
    public Double getDistance(
            @PathVariable Long fromId,
            @PathVariable Long toId) {

        return distanceService.getDistance(fromId, toId);
    }
}