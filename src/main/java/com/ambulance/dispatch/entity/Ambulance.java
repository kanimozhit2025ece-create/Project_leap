
package com.ambulance.dispatch.entity;

import jakarta.persistence.*;

@Entity
public class Ambulance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ambulanceNumber;

    private String status;

    @ManyToOne
    @JoinColumn(name = "home_zone_id")
    private Zone homeZone;

    @ManyToOne
    @JoinColumn(name = "current_zone_id")
    private Zone currentZone;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAmbulanceNumber() {
        return ambulanceNumber;
    }

    public void setAmbulanceNumber(String ambulanceNumber) {
        this.ambulanceNumber = ambulanceNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Zone getHomeZone() {
        return homeZone;
    }

    public void setHomeZone(Zone homeZone) {
        this.homeZone = homeZone;
    }

    public Zone getCurrentZone() {
        return currentZone;
    }

    public void setCurrentZone(Zone currentZone) {
        this.currentZone = currentZone;
    }
}