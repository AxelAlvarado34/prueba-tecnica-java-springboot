package com.axel.alvarado.coworking_service.service;

import com.axel.alvarado.coworking_service.dto.OccupancyReportResponse;
import com.axel.alvarado.coworking_service.model.Reservation;
import com.axel.alvarado.coworking_service.model.Space;
import com.axel.alvarado.coworking_service.repository.ReservationRepository;
import com.axel.alvarado.coworking_service.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OccupancyReportService {

    private final SpaceRepository spaceRepository;
    private final ReservationRepository reservationRepository;

    @Cacheable(value = "occupancyReport", key = "#startDateTime + '_' + #endDateTime")
    @Transactional(readOnly = true)
    public List<OccupancyReportResponse> getOccupancyReport(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        long totalMinutes = Duration.between(startDateTime, endDateTime).toMinutes();

        return spaceRepository.findAll().stream()
                .map(space -> buildReport(space, startDateTime, endDateTime, totalMinutes))
                .toList();
    }

    private OccupancyReportResponse buildReport(Space space, LocalDateTime rangeStart, LocalDateTime rangeEnd,
            long totalMinutes) {
        List<Reservation> reservations = reservationRepository.findOverlapping(space.getId(), rangeStart, rangeEnd);

        long occupiedMinutes = reservations.stream()
                .mapToLong(r -> clippedMinutes(r, rangeStart, rangeEnd))
                .sum();

        double percentage = totalMinutes == 0 ? 0.0 : (occupiedMinutes * 100.0) / totalMinutes;

        return new OccupancyReportResponse(
                space.getId(),
                space.getName(),
                Math.round(percentage * 100.0) / 100.0,
                occupiedMinutes,
                totalMinutes);
    }

    private long clippedMinutes(Reservation reservation, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        LocalDateTime effectiveStart = reservation.getStartDateTime().isBefore(rangeStart) ? rangeStart
                : reservation.getStartDateTime();
        LocalDateTime effectiveEnd = reservation.getEndDateTime().isAfter(rangeEnd) ? rangeEnd
                : reservation.getEndDateTime();
        return Duration.between(effectiveStart, effectiveEnd).toMinutes();
    }
}