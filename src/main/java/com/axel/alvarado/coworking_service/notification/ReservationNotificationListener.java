package com.axel.alvarado.coworking_service.notification;

import com.axel.alvarado.coworking_service.event.ReservationConfirmedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class ReservationNotificationListener {

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservationConfirmed(ReservationConfirmedEvent event) {
        log.info("Notificando a {}: tu reserva #{} en '{}' fue confirmada ({} - {})",
                event.userEmail(), event.reservationId(), event.spaceName(),
                event.startDateTime(), event.endDateTime());
    }
}