package com.axel.alvarado.coworking_service.mapper;

import com.axel.alvarado.coworking_service.dto.ReservationResponse;
import com.axel.alvarado.coworking_service.model.Reservation;

public class ReservationMapper {

    private ReservationMapper() {
    }

    public static ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getSpace().getId(),
                reservation.getSpace().getName(),
                reservation.getUser().getId(),
                reservation.getUser().getEmail(),
                reservation.getStartDateTime(),
                reservation.getEndDateTime(),
                reservation.getStatus());
    }
}
