package com.taskmanager.api.dto.project;

import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(
        @NotNull(message = "O id do usuário é obrigatório") Long userId
) {}
