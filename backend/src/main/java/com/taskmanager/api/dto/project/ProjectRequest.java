package com.taskmanager.api.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank(message = "O nome do projeto é obrigatório")
        @Size(max = 255, message = "O nome do projeto deve ter no máximo 255 caracteres")
        String name,

        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
        String description
) {}
