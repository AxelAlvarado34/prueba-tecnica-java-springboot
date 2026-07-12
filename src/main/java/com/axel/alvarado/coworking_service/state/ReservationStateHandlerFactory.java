package com.axel.alvarado.coworking_service.state;

import com.axel.alvarado.coworking_service.enums.ReservationState;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class ReservationStateHandlerFactory {

    private final Map<ReservationState, ReservationStateHandler> handlers;

    public ReservationStateHandlerFactory(
            PendingStateHandler pending,
            PendingPaymentStateHandler pendingPayment,
            ConfirmedStateHandler confirmed,
            CancelledStateHandler cancelled,
            CompletedStateHandler completed) {
        this.handlers = new EnumMap<>(ReservationState.class);
        handlers.put(ReservationState.PENDING, pending);
        handlers.put(ReservationState.PENDING_PAYMENT, pendingPayment);
        handlers.put(ReservationState.CONFIRMED, confirmed);
        handlers.put(ReservationState.CANCELLED, cancelled);
        handlers.put(ReservationState.COMPLETED, completed);
    }

    public ReservationStateHandler getHandler(ReservationState state) {
        return handlers.get(state);
    }
}