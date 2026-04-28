package com.taskmanager.api.dto.project;

import com.taskmanager.domain.project.Project;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        MemberSummary owner,
        List<MemberSummary> members,
        Instant createdAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                MemberSummary.from(project.getOwner()),
                project.getMembers().stream().map(MemberSummary::from).collect(Collectors.toList()),
                project.getCreatedAt()
        );
    }
}
