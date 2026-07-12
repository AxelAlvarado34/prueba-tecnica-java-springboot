package com.axel.alvarado.coworking_service.dto;

import com.axel.alvarado.coworking_service.enums.Role;

public record UserResponse(
                Long id,
                String email,
                Role role) {

}
