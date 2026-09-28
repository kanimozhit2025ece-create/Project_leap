
package com.ambulance.dispatch.repository;

import com.ambulance.dispatch.entity.EmergencyCall;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmergencyCallRepository
        extends JpaRepository<EmergencyCall, Long> {

    List<EmergencyCall> findByStatus(String status);

    List<EmergencyCall> findByCallerZoneId(Long zoneId);

    List<EmergencyCall> findByReceivedAtBetween(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    // NEW: Lock emergency call during assignment
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EmergencyCall e WHERE e.id = :id")
    Optional<EmergencyCall> findByIdForUpdate(
            @Param("id") Long id
    );
}