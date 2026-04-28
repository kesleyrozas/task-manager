package com.taskmanager.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.domain.user.Role;
import com.taskmanager.domain.user.User;
import com.taskmanager.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.web.FilterChainProxy;

import java.time.Instant;
import java.util.Set;

import jakarta.annotation.PostConstruct;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class TaskFlowIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private ObjectMapper mapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @PostConstruct
    void setupMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilter(springSecurityFilterChain)
                .build();
    }

    @Test
    void fullFlow_registerLoginCreateProjectAndTask() throws Exception {
        seedAdmin("admin1@x.com", "Admin User", "password123");
        String adminToken = login("admin1@x.com", "password123");
        String memberToken = registerAndLogin("member1@x.com", "Member", "password123");

        Long memberId = userIdFromLogin("member1@x.com", "password123");

        Long projectId = createProject(adminToken, "Sprint 1", "Initial sprint");

        addMember(adminToken, projectId, memberId);

        Long taskId = createTask(memberToken, projectId, "Implement login screen", "FE work", "HIGH", memberId);

        startTask(memberToken, projectId, taskId, memberId);

        mockMvc.perform(get("/v1/projects/" + projectId + "/tasks?status=IN_PROGRESS")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(taskId))
                .andExpect(jsonPath("$.total").value(1));

        mockMvc.perform(get("/v1/projects/" + projectId + "/tasks?search=login")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(taskId));

        mockMvc.perform(get("/v1/projects/" + projectId + "/report")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byStatus.IN_PROGRESS").value(1))
                .andExpect(jsonPath("$.byPriority.HIGH").value(1));

        mockMvc.perform(delete("/v1/projects/" + projectId + "/tasks/" + taskId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void unauthenticatedAccess_isRejected() throws Exception {
        mockMvc.perform(get("/v1/projects"))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCannotCreateProject() throws Exception {
        String memberToken = registerAndLogin("plain@x.com", "Plain", "password123");
        String body = """
                { "name": "X", "description": "Y" }
                """;
        mockMvc.perform(post("/v1/projects")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    private String registerAndLogin(String email, String name, String password) throws Exception {
        String body = String.format("""
                { "email": "%s", "password": "%s", "name": "%s" }
                """, email, password, name);

        MvcResult result = mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return readJson(result).get("token").asText();
    }

    private void seedAdmin(String email, String name, String password) {
        userRepository.save(User.builder()
                .email(email)
                .name(name)
                .password(passwordEncoder.encode(password))
                .roles(Set.of(Role.ADMIN))
                .createdAt(Instant.now())
                .build());
    }

    private String login(String email, String password) throws Exception {
        String body = String.format("""
                { "email": "%s", "password": "%s" }
                """, email, password);
        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).get("token").asText();
    }

    private Long userIdFromLogin(String email, String password) throws Exception {
        String body = String.format("""
                { "email": "%s", "password": "%s" }
                """, email, password);
        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).get("userId").asLong();
    }

    private Long createProject(String token, String name, String description) throws Exception {
        String body = String.format("""
                { "name": "%s", "description": "%s" }
                """, name, description);
        MvcResult result = mockMvc.perform(post("/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("id").asLong();
    }

    private void addMember(String adminToken, Long projectId, Long userId) throws Exception {
        String body = String.format("{\"userId\": %d}", userId);
        mockMvc.perform(post("/v1/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private Long createTask(String token, Long projectId, String title, String desc, String priority, Long assigneeId) throws Exception {
        String body = String.format("""
                { "title": "%s", "description": "%s", "priority": "%s", "assigneeId": %d }
                """, title, desc, priority, assigneeId);
        MvcResult result = mockMvc.perform(post("/v1/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = readJson(result);
        assertThat(json.get("status").asText()).isEqualTo("TODO");
        return json.get("id").asLong();
    }

    private void startTask(String token, Long projectId, Long taskId, Long assigneeId) throws Exception {
        String body = String.format("""
                {
                  "title": "Implement login screen",
                  "description": "FE work",
                  "priority": "HIGH",
                  "status": "IN_PROGRESS",
                  "assigneeId": %d
                }
                """, assigneeId);
        mockMvc.perform(put("/v1/projects/" + projectId + "/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString());
    }
}
