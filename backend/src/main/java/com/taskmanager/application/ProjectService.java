package com.taskmanager.application;

import com.taskmanager.api.dto.project.ProjectRequest;
import com.taskmanager.api.exception.ForbiddenException;
import com.taskmanager.api.exception.NotFoundException;
import com.taskmanager.domain.project.Project;
import com.taskmanager.domain.project.ProjectRepository;
import com.taskmanager.domain.user.Role;
import com.taskmanager.domain.user.User;
import com.taskmanager.domain.user.UserRepository;
import com.taskmanager.security.AppUserDetails;
import com.taskmanager.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUser authenticatedUser;

    @Transactional
    public Project create(ProjectRequest request) {
        AppUserDetails current = authenticatedUser.current();
        requireAdmin(current);

        User owner = userRepository.findById(current.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        Project project = Project.builder()
                .name(request.name())
                .description(request.description())
                .owner(owner)
                .createdAt(Instant.now())
                .build();
        project.getMembers().add(owner);
        return projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public List<Project> listAccessible() {
        return projectRepository.findAllAccessibleBy(authenticatedUser.currentId());
    }

    @Transactional(readOnly = true)
    public Project findAccessible(Long projectId) {
        Project project = loadOrThrow(projectId);
        if (!project.hasMember(authenticatedUser.currentId())) {
            throw new ForbiddenException("Você não é membro deste projeto");
        }
        return project;
    }

    @Transactional
    public Project update(Long projectId, ProjectRequest request) {
        Project project = loadOrThrow(projectId);
        requireOwnerOrAdmin(project);
        project.setName(request.name());
        project.setDescription(request.description());
        return project;
    }

    @Transactional
    public void delete(Long projectId) {
        Project project = loadOrThrow(projectId);
        requireOwnerOrAdmin(project);
        projectRepository.delete(project);
    }

    @Transactional
    public Project addMember(Long projectId, Long userId) {
        Project project = loadOrThrow(projectId);
        requireOwnerOrAdmin(project);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + userId));
        project.getMembers().add(user);
        return project;
    }

    @Transactional
    public Project removeMember(Long projectId, Long userId) {
        Project project = loadOrThrow(projectId);
        requireOwnerOrAdmin(project);
        if (project.isOwner(userId)) {
            throw new ForbiddenException("Não é possível remover o dono do projeto");
        }
        project.getMembers().removeIf(m -> m.getId().equals(userId));
        return project;
    }

    private Project loadOrThrow(Long projectId) {
        return projectRepository.findByIdWithMembers(projectId)
                .orElseThrow(() -> new NotFoundException("Projeto não encontrado: " + projectId));
    }

    private void requireAdmin(AppUserDetails details) {
        boolean isAdmin = details.getUser().hasRole(Role.ADMIN);
        if (!isAdmin) {
            throw new ForbiddenException("Apenas ADMIN pode executar esta ação");
        }
    }

    private void requireOwnerOrAdmin(Project project) {
        AppUserDetails current = authenticatedUser.current();
        boolean isOwner = project.isOwner(current.getUserId());
        boolean isAdmin = current.getUser().hasRole(Role.ADMIN);
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Apenas o dono do projeto ou ADMIN pode executar esta ação");
        }
    }
}
