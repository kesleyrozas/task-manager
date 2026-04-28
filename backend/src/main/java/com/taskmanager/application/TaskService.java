package com.taskmanager.application;

import com.taskmanager.api.dto.task.CreateTaskRequest;
import com.taskmanager.api.dto.task.UpdateTaskRequest;
import com.taskmanager.api.exception.BusinessRuleException;
import com.taskmanager.api.exception.ForbiddenException;
import com.taskmanager.api.exception.NotFoundException;
import com.taskmanager.domain.project.Project;
import com.taskmanager.domain.project.ProjectRepository;
import com.taskmanager.domain.task.Priority;
import com.taskmanager.domain.task.Status;
import com.taskmanager.domain.task.Task;
import com.taskmanager.domain.task.TaskRepository;
import com.taskmanager.domain.task.TaskSpecifications;
import com.taskmanager.domain.user.Role;
import com.taskmanager.domain.user.User;
import com.taskmanager.domain.user.UserRepository;
import com.taskmanager.security.AppUserDetails;
import com.taskmanager.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TaskService {

    static final int WIP_LIMIT = 5;

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUser authenticatedUser;

    @Transactional
    public Task create(Long projectId, CreateTaskRequest request) {
        Project project = loadAccessibleProject(projectId);
        User assignee = resolveAssignee(project, request.assigneeId());

        Task task = Task.builder()
                .project(project)
                .title(request.title())
                .description(request.description())
                .priority(request.priority())
                .status(Status.TODO)
                .assignee(assignee)
                .deadline(request.deadline())
                .build();

        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public Task findAccessible(Long projectId, Long taskId) {
        loadAccessibleProject(projectId);
        Task task = loadOrThrow(taskId);
        ensureBelongsToProject(task, projectId);
        return task;
    }

    @Transactional(readOnly = true)
    public Page<Task> list(
            Long projectId,
            Status status,
            Priority priority,
            Long assigneeId,
            Instant deadlineFrom,
            Instant deadlineTo,
            String search,
            Pageable pageable) {
        ensureCallerCanAccess(projectId);
        return taskRepository.findAll(
                TaskSpecifications.withFilters(projectId, status, priority, assigneeId, deadlineFrom, deadlineTo, search),
                pageable
        );
    }

    private void ensureCallerCanAccess(Long projectId) {
        if (!projectRepository.isAccessibleBy(projectId, authenticatedUser.currentId())) {
            throw new ForbiddenException("Você não tem acesso a este projeto");
        }
    }

    @Transactional
    public Task update(Long projectId, Long taskId, UpdateTaskRequest request) {
        Project project = loadAccessibleProject(projectId);
        Task task = loadOrThrow(taskId);
        ensureBelongsToProject(task, projectId);

        Status nextStatus = request.status();
        Priority nextPriority = request.priority();

        if (task.getStatus() != nextStatus) {
            applyStatusTransition(task, nextStatus, project);
        }

        User newAssignee = resolveAssignee(project, request.assigneeId());

        if (movesToInProgress(task.getStatus(), nextStatus, task.getAssignee(), newAssignee)) {
            enforceWipLimit(newAssignee);
        }

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(nextPriority);
        task.setStatus(nextStatus);
        task.setAssignee(newAssignee);
        task.setDeadline(request.deadline());
        return task;
    }

    @Transactional
    public void delete(Long projectId, Long taskId) {
        loadAccessibleProject(projectId);
        Task task = loadOrThrow(taskId);
        ensureBelongsToProject(task, projectId);
        taskRepository.delete(task);
    }

    /**
     * Regras:
     * 1. DONE → TODO é proibido (apenas DONE → IN_PROGRESS).
     * 2. CRITICAL → DONE só por ADMIN do projeto (owner ou role ADMIN global).
     */
    private void applyStatusTransition(Task task, Status next, Project project) {
        Status current = task.getStatus();
        if (!current.canTransitionTo(next)) {
            throw new BusinessRuleException(
                    "invalid-status-transition",
                    "Transição de status inválida: de " + current + " para " + next
            );
        }
        if (next == Status.DONE && task.getPriority() == Priority.CRITICAL) {
            AppUserDetails actor = authenticatedUser.current();
            boolean isProjectAdmin = project.isOwner(actor.getUserId())
                    || actor.getUser().hasRole(Role.ADMIN);
            if (!isProjectAdmin) {
                throw new BusinessRuleException(
                        "critical-task-admin-only",
                        "Tarefas com prioridade CRITICAL só podem ser fechadas pelo ADMIN do projeto"
                );
            }
        }
    }

    /**
     * Regra 3: cada usuário pode ter no máximo 5 tarefas IN_PROGRESS simultaneamente.
     */
    private void enforceWipLimit(User assignee) {
        if (assignee == null) {
            return;
        }
        // Lock pessimista evita race entre count e save em PUTs concorrentes ao mesmo assignee.
        long inProgress = taskRepository.countInProgressForUpdate(assignee.getId(), Status.IN_PROGRESS);
        if (inProgress >= WIP_LIMIT) {
            throw new BusinessRuleException(
                    "wip-limit-exceeded",
                    "O responsável já possui " + inProgress + " tarefas IN_PROGRESS (limite: " + WIP_LIMIT + ")"
            );
        }
    }

    private boolean movesToInProgress(Status current, Status next, User currentAssignee, User newAssignee) {
        if (next != Status.IN_PROGRESS || newAssignee == null) {
            return false;
        }
        boolean assigneeChanged = currentAssignee == null || !currentAssignee.getId().equals(newAssignee.getId());
        boolean statusChanged = current != Status.IN_PROGRESS;
        return assigneeChanged || statusChanged;
    }

    private User resolveAssignee(Project project, Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        if (!project.hasMember(assigneeId)) {
            throw new BusinessRuleException(
                    "assignee-not-member",
                    "O responsável deve ser membro do projeto"
            );
        }
        return userRepository.findById(assigneeId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + assigneeId));
    }

    private Project loadAccessibleProject(Long projectId) {
        Project project = projectRepository.findByIdWithMembers(projectId)
                .orElseThrow(() -> new NotFoundException("Projeto não encontrado: " + projectId));
        if (!project.hasMember(authenticatedUser.currentId())) {
            throw new ForbiddenException("Você não é membro deste projeto");
        }
        return project;
    }

    private Task loadOrThrow(Long taskId) {
        return taskRepository.findByIdWithRelations(taskId)
                .orElseThrow(() -> new NotFoundException("Tarefa não encontrada: " + taskId));
    }

    private void ensureBelongsToProject(Task task, Long projectId) {
        if (!task.getProject().getId().equals(projectId)) {
            throw new NotFoundException("A tarefa " + task.getId() + " não pertence ao projeto " + projectId);
        }
    }
}
