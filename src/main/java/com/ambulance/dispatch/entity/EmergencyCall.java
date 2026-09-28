
package com.ambulance.dispatch.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class EmergencyCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "caller_zone_id")
    private Zone callerZone;

    @ManyToOne
    @JoinColumn(name = "ambulance_id")
    private Ambulance ambulance;

    // Public user who registered this emergency.
    // Nullable so existing emergency records still work.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_user_id")
    private UserAccount registeredUser;

    @Column(nullable = false)
    private String priority = "NORMAL";

    private String status;

    private LocalDateTime receivedAt;

    private LocalDateTime arrivedAt;

    private LocalDateTime completedAt;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Zone getCallerZone() {
        return callerZone;
    }

    public void setCallerZone(Zone callerZone) {
        this.callerZone = callerZone;
    }

    public Ambulance getAmbulance() {
        return ambulance;
    }

    public void setAmbulance(Ambulance ambulance) {
        this.ambulance = ambulance;
    }

    public UserAccount getRegisteredUser() {
        return registeredUser;
    }

    public void setRegisteredUser(UserAccount registeredUser) {
        this.registeredUser = registeredUser;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public LocalDateTime getArrivedAt() {
        return arrivedAt;
    }

    public void setArrivedAt(LocalDateTime arrivedAt) {
        this.arrivedAt = arrivedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}