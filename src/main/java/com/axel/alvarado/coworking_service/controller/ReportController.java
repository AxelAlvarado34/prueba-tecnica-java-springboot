package com.axel.alvarado.coworking_service.controller;

import com.axel.alvarado.coworking_service.dto.OccupancyReportResponse;
import com.axel.alvarado.coworking_service.service.OccupancyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final OccupancyReportService occupancyReportService;

    @GetMapping("/occupancy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OccupancyReportResponse>> getOccupancy(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(occupancyReportService.getOccupancyReport(start, end));
    }
}