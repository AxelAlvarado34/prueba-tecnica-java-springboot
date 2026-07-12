package com.axel.alvarado.coworking_service.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.axel.alvarado.coworking_service.dto.AuthResponse;
import com.axel.alvarado.coworking_service.dto.LoginRequest;
import com.axel.alvarado.coworking_service.dto.UserRegisterRequest;
import com.axel.alvarado.coworking_service.dto.UserResponse;
import com.axel.alvarado.coworking_service.enums.Role;
import com.axel.alvarado.coworking_service.exception.EmailAlreadyExistsException;
import com.axel.alvarado.coworking_service.mapper.UserMapper;
import com.axel.alvarado.coworking_service.model.User;
import com.axel.alvarado.coworking_service.repository.UserRepository;
import com.axel.alvarado.coworking_service.security.JwtService;
import org.springframework.security.core.Authentication;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public UserResponse register(UserRegisterRequest request) {

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailAlreadyExistsException("Ya existe un usuario registrado con ese email: " + request.email());
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        User saved = userRepository.save(user);
        return UserMapper.toResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(token);
    }
}
