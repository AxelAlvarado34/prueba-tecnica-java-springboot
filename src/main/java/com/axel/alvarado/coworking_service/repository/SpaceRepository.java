package com.axel.alvarado.coworking_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.axel.alvarado.coworking_service.model.Space;

public interface SpaceRepository extends JpaRepository<Space, Long> {
}
