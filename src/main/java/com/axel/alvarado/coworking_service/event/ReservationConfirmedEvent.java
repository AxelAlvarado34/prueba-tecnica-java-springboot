package com.axel.alvarado.coworking_service.event;

import java.time.LocalDateTime;

public record ReservationConfirmedEvent(
        Long reservationId,
        String userEmail,
        String spaceName,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime) {
}