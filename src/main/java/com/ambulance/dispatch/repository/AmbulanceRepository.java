
package com.ambulance.dispatch.repository;

import com.ambulance.dispatch.entity.Ambulance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AmbulanceRepository
        extends JpaRepository<Ambulance, Long> {

    List<Ambulance> findByStatus(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a FROM Ambulance a
        WHERE a.status = 'AVAILABLE'
        ORDER BY a.id
        """)
    List<Ambulance> findAvailableAmbulancesForUpdate();
}