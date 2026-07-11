package com.axel.alvarado.coworking_service.dto;

import java.math.BigDecimal;

import com.axel.alvarado.coworking_service.enums.SpaceType;

public record SpaceResponse(
        Long id,
        String name,
        SpaceType type,
        Integer capacity,
        String location,
        BigDecimal hourlyRate) {
}
