package com.taskmanager.api.controller;

import com.taskmanager.api.dto.PageResponse;
import com.taskmanager.api.dto.task.CreateTaskRequest;
import com.taskmanager.api.dto.task.TaskResponse;
import com.taskmanager.api.dto.task.UpdateTaskRequest;
import com.taskmanager.application.TaskService;
import com.taskmanager.domain.task.Priority;
import com.taskmanager.domain.task.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/v1/projects/{projectId}/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "Create a task in the project")
    @PostMapping
    public ResponseEntity<TaskResponse> create(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = TaskResponse.from(taskService.create(projectId, request));
        return ResponseEntity.created(URI.create("/v1/projects/" + projectId + "/tasks/" + response.id()))
                .body(response);
    }

    @Operation(summary = "List tasks with filters, search and pagination")
    @GetMapping
    public PageResponse<TaskResponse> list(
            @PathVariable Long projectId,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Instant deadlineFrom,
            @RequestParam(required = false) Instant deadlineTo,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(
                taskService.list(projectId, status, priority, assigneeId, deadlineFrom, deadlineTo, search, pageable),
                TaskResponse::from
        );
    }

    @Operation(summary = "Get a task")
    @GetMapping("/{taskId}")
    public TaskResponse get(@PathVariable Long projectId, @PathVariable Long taskId) {
        return TaskResponse.from(taskService.findAccessible(projectId, taskId));
    }

    @Operation(summary = "Update a task")
    @PutMapping("/{taskId}")
    public TaskResponse update(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        return TaskResponse.from(taskService.update(projectId, taskId, request));
    }

    @Operation(summary = "Delete a task")
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId, @PathVariable Long taskId) {
        taskService.delete(projectId, taskId);
        return ResponseEntity.noContent().build();
    }
}
