package com.taskmanager.api.controller;

import com.taskmanager.api.dto.auth.AuthResponse;
import com.taskmanager.api.dto.auth.LoginRequest;
import com.taskmanager.api.dto.auth.RegisterRequest;
import com.taskmanager.application.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.taskmanager.api.exception.ForbiddenException;
import com.taskmanager.domain.user.Role;
import com.taskmanager.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final AuthenticatedUser authenticatedUser;

    @Operation(summary = "Registra um novo usuário (sempre como MEMBER)")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Login com email e senha; retorna JWT")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Cria um novo usuário com role ADMIN (apenas ADMIN logado)")
    @PostMapping("/admins")
    public ResponseEntity<AuthResponse> registerAdmin(@Valid @RequestBody RegisterRequest request) {
        if (!authenticatedUser.current().getUser().hasRole(Role.ADMIN)) {
            throw new ForbiddenException("Apenas ADMIN pode criar outros administradores");
        }
        AuthResponse response = authService.registerAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
