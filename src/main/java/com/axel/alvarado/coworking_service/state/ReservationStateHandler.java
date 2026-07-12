package com.axel.alvarado.coworking_service.state;

import com.axel.alvarado.coworking_service.exception.InvalidReservationStateException;
import com.axel.alvarado.coworking_service.model.Reservation;

public interface ReservationStateHandler {

    default void confirm(Reservation reservation) {
        throw invalidTransition(reservation, "CONFIRMED");
    }

    default void cancel(Reservation reservation) {
        throw invalidTransition(reservation, "CANCELLED");
    }

    default void markPendingPayment(Reservation reservation) {
        throw invalidTransition(reservation, "PENDING_PAYMENT");
    }

    default void complete(Reservation reservation) {
        throw invalidTransition(reservation, "COMPLETED");
    }

    private InvalidReservationStateException invalidTransition(Reservation reservation, String target) {
        return new InvalidReservationStateException(
                "No se puede pasar de " + reservation.getStatus() + " a " + target);
    }
}