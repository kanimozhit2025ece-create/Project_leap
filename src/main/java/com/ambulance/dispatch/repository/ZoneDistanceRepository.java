
package com.ambulance.dispatch.repository;

import com.ambulance.dispatch.entity.ZoneDistance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ZoneDistanceRepository
        extends JpaRepository<ZoneDistance, Long> {

    Optional<ZoneDistance> findByFromZoneIdAndToZoneId(
            Long fromZoneId,
            Long toZoneId
    );
}