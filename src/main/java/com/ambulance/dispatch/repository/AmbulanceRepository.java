
package com.ambulance.dispatch.repository;

import com.ambulance.dispatch.entity.Ambulance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AmbulanceRepository
        extends JpaRepository<Ambulance, Long> {

    List<Ambulance> findByStatus(String status);

}