package com.taskmanager.application;

import com.taskmanager.api.dto.task.CreateTaskRequest;
import com.taskmanager.api.dto.task.UpdateTaskRequest;
import com.taskmanager.api.exception.BusinessRuleException;
import com.taskmanager.api.exception.ForbiddenException;
import com.taskmanager.domain.project.Project;
import com.taskmanager.domain.project.ProjectRepository;
import com.taskmanager.domain.task.Priority;
import com.taskmanager.domain.task.Status;
import com.taskmanager.domain.task.Task;
import com.taskmanager.domain.task.TaskRepository;
import com.taskmanager.domain.user.Role;
import com.taskmanager.domain.user.User;
import com.taskmanager.domain.user.UserRepository;
import com.taskmanager.security.AppUserDetails;
import com.taskmanager.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthenticatedUser authenticatedUser;

    @InjectMocks private TaskService taskService;

    private User owner;
    private User member;
    private User other;
    private Project project;

    @BeforeEach
    void setUp() {
        owner  = User.builder().id(1L).email("owner@x.com").name("Owner").roles(Set.of(Role.MEMBER)).build();
        member = User.builder().id(2L).email("m@x.com").name("Member").roles(Set.of(Role.MEMBER)).build();
        other  = User.builder().id(3L).email("o@x.com").name("Other").roles(Set.of(Role.MEMBER)).build();

        Set<User> members = new HashSet<>(Set.of(owner, member));
        project = Project.builder().id(10L).name("P").owner(owner).members(members).build();

        lenient().when(projectRepository.findByIdWithMembers(10L)).thenReturn(Optional.of(project));
        lenient().when(userRepository.findById(2L)).thenReturn(Optional.of(member));
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        lenient().when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("create: cria tarefa com status TODO mesmo se request não trouxer status")
    void create_setsStatusTodo() {
        loginAs(member);
        CreateTaskRequest req = new CreateTaskRequest("T1", "desc", Priority.LOW, 2L, null);

        Task task = taskService.create(10L, req);

        assertThat(task.getStatus()).isEqualTo(Status.TODO);
        assertThat(task.getProject()).isEqualTo(project);
        assertThat(task.getAssignee()).isEqualTo(member);
    }

    @Test
    @DisplayName("create: bloqueia quando assignee não é membro do projeto")
    void create_rejectsAssigneeNotMember() {
        loginAs(member);
        CreateTaskRequest req = new CreateTaskRequest("T1", null, Priority.LOW, 3L, null);

        assertThatThrownBy(() -> taskService.create(10L, req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("membro do projeto");
    }

    @Test
    @DisplayName("create: bloqueia quando usuário logado não é membro do projeto")
    void create_rejectsNonMemberCaller() {
        loginAs(other);
        CreateTaskRequest req = new CreateTaskRequest("T1", null, Priority.LOW, 2L, null);

        assertThatThrownBy(() -> taskService.create(10L, req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Regra 1: DONE → TODO é proibido")
    void rule1_doneCannotGoBackToTodo() {
        loginAs(member);
        Task task = existingTask(Status.DONE, Priority.LOW, member);
        when(taskRepository.findByIdWithRelations(99L)).thenReturn(Optional.of(task));

        UpdateTaskRequest req = new UpdateTaskRequest("T", null, Priority.LOW, Status.TODO, 2L, null);

        assertThatThrownBy(() -> taskService.update(10L, 99L, req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("DONE")
                .hasMessageContaining("TODO");
    }

    @Test
    @DisplayName("Regra 1: DONE → IN_PROGRESS é permitido")
    void rule1_doneCanGoToInProgress() {
        loginAs(member);
        Task task = existingTask(Status.DONE, Priority.LOW, member);
        when(taskRepository.findByIdWithRelations(99L)).thenReturn(Optional.of(task));
        when(taskRepository.countInProgressForUpdate(2L, Status.IN_PROGRESS)).thenReturn(0L);

        UpdateTaskRequest req = new UpdateTaskRequest("T", null, Priority.LOW, Status.IN_PROGRESS, 2L, null);

        Task updated = taskService.update(10L, 99L, req);
        assertThat(updated.getStatus()).isEqualTo(Status.IN_PROGRESS);
    }

    @Test
    @DisplayName("Regra 2: CRITICAL não pode ser fechada por MEMBER comum")
    void rule2_criticalCannotBeClosedByMember() {
        loginAs(member);
        Task task = existingTask(Status.IN_PROGRESS, Priority.CRITICAL, member);
        when(taskRepository.findByIdWithRelations(99L)).thenReturn(Optional.of(task));

        UpdateTaskRequest req = new UpdateTaskRequest("T", null, Priority.CRITICAL, Status.DONE, 2L, null);

        assertThatThrownBy(() -> taskService.update(10L, 99L, req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CRITICAL");
    }

    @Test
    @DisplayName("Regra 2: CRITICAL pode ser fechada pelo owner do projeto")
    void rule2_criticalCanBeClosedByOwner() {
        loginAs(owner);
        Task task = existingTask(Status.IN_PROGRESS, Priority.CRITICAL, owner);
        when(taskRepository.findByIdWithRelations(99L)).thenReturn(Optional.of(task));

        UpdateTaskRequest req = new UpdateTaskRequest("T", null, Priority.CRITICAL, Status.DONE, 1L, null);

        Task updated = taskService.update(10L, 99L, req);
        assertThat(updated.getStatus()).isEqualTo(Status.DONE);
    }

    @Test
    @DisplayName("Regra 2: CRITICAL pode ser fechada por usuário com role ADMIN global")
    void rule2_criticalCanBeClosedByGlobalAdmin() {
        User admin = User.builder().id(99L).email("a@x.com").name("Admin").roles(Set.of(Role.ADMIN)).build();
        project.getMembers().add(admin);
        when(userRepository.findById(99L)).thenReturn(Optional.of(admin));
        loginAs(admin);

        Task task = existingTask(Status.IN_PROGRESS, Priority.CRITICAL, admin);
        when(taskRepository.findByIdWithRelations(99L)).thenReturn(Optional.of(task));

        UpdateTaskRequest req = new UpdateTaskRequest("T", null, Priority.CRITICAL, Status.DONE, 99L, null);

        Task updated = taskService.update(10L, 99L, req);
        assertThat(updated.getStatus()).isEqualTo(Status.DONE);
    }

    @Test
    @DisplayName("Regra 3: WIP limit bloqueia quando assignee já tem 5 IN_PROGRESS")
    void rule3_wipLimitBlocksAtFive() {
        loginAs(member);
        Task task = existingTask(Status.TODO, Priority.LOW, member);
        when(taskRepository.findByIdWithRelations(99L)).thenReturn(Optional.of(task));
        when(taskRepository.countInProgressForUpdate(2L, Status.IN_PROGRESS)).thenReturn(5L);

        UpdateTaskRequest req = new UpdateTaskRequest("T", null, Priority.LOW, Status.IN_PROGRESS, 2L, null);

        assertThatThrownBy(() -> taskService.update(10L, 99L, req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("limit");
    }

    @Test
    @DisplayName("Regra 3: WIP limit não conta se já está IN_PROGRESS e mantém mesmo assignee")
    void rule3_wipLimitDoesNotApplyToSameAssigneeStaying() {
        loginAs(member);
        Task task = existingTask(Status.IN_PROGRESS, Priority.LOW, member);
        when(taskRepository.findByIdWithRelations(99L)).thenReturn(Optional.of(task));

        UpdateTaskRequest req = new UpdateTaskRequest("Updated", null, Priority.HIGH, Status.IN_PROGRESS, 2L, null);

        Task updated = taskService.update(10L, 99L, req);
        assertThat(updated.getTitle()).isEqualTo("Updated");
        assertThat(updated.getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    @DisplayName("Regra 3: WIP limit é checado quando reatribui IN_PROGRESS para outro membro")
    void rule3_wipLimitChecksOnReassign() {
        loginAs(owner);
        Task task = existingTask(Status.IN_PROGRESS, Priority.LOW, owner);
        when(taskRepository.findByIdWithRelations(99L)).thenReturn(Optional.of(task));
        when(taskRepository.countInProgressForUpdate(2L, Status.IN_PROGRESS)).thenReturn(5L);

        UpdateTaskRequest req = new UpdateTaskRequest("T", null, Priority.LOW, Status.IN_PROGRESS, 2L, null);

        assertThatThrownBy(() -> taskService.update(10L, 99L, req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("limit");
    }

    private void loginAs(User user) {
        AppUserDetails details = new AppUserDetails(user);
        lenient().when(authenticatedUser.current()).thenReturn(details);
        lenient().when(authenticatedUser.currentId()).thenReturn(user.getId());
    }

    private Task existingTask(Status status, Priority priority, User assignee) {
        return Task.builder()
                .id(99L)
                .project(project)
                .title("Existing")
                .status(status)
                .priority(priority)
                .assignee(assignee)
                .build();
    }
}
