package com.axel.alvarado.coworking_service.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.axel.alvarado.coworking_service.model.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

  @EntityGraph(attributePaths = { "space", "user" })
  List<Reservation> findByUserId(Long id);

  @Override
  @EntityGraph(attributePaths = { "space", "user" })
  List<Reservation> findAll();

  @Query("""
      SELECT r FROM Reservation r
      WHERE r.space.id = :spaceId
        AND r.status <> com.axel.alvarado.coworking_service.enums.ReservationState.CANCELLED
        AND r.startDateTime < :endDateTime
        AND r.endDateTime > :startDateTime
      """)
  List<Reservation> findOverlapping(
      @Param("spaceId") Long spaceId,
      @Param("startDateTime") LocalDateTime startDateTime,
      @Param("endDateTime") LocalDateTime endDateTime);
}
