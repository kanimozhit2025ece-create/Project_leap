
package com.ambulance.dispatch.controller;

import com.ambulance.dispatch.entity.Zone;
import com.ambulance.dispatch.service.ZoneService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/zone")
public class ZoneController {

    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @PostMapping
    public Zone addZone(@RequestBody Zone zone) {
        return zoneService.addZone(zone);
    }

    @GetMapping
    public List<Zone> getAllZones() {
        return zoneService.getAllZones();
    }

    @GetMapping("/{id}")
    public Zone getZoneById(@PathVariable Long id) {
        return zoneService.getZoneById(id);
    }
}