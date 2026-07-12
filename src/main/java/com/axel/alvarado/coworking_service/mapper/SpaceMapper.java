package com.axel.alvarado.coworking_service.mapper;

import com.axel.alvarado.coworking_service.dto.SpaceRequest;
import com.axel.alvarado.coworking_service.dto.SpaceResponse;
import com.axel.alvarado.coworking_service.model.Space;

public class SpaceMapper {

    private SpaceMapper() {
    }

    public static Space toEntity(SpaceRequest request) {
        return Space.builder()
                .name(request.name())
                .type(request.type())
                .capacity(request.capacity())
                .location(request.location())
                .hourlyRate(request.hourlyRate())
                .build();
    }

    public static SpaceResponse toResponse(Space space) {
        return new SpaceResponse(
                space.getId(),
                space.getName(),
                space.getType(),
                space.getCapacity(),
                space.getLocation(),
                space.getHourlyRate());
    }
}
