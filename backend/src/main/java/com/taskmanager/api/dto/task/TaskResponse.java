package com.taskmanager.api.dto.task;

import com.taskmanager.api.dto.project.MemberSummary;
import com.taskmanager.domain.task.Priority;
import com.taskmanager.domain.task.Status;
import com.taskmanager.domain.task.Task;

import java.time.Instant;

public record TaskResponse(
        Long id,
        Long projectId,
        String title,
        String description,
        Status status,
        Priority priority,
        MemberSummary assignee,
        Instant deadline,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                MemberSummary.from(task.getAssignee()),
                task.getDeadline(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
