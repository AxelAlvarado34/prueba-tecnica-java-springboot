package com.axel.alvarado.coworking_service.state;

import com.axel.alvarado.coworking_service.enums.ReservationState;
import com.axel.alvarado.coworking_service.model.Reservation;
import org.springframework.stereotype.Component;

@Component
public class PendingStateHandler implements ReservationStateHandler {

    @Override
    public void confirm(Reservation reservation) {
        reservation.setStatus(ReservationState.CONFIRMED);
    }

    @Override
    public void cancel(Reservation reservation) {
        reservation.setStatus(ReservationState.CANCELLED);
    }

    @Override
    public void markPendingPayment(Reservation reservation) {
        reservation.setStatus(ReservationState.PENDING_PAYMENT);
    }
}