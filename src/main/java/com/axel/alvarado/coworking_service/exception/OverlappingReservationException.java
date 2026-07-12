package com.axel.alvarado.coworking_service.exception;

public class OverlappingReservationException extends RuntimeException {
    public OverlappingReservationException(String message) {
        super(message);
    }
}