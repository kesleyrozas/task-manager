package com.taskmanager.application;

import com.taskmanager.api.dto.auth.AuthResponse;
import com.taskmanager.api.dto.auth.LoginRequest;
import com.taskmanager.api.dto.auth.RegisterRequest;
import com.taskmanager.api.exception.EmailAlreadyUsedException;
import com.taskmanager.domain.user.Role;
import com.taskmanager.domain.user.User;
import com.taskmanager.domain.user.UserRepository;
import com.taskmanager.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        return createUser(request, Set.of(Role.MEMBER));
    }

    @Transactional
    public AuthResponse registerAdmin(RegisterRequest request) {
        return createUser(request, Set.of(Role.ADMIN));
    }

    private AuthResponse createUser(RegisterRequest request, Set<Role> roles) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyUsedException(request.email());
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .createdAt(Instant.now())
                .roles(roles)
                .build();

        User saved = userRepository.save(user);
        return buildAuthResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        User user = userRepository.findByEmail(request.email()).orElseThrow();
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        Map<String, Object> claims = Map.of(
                "userId", user.getId(),
                "roles", user.getRoles()
        );
        String token = jwtService.generateToken(user.getEmail(), claims);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), user.getRoles());
    }
}
