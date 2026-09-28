
package com.ambulance.dispatch.service;

import com.ambulance.dispatch.entity.EmergencyCall;
import com.ambulance.dispatch.repository.EmergencyCallRepository;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class ReportService {

    private final EmergencyCallRepository callRepository;

    public ReportService(
            EmergencyCallRepository callRepository) {
        this.callRepository = callRepository;
    }

    public Map<String, Double> getAverageResponseTime(
            LocalDateTime startDate,
            LocalDateTime endDate) {

        List<EmergencyCall> calls =
                callRepository.findByReceivedAtBetween(
                        startDate, endDate
                );

        Map<String, Long> totalTime = new HashMap<>();
        Map<String, Integer> callCount = new HashMap<>();

        for (EmergencyCall call : calls) {

            if (call.getArrivedAt() == null) {
                continue;
            }

            String zone = call.getCallerZone()
                    .getZoneName();

            long seconds = Duration.between(
                    call.getReceivedAt(),
                    call.getArrivedAt()
            ).getSeconds();

            totalTime.merge(zone, seconds, Long::sum);
            callCount.merge(zone, 1, Integer::sum);
        }

        Map<String, Double> result = new HashMap<>();

        for (String zone : totalTime.keySet()) {

            double averageMinutes =
                    totalTime.get(zone) /
                            (callCount.get(zone) * 60.0);

            double roundedAverage =
                    Math.round(averageMinutes * 100.0) / 100.0;

            result.put(zone, roundedAverage);
        }

        return result;
    }
}