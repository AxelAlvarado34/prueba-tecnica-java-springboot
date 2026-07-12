package com.axel.alvarado.coworking_service.service;

import com.axel.alvarado.coworking_service.dto.ReservationRequest;
import com.axel.alvarado.coworking_service.dto.ReservationResponse;
import com.axel.alvarado.coworking_service.enums.ReservationState;
import com.axel.alvarado.coworking_service.event.ReservationConfirmedEvent;
import com.axel.alvarado.coworking_service.exception.OverlappingReservationException;
import com.axel.alvarado.coworking_service.exception.ResourceNotFoundException;
import com.axel.alvarado.coworking_service.mapper.ReservationMapper;
import com.axel.alvarado.coworking_service.model.Reservation;
import com.axel.alvarado.coworking_service.model.Space;
import com.axel.alvarado.coworking_service.model.User;
import com.axel.alvarado.coworking_service.repository.ReservationRepository;
import com.axel.alvarado.coworking_service.repository.SpaceRepository;
import com.axel.alvarado.coworking_service.repository.UserRepository;
import com.axel.alvarado.coworking_service.state.ReservationStateHandlerFactory;
import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

        private final ReservationRepository reservationRepository;
        private final SpaceRepository spaceRepository;
        private final UserRepository userRepository;
        private final ReservationStateHandlerFactory stateHandlerFactory;

        private final ApplicationEventPublisher eventPublisher;

        @Transactional
        public ReservationResponse createReservation(ReservationRequest request, String userEmail) {
                Space space = spaceRepository.findByIdForUpdate(request.spaceId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Espacio no encontrado: " + request.spaceId()));

                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Usuario no encontrado: " + userEmail));

                List<Reservation> overlapping = reservationRepository.findOverlapping(
                                space.getId(), request.startDateTime(), request.endDateTime());

                if (!overlapping.isEmpty()) {
                        throw new OverlappingReservationException("El espacio ya tiene una reserva en ese horario");
                }

                Reservation reservation = Reservation.builder()
                                .space(space)
                                .user(user)
                                .startDateTime(request.startDateTime())
                                .endDateTime(request.endDateTime())
                                .status(ReservationState.PENDING)
                                .build();

                Reservation saved = reservationRepository.save(reservation);
                return ReservationMapper.toResponse(saved);
        }

        @Transactional
        public ReservationResponse cancelReservation(Long reservationId, String userEmail, boolean isAdmin) {
                Reservation reservation = reservationRepository.findById(reservationId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Reserva no encontrada: " + reservationId));

                if (!isAdmin && !reservation.getUser().getEmail().equals(userEmail)) {
                        throw new ResourceNotFoundException("Reserva no encontrada: " + reservationId);
                }

                stateHandlerFactory.getHandler(reservation.getStatus()).cancel(reservation);

                return ReservationMapper.toResponse(reservation);
        }

        @Transactional(readOnly = true)
        public List<ReservationResponse> getMyReservations(String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Usuario no encontrado: " + userEmail));

                return reservationRepository.findByUserId(user.getId()).stream()
                                .map(ReservationMapper::toResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<ReservationResponse> getAllReservations() {
                return reservationRepository.findAll().stream()
                                .map(ReservationMapper::toResponse)
                                .toList();
        }

        @Transactional
        public ReservationResponse confirmReservation(Long reservationId) {
                Reservation reservation = reservationRepository.findById(reservationId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Reserva no encontrada: " + reservationId));

                stateHandlerFactory.getHandler(reservation.getStatus()).confirm(reservation);

                eventPublisher.publishEvent(new ReservationConfirmedEvent(
                                reservation.getId(),
                                reservation.getUser().getEmail(),
                                reservation.getSpace().getName(),
                                reservation.getStartDateTime(),
                                reservation.getEndDateTime()));

                return ReservationMapper.toResponse(reservation);
        }

        @Transactional
        public ReservationResponse completeReservation(Long reservationId) {
                Reservation reservation = reservationRepository.findById(reservationId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Reserva no encontrada: " + reservationId));

                stateHandlerFactory.getHandler(reservation.getStatus()).complete(reservation);

                return ReservationMapper.toResponse(reservation);
        }
}