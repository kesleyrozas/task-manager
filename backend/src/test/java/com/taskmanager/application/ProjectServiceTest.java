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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthenticatedUser authenticatedUser;

    @InjectMocks private ProjectService projectService;

    private User admin;
    private User member;
    private User outsider;
    private Project project;

    @BeforeEach
    void setUp() {
        admin    = User.builder().id(1L).email("a@x.com").name("Admin").roles(Set.of(Role.ADMIN)).build();
        member   = User.builder().id(2L).email("m@x.com").name("Member").roles(Set.of(Role.MEMBER)).build();
        outsider = User.builder().id(3L).email("o@x.com").name("Outsider").roles(Set.of(Role.MEMBER)).build();

        Set<User> members = new HashSet<>();
        members.add(admin);
        members.add(member);
        project = Project.builder().id(10L).name("P").owner(admin).members(members).build();

        lenient().when(projectRepository.findByIdWithMembers(10L)).thenReturn(Optional.of(project));
        lenient().when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("create: ADMIN consegue criar projeto e vira owner+member")
    void create_admin_succeeds() {
        loginAs(admin);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        Project created = projectService.create(new ProjectRequest("New", "desc"));

        assertThat(created.getOwner()).isEqualTo(admin);
        assertThat(created.getMembers()).contains(admin);
    }

    @Test
    @DisplayName("create: MEMBER comum não pode criar projeto (403)")
    void create_member_forbidden() {
        loginAs(member);

        assertThatThrownBy(() -> projectService.create(new ProjectRequest("New", "desc")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    @DisplayName("findAccessible: 403 quando o usuário não é membro nem owner")
    void findAccessible_outsider_forbidden() {
        loginAs(outsider);

        assertThatThrownBy(() -> projectService.findAccessible(10L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("findAccessible: 404 quando projeto não existe")
    void findAccessible_notFound() {
        when(projectRepository.findByIdWithMembers(99L)).thenReturn(Optional.empty());
        loginAs(admin);

        assertThatThrownBy(() -> projectService.findAccessible(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("update: owner consegue atualizar")
    void update_owner_succeeds() {
        loginAs(admin);

        Project updated = projectService.update(10L, new ProjectRequest("Renamed", "new desc"));

        assertThat(updated.getName()).isEqualTo("Renamed");
        assertThat(updated.getDescription()).isEqualTo("new desc");
    }

    @Test
    @DisplayName("update: MEMBER comum não pode atualizar projeto que não é dele")
    void update_member_forbidden() {
        loginAs(member);

        assertThatThrownBy(() -> projectService.update(10L, new ProjectRequest("X", "Y")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("addMember: owner consegue adicionar novo membro")
    void addMember_owner_succeeds() {
        loginAs(admin);
        when(userRepository.findById(3L)).thenReturn(Optional.of(outsider));

        Project result = projectService.addMember(10L, 3L);

        assertThat(result.getMembers()).contains(outsider);
    }

    @Test
    @DisplayName("addMember: 404 quando usuário a adicionar não existe")
    void addMember_userNotFound() {
        loginAs(admin);
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.addMember(10L, 404L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Usuário não encontrado");
    }

    @Test
    @DisplayName("removeMember: bloqueia remoção do owner do projeto")
    void removeMember_cannotRemoveOwner() {
        loginAs(admin);

        assertThatThrownBy(() -> projectService.removeMember(10L, admin.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("dono");
    }

    @Test
    @DisplayName("removeMember: owner remove membro comum com sucesso")
    void removeMember_owner_removes_member() {
        loginAs(admin);

        Project result = projectService.removeMember(10L, member.getId());

        assertThat(result.getMembers()).doesNotContain(member);
    }

    @Test
    @DisplayName("delete: outsider não pode apagar")
    void delete_outsider_forbidden() {
        loginAs(outsider);

        assertThatThrownBy(() -> projectService.delete(10L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("delete: owner consegue apagar")
    void delete_owner_succeeds() {
        loginAs(admin);

        projectService.delete(10L);

        verify(projectRepository).delete(project);
    }

    private void loginAs(User user) {
        AppUserDetails details = new AppUserDetails(user);
        lenient().when(authenticatedUser.current()).thenReturn(details);
        lenient().when(authenticatedUser.currentId()).thenReturn(user.getId());
    }
}
