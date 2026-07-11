package com.axel.alvarado.coworking_service.dto;

import javax.management.relation.Role;

public record UserResponse(
        Long id,
        String email,
        Role role) {

}
