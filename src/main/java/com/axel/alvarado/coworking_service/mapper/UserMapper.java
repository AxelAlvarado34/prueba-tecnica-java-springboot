package com.axel.alvarado.coworking_service.mapper;

import com.axel.alvarado.coworking_service.dto.UserResponse;
import com.axel.alvarado.coworking_service.model.User;

public class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole());
    }
}
