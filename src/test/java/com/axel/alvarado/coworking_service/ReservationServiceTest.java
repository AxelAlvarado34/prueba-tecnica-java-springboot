package com.axel.alvarado.coworking_service;

import com.axel.alvarado.coworking_service.dto.ReservationRequest;
import com.axel.alvarado.coworking_service.enums.ReservationState;
import com.axel.alvarado.coworking_service.event.ReservationConfirmedEvent;
import com.axel.alvarado.coworking_service.exception.OverlappingReservationException;
import com.axel.alvarado.coworking_service.model.Reservation;
import com.axel.alvarado.coworking_service.model.Space;
import com.axel.alvarado.coworking_service.model.User;
import com.axel.alvarado.coworking_service.repository.ReservationRepository;
import com.axel.alvarado.coworking_service.repository.SpaceRepository;
import com.axel.alvarado.coworking_service.repository.UserRepository;
import com.axel.alvarado.coworking_service.service.PaymentGatewayClient;
import com.axel.alvarado.coworking_service.service.ReservationService;
import com.axel.alvarado.coworking_service.state.ReservationStateHandler;
import com.axel.alvarado.coworking_service.state.ReservationStateHandlerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private SpaceRepository spaceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReservationStateHandlerFactory stateHandlerFactory;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private PaymentGatewayClient paymentGatewayClient;
    @Mock
    private ReservationStateHandler stateHandler;

    @InjectMocks
    private ReservationService reservationService;

    private Space space;
    private User user;

    @BeforeEach
    void setUp() {
        space = Space.builder().id(1L).name("Sala Test").build();
        user = User.builder().id(1L).email("user@test.com").build();
    }

    @Test
    void createReservation_whenOverlapExists_throwsExceptionAndNeverSaves() {
        when(spaceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(space));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(reservationRepository.findOverlapping(anyLong(), any(), any()))
                .thenReturn(List.of(new Reservation()));

        ReservationRequest request = new ReservationRequest(1L, LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2));

        assertThatThrownBy(() -> reservationService.createReservation(request, "user@test.com"))
                .isInstanceOf(OverlappingReservationException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_whenPaymentSucceeds_confirmsAndPublishesEvent() {
        when(spaceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(space));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(reservationRepository.findOverlapping(anyLong(), any(), any())).thenReturn(List.of());

        Reservation saved = Reservation.builder()
                .id(10L).space(space).user(user).status(ReservationState.PENDING)
                .startDateTime(LocalDateTime.now().plusDays(1))
                .endDateTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .build();
        when(reservationRepository.save(any())).thenReturn(saved);
        when(stateHandlerFactory.getHandler(ReservationState.PENDING)).thenReturn(stateHandler);
        when(paymentGatewayClient.validatePayment(10L)).thenReturn(true);

        ReservationRequest request = new ReservationRequest(1L, saved.getStartDateTime(), saved.getEndDateTime());
        reservationService.createReservation(request, "user@test.com");

        verify(stateHandler).confirm(saved);
        verify(eventPublisher).publishEvent(any(ReservationConfirmedEvent.class));
    }

    @Test
    void createReservation_whenPaymentFails_marksPendingPaymentWithoutNotifying() {
        when(spaceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(space));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(reservationRepository.findOverlapping(anyLong(), any(), any())).thenReturn(List.of());

        Reservation saved = Reservation.builder()
                .id(11L).space(space).user(user).status(ReservationState.PENDING)
                .startDateTime(LocalDateTime.now().plusDays(1))
                .endDateTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .build();
        when(reservationRepository.save(any())).thenReturn(saved);
        when(stateHandlerFactory.getHandler(ReservationState.PENDING)).thenReturn(stateHandler);
        when(paymentGatewayClient.validatePayment(11L)).thenReturn(false);

        ReservationRequest request = new ReservationRequest(1L, saved.getStartDateTime(), saved.getEndDateTime());
        reservationService.createReservation(request, "user@test.com");

        verify(stateHandler).markPendingPayment(saved);
        verify(stateHandler, never()).confirm(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}