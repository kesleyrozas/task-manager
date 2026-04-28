package com.taskmanager.application;

import com.taskmanager.api.dto.report.ProjectReportResponse;
import com.taskmanager.api.exception.ForbiddenException;
import com.taskmanager.domain.project.ProjectRepository;
import com.taskmanager.domain.task.TaskRepository;
import com.taskmanager.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final AuthenticatedUser authenticatedUser;

    @Transactional(readOnly = true)
    public ProjectReportResponse summary(Long projectId) {
        if (!projectRepository.isAccessibleBy(projectId, authenticatedUser.currentId())) {
            throw new ForbiddenException("Você não tem acesso a este projeto");
        }

        Map<String, Long> byStatus = new LinkedHashMap<>();
        taskRepository.countByStatusForProject(projectId)
                .forEach(row -> byStatus.put(row.getStatus().name(), row.getTotal()));

        Map<String, Long> byPriority = new LinkedHashMap<>();
        taskRepository.countByPriorityForProject(projectId)
                .forEach(row -> byPriority.put(row.getPriority().name(), row.getTotal()));

        return new ProjectReportResponse(projectId, byStatus, byPriority);
    }
}
