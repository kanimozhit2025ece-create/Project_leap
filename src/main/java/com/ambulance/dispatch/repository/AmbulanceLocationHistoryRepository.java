
package com.ambulance.dispatch.repository;

import com.ambulance.dispatch.entity.AmbulanceLocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AmbulanceLocationHistoryRepository
        extends JpaRepository<AmbulanceLocationHistory, Long> {

    List<AmbulanceLocationHistory>
    findByAmbulanceIdOrderByUpdatedAtDescIdDesc(Long ambulanceId);
}