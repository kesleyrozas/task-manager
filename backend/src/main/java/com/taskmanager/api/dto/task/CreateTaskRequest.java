package com.taskmanager.api.dto.task;

import com.taskmanager.domain.task.Priority;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateTaskRequest(
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
        String title,

        @Size(max = 4000, message = "A descrição deve ter no máximo 4000 caracteres")
        String description,

        @NotNull(message = "A prioridade é obrigatória")
        Priority priority,

        Long assigneeId,

        @Future(message = "O prazo (deadline) deve ser uma data futura")
        Instant deadline
) {}
