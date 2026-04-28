package com.taskmanager.api.controller;

import com.taskmanager.api.dto.project.AddMemberRequest;
import com.taskmanager.api.dto.project.ProjectRequest;
import com.taskmanager.api.dto.project.ProjectResponse;
import com.taskmanager.application.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Projects")
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "Create project (ADMIN)")
    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest request) {
        ProjectResponse response = ProjectResponse.from(projectService.create(request));
        return ResponseEntity.created(URI.create("/v1/projects/" + response.id())).body(response);
    }

    @Operation(summary = "List projects accessible to current user")
    @GetMapping
    public List<ProjectResponse> list() {
        return projectService.listAccessible().stream().map(ProjectResponse::from).toList();
    }

    @Operation(summary = "Get project by id (member only)")
    @GetMapping("/{id}")
    public ProjectResponse get(@PathVariable Long id) {
        return ProjectResponse.from(projectService.findAccessible(id));
    }

    @Operation(summary = "Update project (owner or ADMIN)")
    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return ProjectResponse.from(projectService.update(id, request));
    }

    @Operation(summary = "Delete project (owner or ADMIN)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add member to project (owner or ADMIN)")
    @PostMapping("/{id}/members")
    public ResponseEntity<ProjectResponse> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddMemberRequest request) {
        ProjectResponse response = ProjectResponse.from(projectService.addMember(id, request.userId()));
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Remove member from project (owner or ADMIN)")
    @DeleteMapping("/{id}/members/{userId}")
    public ProjectResponse removeMember(@PathVariable Long id, @PathVariable Long userId) {
        return ProjectResponse.from(projectService.removeMember(id, userId));
    }
}
