package com.axel.alvarado.coworking_service.dto;

public record OccupancyReportResponse(
        Long spaceId,
        String spaceName,
        double occupancyPercentage,
        long occupiedMinutes,
        long totalMinutes) {
}